package com.comandadigital.api.service;

import com.comandadigital.api.dto.CriarPedidoRequest;
import com.comandadigital.api.dto.ItemPedidoRequest;
import com.comandadigital.api.dto.PedidoResponse;
import com.comandadigital.api.dto.PedidoStatusResponse;
import com.comandadigital.api.exception.RecursoNaoEncontradoException;
import com.comandadigital.api.exception.RegraDeNegocioException;
import com.comandadigital.api.model.*;
import com.comandadigital.api.repository.PedidoRepository;
import com.comandadigital.api.repository.PratoRepository;
import com.comandadigital.api.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PedidoService {

        private final PedidoRepository pedidoRepository;
    private final PratoRepository pratoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstoqueService estoqueService;

    public PedidoService(PedidoRepository pedidoRepository, PratoRepository pratoRepository,
                         UsuarioRepository usuarioRepository, EstoqueService estoqueService) {
        this.pedidoRepository = pedidoRepository;
        this.pratoRepository = pratoRepository;
        this.usuarioRepository = usuarioRepository;
        this.estoqueService = estoqueService;
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

        estoqueService.validar(qtdPorPrato);

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
}
