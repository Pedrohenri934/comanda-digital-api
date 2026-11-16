package com.comandadigital.api.service;

import com.comandadigital.api.dto.RelatorioDtos.PratoBcg;
import com.comandadigital.api.dto.RelatorioDtos.RelatorioBcgResponse;
import com.comandadigital.api.exception.RegraDeNegocioException;
import com.comandadigital.api.repository.CadastroJdbcRepository;
import com.comandadigital.api.repository.RelatorioJdbcRepository;
import com.comandadigital.api.repository.RelatorioJdbcRepository.VendaPrato;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Matriz BCG do cardapio: popularidade (unidades vendidas) x margem (preco - custo da ficha tecnica).
 * Popular = vendeu pelo menos 70% da media do cardapio (e mais de zero). Margem alta = igual ou acima da media.
 */
@Service
public class RelatorioService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final RelatorioJdbcRepository repo;
    private final CadastroJdbcRepository cadastro;

    public RelatorioService(RelatorioJdbcRepository repo, CadastroJdbcRepository cadastro) {
        this.repo = repo;
        this.cadastro = cadastro;
    }

    @Transactional(readOnly = true)
    public RelatorioBcgResponse bcg(int dias) {
        if (dias < 1 || dias > 365) {
            throw new RegraDeNegocioException("O período deve ser de 1 a 365 dias");
        }
        Timestamp inicio = Timestamp.valueOf(LocalDate.now().minusDays(dias - 1L).atStartOfDay());
        List<VendaPrato> vendas = repo.vendasPorPrato(inicio);
        if (vendas.isEmpty()) {
            return new RelatorioBcgResponse(dias, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of(), List.of());
        }

        Map<Long, BigDecimal> custos = cadastro.custosPorPrato(vendas.stream().map(VendaPrato::pratoId).toList());

        // Margem de cada prato (em %).
        BigDecimal somaMargem = BigDecimal.ZERO;
        long totalVendido = 0;
        for (VendaPrato v : vendas) {
            somaMargem = somaMargem.add(margem(v, custos.get(v.pratoId())));
            totalVendido += v.quantidade();
        }
        BigDecimal n = BigDecimal.valueOf(vendas.size());
        BigDecimal mediaMargem = somaMargem.divide(n, 2, RoundingMode.HALF_UP);
        BigDecimal mediaVendas = BigDecimal.valueOf(totalVendido).divide(n, 4, RoundingMode.HALF_UP);
        BigDecimal limiar = mediaVendas.multiply(new BigDecimal("0.70")).setScale(2, RoundingMode.HALF_UP);

        List<PratoBcg> pratos = vendas.stream().map(v -> {
            BigDecimal custo = custos.get(v.pratoId());
            BigDecimal margem = margem(v, custo);
            boolean popular = v.quantidade() > 0 && BigDecimal.valueOf(v.quantidade()).compareTo(limiar) >= 0;
            boolean margemAlta = margem.compareTo(mediaMargem) >= 0;
            String quadrante = popular ? (margemAlta ? "ESTRELA" : "VACA_LEITEIRA")
                                       : (margemAlta ? "INTERROGACAO" : "ABACAXI");
            return new PratoBcg(v.pratoId(), v.nome(), v.categoria(), v.fotoUrl(), v.preco(), custo, margem,
                    v.quantidade(), v.receita(), quadrante);
        }).toList();

        List<PratoBcg> mais = pratos.stream().filter(p -> p.quantidade() > 0)
                .sorted(Comparator.comparingLong(PratoBcg::quantidade).reversed()).limit(5).toList();
        List<PratoBcg> menos = pratos.stream()
                .sorted(Comparator.comparingLong(PratoBcg::quantidade).thenComparing(PratoBcg::nome)).limit(5).toList();

        return new RelatorioBcgResponse(dias, mediaMargem, limiar, pratos, mais, menos);
    }

    /** Margem em % = (preco - custo) / preco. Sem ficha tecnica o custo e desconhecido: margem 0. */
    private static BigDecimal margem(VendaPrato v, BigDecimal custo) {
        if (custo == null || v.preco().signum() == 0) return BigDecimal.ZERO;
        return v.preco().subtract(custo).multiply(CEM).divide(v.preco(), 2, RoundingMode.HALF_UP);
    }
}
