package com.comandadigital.api.dto;

import com.comandadigital.api.model.Pedido;
import com.comandadigital.api.model.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(Long id, StatusPedido status, BigDecimal valorTotal, String enderecoEntrega,
                             String observacoes, boolean pago, String motivoCancelamento, LocalDateTime criadoEm,
                             List<PedidoItemResponse> itens) {
    public static PedidoResponse from(Pedido p) {
        return new PedidoResponse(p.getId(), p.getStatus(), p.getValorTotal(), p.getEnderecoEntrega(),
                p.getObservacoes(), p.isPago(), p.getMotivoCancelamento(), p.getCreatedAt(),
                p.getItens().stream().map(PedidoItemResponse::from).toList());
    }
}
