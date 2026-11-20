package com.comandadigital.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/** Combos com escolhas: grupos (ex.: Bebida) e opcoes de cada grupo (ex.: Coca-Cola). */
public final class ComboDtos {

    private ComboDtos() {}

    public record OpcaoResponse(Long id, String nome, Long pratoId, BigDecimal acrescimo, boolean disponivel) {}

    public record GrupoResponse(Long id, String nome, int minimo, int maximo, List<OpcaoResponse> opcoes) {}

    public record OpcaoRequest(
            @NotBlank(message = "Informe o nome da opção") @Size(max = 80, message = "O nome da opção pode ter até 80 caracteres") String nome,
            Long pratoId,
            @NotNull(message = "Informe o acréscimo (0 se não houver)") @DecimalMin(value = "0.00", message = "O acréscimo não pode ser negativo")
            @Digits(integer = 8, fraction = 2, message = "Acréscimo inválido") BigDecimal acrescimo) {}

    public record GrupoRequest(
            @NotBlank(message = "Informe o nome do grupo") @Size(max = 80, message = "O nome do grupo pode ter até 80 caracteres") String nome,
            @NotNull(message = "Informe o mínimo") @Min(value = 0, message = "O mínimo não pode ser negativo") @Max(value = 10, message = "O mínimo máximo é 10") Integer minimo,
            @NotNull(message = "Informe o máximo") @Min(value = 1, message = "O máximo mínimo é 1") @Max(value = 10, message = "O máximo é 10") Integer maximo,
            @NotEmpty(message = "Cada grupo precisa de pelo menos uma opção") List<@Valid OpcaoRequest> opcoes) {}

    public record OpcoesRequest(@NotNull(message = "Informe os grupos") List<@Valid GrupoRequest> grupos) {}
}
