package com.comandadigital.api.service;

import com.comandadigital.api.exception.RegraDeNegocioException;
import com.comandadigital.api.repository.EstoqueConsultaRepository;
import com.comandadigital.api.repository.EstoqueConsultaRepository.FichaLinha;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Regras de estoque ligadas a venda: conferir (RN03), dar baixa e estornar. */
@Service
public class EstoqueService {

    private static final BigDecimal MIL = BigDecimal.valueOf(1000);

    private final EstoqueConsultaRepository repo;

    public EstoqueService(EstoqueConsultaRepository repo) {
        this.repo = repo;
    }

    /** Quanto de cada ingrediente (na unidade padrao) as porcoes consomem, ja com fator de correcao e rendimento. */
    private record Consumo(Map<Long, BigDecimal> necessario, Map<Long, FichaLinha> info) {}

    private Consumo calcular(Map<Long, Integer> qtdPorPrato) {
        List<FichaLinha> fichas = repo.fichas(qtdPorPrato.keySet());
        Map<Long, BigDecimal> necessario = new LinkedHashMap<>();
        Map<Long, FichaLinha> info = new HashMap<>();
        for (FichaLinha f : fichas) {
            BigDecimal porcoes = BigDecimal.valueOf(qtdPorPrato.get(f.pratoId()));
            BigDecimal consumo = porcoes.multiply(f.quantidade()).multiply(f.fatorCorrecao())
                    .divide(BigDecimal.valueOf(Math.max(f.rendimento(), 1)), 4, RoundingMode.HALF_UP);
            consumo = paraUnidadePadrao(consumo, f.unidadeItem(), f.unidadePadrao());
            necessario.merge(f.ingredienteId(), consumo, BigDecimal::add);
            info.putIfAbsent(f.ingredienteId(), f);
        }
        return new Consumo(necessario, info);
    }

    /**
     * Pratos que nao podem ser feitos agora: falta algum ingrediente da ficha tecnica para ao menos 1 porcao.
     * Pratos sem ficha tecnica nao entram (nao ha o que conferir). Usado para esconder o prato do cardapio.
     */
    public Set<Long> pratosSemEstoque(Collection<Long> pratoIds) {
        if (pratoIds.isEmpty()) return Set.of();
        List<FichaLinha> fichas = repo.fichas(pratoIds);
        if (fichas.isEmpty()) return Set.of();
        Map<Long, BigDecimal> saldos = repo.saldos(fichas.stream().map(FichaLinha::ingredienteId).distinct().toList());

        // consumo de 1 porcao, por prato e por ingrediente
        Map<Long, Map<Long, BigDecimal>> porPrato = new HashMap<>();
        for (FichaLinha f : fichas) {
            BigDecimal consumo = f.quantidade().multiply(f.fatorCorrecao())
                    .divide(BigDecimal.valueOf(Math.max(f.rendimento(), 1)), 4, RoundingMode.HALF_UP);
            consumo = paraUnidadePadrao(consumo, f.unidadeItem(), f.unidadePadrao());
            porPrato.computeIfAbsent(f.pratoId(), k -> new HashMap<>()).merge(f.ingredienteId(), consumo, BigDecimal::add);
        }
        Set<Long> semEstoque = new HashSet<>();
        for (Map.Entry<Long, Map<Long, BigDecimal>> p : porPrato.entrySet()) {
            for (Map.Entry<Long, BigDecimal> ing : p.getValue().entrySet()) {
                if (saldos.getOrDefault(ing.getKey(), BigDecimal.ZERO).compareTo(ing.getValue()) < 0) {
                    semEstoque.add(p.getKey());
                    break;
                }
            }
        }
        return semEstoque;
    }

    /** RN03: se faltar ingrediente, lanca 422 listando o que falta. */
    public void validar(Map<Long, Integer> qtdPorPrato) {
        Consumo c = calcular(qtdPorPrato);
        if (c.necessario().isEmpty()) return;
        Map<Long, BigDecimal> saldos = repo.saldos(c.necessario().keySet());

        Map<String, String> faltando = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> e : c.necessario().entrySet()) {
            BigDecimal saldo = saldos.getOrDefault(e.getKey(), BigDecimal.ZERO);
            if (saldo.compareTo(e.getValue()) < 0) {
                FichaLinha info = c.info().get(e.getKey());
                BigDecimal falta = e.getValue().subtract(saldo);
                faltando.put(info.ingredienteNome(), "faltam " + formatar(falta) + " " + info.unidadePadrao()
                        + " (disponível: " + formatar(saldo) + " " + info.unidadePadrao() + ")");
            }
        }
        if (!faltando.isEmpty()) {
            throw new RegraDeNegocioException("Estoque insuficiente para preparar o pedido", faltando);
        }
    }

    /** Confere e da baixa (SAIDA / VENDA) de cada ingrediente do pedido. Deve rodar dentro de uma transacao. */
    public void darBaixa(Long pedidoId, Long usuarioId, Map<Long, Integer> qtdPorPrato) {
        validar(qtdPorPrato);
        Consumo c = calcular(qtdPorPrato);
        for (Map.Entry<Long, BigDecimal> e : c.necessario().entrySet()) {
            repo.registrarMovimentacao(e.getKey(), "SAIDA", e.getValue().setScale(3, RoundingMode.HALF_UP),
                    "VENDA", pedidoId, usuarioId);
        }
    }

    /** Devolve ao estoque exatamente o que saiu pelo pedido (ESTORNO). Nao faz nada se nao houve baixa ou ja estornou. */
    public void estornar(Long pedidoId, Long usuarioId) {
        if (repo.jaEstornado(pedidoId)) return;
        for (Map.Entry<Long, BigDecimal> e : repo.saidasDoPedido(pedidoId).entrySet()) {
            repo.registrarMovimentacao(e.getKey(), "ESTORNO", e.getValue(), "ESTORNO", pedidoId, usuarioId);
        }
    }

    private static String formatar(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static BigDecimal paraUnidadePadrao(BigDecimal q, String de, String para) {
        if (de == null || para == null || de.equals(para)) return q;
        if (de.equals("G") && para.equals("KG")) return q.divide(MIL, 6, RoundingMode.HALF_UP);
        if (de.equals("KG") && para.equals("G")) return q.multiply(MIL);
        if (de.equals("ML") && para.equals("L")) return q.divide(MIL, 6, RoundingMode.HALF_UP);
        if (de.equals("L") && para.equals("ML")) return q.multiply(MIL);
        return q;
    }
}
