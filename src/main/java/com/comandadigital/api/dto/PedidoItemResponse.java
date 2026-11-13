package com.comandadigital.api.dto;

import com.comandadigital.api.model.PedidoItem;

import java.math.BigDecimal;

public record PedidoItemResponse(Long pratoId, String pratoNome, Integer quantidade, BigDecimal precoUnitario,
                                 BigDecimal subtotal, String observacoes) {
    public static PedidoItemResponse from(PedidoItem i) {
        return new PedidoItemResponse(i.getPrato().getId(), i.getPrato().getNome(), i.getQuantidade(),
                i.getPrecoUnitario(), i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())),
                i.getObservacoes());
    }
}
