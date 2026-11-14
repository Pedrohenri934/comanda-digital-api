package com.comandadigital.api.dto;

import com.comandadigital.api.model.Pedido;
import com.comandadigital.api.model.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Visao do pedido para a cozinha e a gerencia (inclui dados do cliente). */
public record PedidoGestaoResponse(Long id, StatusPedido status, BigDecimal valorTotal, String enderecoEntrega,
                                   String observacoes, boolean pago, String canal, String motivoCancelamento,
                                   LocalDateTime criadoEm, LocalDateTime atualizadoEm,
                                   Long clienteId, String clienteNome, String clienteTelefone,
                                   List<PedidoItemResponse> itens) {
    public static PedidoGestaoResponse from(Pedido p) {
        return new PedidoGestaoResponse(p.getId(), p.getStatus(), p.getValorTotal(), p.getEnderecoEntrega(),
                p.getObservacoes(), p.isPago(), p.getCanal(), p.getMotivoCancelamento(), p.getCreatedAt(),
                p.getUpdatedAt(), p.getCliente().getId(), p.getCliente().getNome(), p.getCliente().getTelefone(),
                p.getItens().stream().map(PedidoItemResponse::from).toList());
    }
}
