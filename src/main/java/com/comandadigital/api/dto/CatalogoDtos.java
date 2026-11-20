package com.comandadigital.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/** DTOs do catalogo de produtos por fornecedor e da cotacao comparativa. */
public final class CatalogoDtos {

    private CatalogoDtos() {}

    public record ProdutoFornecedorRequest(
            @NotNull(message = "Escolha o ingrediente") Long ingredienteId,
            @NotNull(message = "Informe o preço") @DecimalMin(value = "0.0001", message = "O preço deve ser maior que zero") BigDecimal preco,
            @Size(max = 10, message = "A unidade de venda pode ter no máximo 10 caracteres") String unidadeVenda) {}

    public record ProdutoFornecedorResponse(Long ingredienteId, String ingredienteNome, String unidadePadrao,
                                            BigDecimal preco, String unidadeVenda) {}

    /** Uma linha da cotacao: um fornecedor ativo que vende o ingrediente. */
    public record CotacaoLinha(Long fornecedorId, String fornecedorNome, BigDecimal preco, String unidadeVenda,
                               BigDecimal diferencaPercentual, boolean menorPreco) {}

    public record CotacaoResponse(Long ingredienteId, String ingredienteNome, String unidadePadrao,
                                  BigDecimal custoAtual, List<CotacaoLinha> fornecedores) {}
}
