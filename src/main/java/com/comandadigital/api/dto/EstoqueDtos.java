package com.comandadigital.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Movimentacoes manuais de estoque: entrada (lote, validade, custo) e baixa por perda. */
public final class EstoqueDtos {

    private EstoqueDtos() {}

    public record EntradaRequest(
            @NotNull(message = "Escolha o ingrediente") Long ingredienteId,
            @NotNull(message = "Informe a quantidade") @DecimalMin(value = "0.001", message = "A quantidade deve ser maior que zero")
            @Digits(integer = 9, fraction = 3, message = "Quantidade inválida") BigDecimal quantidade,
            @NotNull(message = "Informe o custo unitário") @DecimalMin(value = "0.0000", message = "O custo não pode ser negativo")
            @Digits(integer = 6, fraction = 4, message = "Custo inválido") BigDecimal custoUnitario,
            @Size(max = 50, message = "O lote pode ter até 50 caracteres") String lote,
            LocalDate validade,
            @Size(max = 255, message = "A observação pode ter até 255 caracteres") String observacao) {}

    public record SaidaRequest(
            @NotNull(message = "Escolha o ingrediente") Long ingredienteId,
            @NotNull(message = "Informe a quantidade") @DecimalMin(value = "0.001", message = "A quantidade deve ser maior que zero")
            @Digits(integer = 9, fraction = 3, message = "Quantidade inválida") BigDecimal quantidade,
            @NotBlank(message = "Informe o motivo") @Pattern(regexp = "DESPERDICIO|VENCIMENTO|QUEBRA|USO_INTERNO",
                    message = "Motivo deve ser DESPERDICIO, VENCIMENTO, QUEBRA ou USO_INTERNO") String motivo,
            @Size(max = 255, message = "A observação pode ter até 255 caracteres") String observacao) {}

    public record MovimentacaoResponse(Long id, Long ingredienteId, String ingredienteNome, String unidade, String tipo,
                                       String motivo, BigDecimal quantidade, String lote, LocalDate validade,
                                       BigDecimal custoUnitario, String observacao, String usuarioNome, Long pedidoId,
                                       LocalDateTime criadoEm) {}
}
