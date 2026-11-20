package com.comandadigital.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ItemPedidoRequest(
        @NotNull(message = "Informe o prato") Long pratoId,
        @NotNull(message = "Informe a quantidade") @Min(value = 1, message = "A quantidade mínima é 1")
        @Max(value = 50, message = "A quantidade máxima por item é 50") Integer quantidade,
        @Size(max = 255, message = "A observação do item pode ter até 255 caracteres") String observacoes,
        @Size(max = 20, message = "Escolhas demais neste item") List<Long> opcoes
) {}
