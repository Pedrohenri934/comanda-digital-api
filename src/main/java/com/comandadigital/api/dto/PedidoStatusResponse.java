package com.comandadigital.api.dto;

import com.comandadigital.api.model.Pedido;
import com.comandadigital.api.model.StatusPedido;

import java.time.LocalDateTime;

/** Usado pela timeline de acompanhamento do cliente (RF-007). */
public record PedidoStatusResponse(Long id, StatusPedido status, String motivoCancelamento,
                                   LocalDateTime atualizadoEm) {
    public static PedidoStatusResponse from(Pedido p) {
        return new PedidoStatusResponse(p.getId(), p.getStatus(), p.getMotivoCancelamento(), p.getUpdatedAt());
    }
}
