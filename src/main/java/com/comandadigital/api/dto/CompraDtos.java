package com.comandadigital.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs de fornecedores e pedidos de compra (entrada de estoque). */
public final class CompraDtos {

    private CompraDtos() {}

    public record FornecedorRequest(
            @NotBlank(message = "Informe a razão social") @Size(max = 160, message = "A razão social pode ter no máximo 160 caracteres") String razaoSocial,
            @NotBlank(message = "Informe o CNPJ") String cnpj,
            @Size(max = 20, message = "O telefone pode ter no máximo 20 caracteres") String telefone,
            @Email(message = "E-mail inválido") @Size(max = 160, message = "O e-mail pode ter no máximo 160 caracteres") String email) {}

    public record FornecedorResponse(Long id, String razaoSocial, String cnpj, String telefone, String email, String status) {}

    public record CompraItemRequest(
            @NotNull(message = "Escolha o ingrediente") Long ingredienteId,
            @NotNull(message = "Informe a quantidade") @DecimalMin(value = "0.001", message = "A quantidade deve ser maior que zero") BigDecimal quantidade,
            @NotNull(message = "Informe o preço unitário") @DecimalMin(value = "0", message = "O preço não pode ser negativo") BigDecimal precoUnitario) {}

    public record CompraRequest(
            @NotNull(message = "Escolha o fornecedor") Long fornecedorId,
            @NotEmpty(message = "Inclua pelo menos um item") @Valid List<CompraItemRequest> itens) {}

    public record CompraStatusRequest(@NotBlank(message = "Informe o status") String status) {}

    public record CompraItemResponse(Long ingredienteId, String ingredienteNome, String unidade, BigDecimal quantidade,
                                     BigDecimal precoUnitario, BigDecimal subtotal) {}

    public record CompraResponse(Long id, Long fornecedorId, String fornecedorNome, String status, BigDecimal valorTotal,
                                 LocalDateTime criadoEm, List<CompraItemResponse> itens) {}
}
