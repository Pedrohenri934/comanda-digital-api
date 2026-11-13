package com.comandadigital.api.service;

import com.comandadigital.api.dto.CriarPedidoRequest;
import com.comandadigital.api.dto.ItemPedidoRequest;
import com.comandadigital.api.dto.PedidoResponse;
import com.comandadigital.api.dto.PedidoStatusResponse;
import com.comandadigital.api.exception.RecursoNaoEncontradoException;
import com.comandadigital.api.exception.RegraDeNegocioException;
import com.comandadigital.api.model.*;
import com.comandadigital.api.repository.EstoqueConsultaRepository;
import com.comandadigital.api.repository.EstoqueConsultaRepository.FichaLinha;
import com.comandadigital.api.repository.PedidoRepository;
import com.comandadigital.api.repository.PratoRepository;
import com.comandadigital.api.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PedidoService {

    private static final BigDecimal MIL = BigDecimal.valueOf(1000);

    private final PedidoRepository pedidoRepository;
    private final PratoRepository pratoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstoqueConsultaRepository estoqueConsulta;

    public PedidoService(PedidoRepository pedidoRepository, PratoRepository pratoRepository,
                         UsuarioRepository usuarioRepository, EstoqueConsultaRepository estoqueConsulta) {
        this.pedidoRepository = pedidoRepository;
        this.pratoRepository = pratoRepository;
        this.usuarioRepository = usuarioRepository;
        this.estoqueConsulta = estoqueConsulta;
    }

    /**
     * Cria o pedido a partir do carrinho. O preco e o total vem do banco, nunca do cliente.
     * Confere o estoque (RN03 -> 422). A baixa do estoque acontece depois, quando o pedido e CONFIRMADO.
     */
    @Transactional
    public PedidoResponse criar(String emailCliente, CriarPedidoRequest req) {
        Usuario cliente = usuarioRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        // Se o mesmo prato aparecer duas vezes no carrinho, soma as quantidades para conferir o estoque.
        Map<Long, Integer> qtdPorPrato = new LinkedHashMap<>();
        for (ItemPedidoRequest item : req.itens()) {
            qtdPorPrato.merge(item.pratoId(), item.quantidade(), Integer::sum);
        }

        Map<Long, Prato> pratos = new HashMap<>();
        for (Prato p : pratoRepository.findByIdIn(qtdPorPrato.keySet())) pratos.put(p.getId(), p);

        for (Long pratoId : qtdPorPrato.keySet()) {
            Prato p = pratos.get(pratoId);
            if (p == null) throw new RegraDeNegocioException("Prato não encontrado: " + pratoId);
            if (p.getStatus() != StatusPrato.ATIVO) {
                throw new RegraDeNegocioException("O prato '" + p.getNome() + "' não está disponível no momento");
            }
        }

        String endereco = (req.enderecoEntrega() != null && !req.enderecoEntrega().isBlank())
                ? req.enderecoEntrega().trim() : cliente.getEndereco();
        if (endereco == null || endereco.isBlank()) {
            throw new RegraDeNegocioException("Informe o endereço de entrega");
        }

        validarEstoque(qtdPorPrato);

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setEnderecoEntrega(endereco);
        pedido.setObservacoes(req.observacoes());
        pedido.setCanal("SITE");
        pedido.setPago(true); // pagamento simulado (RF-006)

        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedidoRequest item : req.itens()) {
            Prato prato = pratos.get(item.pratoId());
            PedidoItem pi = new PedidoItem();
            pi.setPrato(prato);
            pi.setQuantidade(item.quantidade());
            pi.setPrecoUnitario(prato.getPrecoVenda());
            pi.setObservacoes(item.observacoes());
            pedido.adicionarItem(pi);
            total = total.add(prato.getPrecoVenda().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        pedido.setValorTotal(total);

        pedidoRepository.save(pedido);
        return PedidoResponse.from(pedido);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponse> meusPedidos(String emailCliente, Pageable pageable) {
        return pedidoRepository.findByClienteEmail(emailCliente, pageable).map(PedidoResponse::from);
    }

    /** O cliente so enxerga o proprio pedido; de outro cliente responde 404. */
    @Transactional(readOnly = true)
    public PedidoStatusResponse status(String emailCliente, Long id) {
        return pedidoRepository.findByIdAndClienteEmail(id, emailCliente)
                .map(PedidoStatusResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
    }

    // ---------------------------------------------------------------- estoque (RN03)

    private void validarEstoque(Map<Long, Integer> qtdPorPrato) {
        List<FichaLinha> fichas = estoqueConsulta.fichas(qtdPorPrato.keySet());

        // Quanto de cada ingrediente o pedido consome, na unidade padrao do ingrediente.
        Map<Long, BigDecimal> necessario = new LinkedHashMap<>();
        Map<Long, FichaLinha> infoIngrediente = new HashMap<>();
        for (FichaLinha f : fichas) {
            BigDecimal porcoes = BigDecimal.valueOf(qtdPorPrato.get(f.pratoId()));
            BigDecimal consumo = porcoes.multiply(f.quantidade()).multiply(f.fatorCorrecao())
                    .divide(BigDecimal.valueOf(Math.max(f.rendimento(), 1)), 4, RoundingMode.HALF_UP);
            consumo = paraUnidadePadrao(consumo, f.unidadeItem(), f.unidadePadrao());
            necessario.merge(f.ingredienteId(), consumo, BigDecimal::add);
            infoIngrediente.putIfAbsent(f.ingredienteId(), f);
        }
        if (necessario.isEmpty()) return;

        Map<Long, BigDecimal> saldos = estoqueConsulta.saldos(necessario.keySet());

        Map<String, String> faltando = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> e : necessario.entrySet()) {
            BigDecimal saldo = saldos.getOrDefault(e.getKey(), BigDecimal.ZERO);
            if (saldo.compareTo(e.getValue()) < 0) {
                FichaLinha info = infoIngrediente.get(e.getKey());
                BigDecimal falta = e.getValue().subtract(saldo);
                faltando.put(info.ingredienteNome(), "faltam " + formatar(falta) + " " + info.unidadePadrao()
                        + " (disponível: " + formatar(saldo) + " " + info.unidadePadrao() + ")");
            }
        }
        if (!faltando.isEmpty()) {
            throw new RegraDeNegocioException("Estoque insuficiente para preparar o pedido", faltando);
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
