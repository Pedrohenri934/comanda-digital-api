package com.comandadigital.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Pedido vindo do carrinho. O preco e o total NAO vem do cliente: o back calcula. */
public record CriarPedidoRequest(
        @NotEmpty(message = "O pedido precisa ter pelo menos um item") List<@Valid ItemPedidoRequest> itens,
        @Size(max = 500, message = "A observação pode ter até 500 caracteres") String observacoes,
        @Size(max = 255, message = "O endereço pode ter até 255 caracteres") String enderecoEntrega
) {}
