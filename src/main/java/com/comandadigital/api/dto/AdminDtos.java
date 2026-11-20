package com.comandadigital.api.dto;

import com.comandadigital.api.model.StatusPrato;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/** DTOs das telas de cadastro do admin (categorias, pratos, ingredientes e ficha tecnica). */
public final class AdminDtos {

    private AdminDtos() {}

    public record CategoriaRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 80, message = "O nome pode ter no máximo 80 caracteres") String nome,
            @Size(max = 255, message = "A descrição pode ter no máximo 255 caracteres") String descricao,
            @Min(value = 0, message = "A ordem não pode ser negativa") Integer ordem) {}

    public record CategoriaAdminResponse(Long id, String nome, String descricao, Integer ordem, String status) {}

    public record PratoRequest(
            @NotNull(message = "Escolha a categoria") Long categoriaId,
            @NotBlank(message = "Informe o nome") @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres") String nome,
            String descricao,
            @Size(max = 500, message = "A URL da foto pode ter no máximo 500 caracteres") String fotoUrl,
            @NotNull(message = "Informe o preço") @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero") BigDecimal precoVenda,
            @NotNull(message = "Informe o tempo de preparo") @Min(value = 1, message = "O tempo de preparo deve ser de pelo menos 1 minuto") Integer tempoPreparoMin) {}

    public record StatusPratoRequest(@NotNull(message = "Informe o status") StatusPrato status) {}

    /** custoPorPorcao e foodCost (%) ficam nulos enquanto o prato nao tem ficha tecnica. */
    public record PratoAdminResponse(Long id, String nome, String descricao, String fotoUrl, BigDecimal precoVenda,
                                     Integer tempoPreparoMin, Long categoriaId, String categoria, StatusPrato status,
                                     BigDecimal custoPorPorcao, BigDecimal foodCost, boolean temFicha) {}

    public record IngredienteRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres") String nome,
            @NotBlank(message = "Informe o SKU") @Size(max = 40, message = "O SKU pode ter no máximo 40 caracteres") String sku,
            @NotBlank(message = "Informe a unidade") @Pattern(regexp = "G|ML|UN|KG|L", message = "Unidade deve ser G, KG, ML, L ou UN") String unidadePadrao,
            @NotNull(message = "Informe o estoque mínimo") @DecimalMin(value = "0", message = "O estoque mínimo não pode ser negativo") BigDecimal estoqueMinimo,
            @NotNull(message = "Informe o custo unitário") @DecimalMin(value = "0", message = "O custo não pode ser negativo") BigDecimal custoUnitario) {}

    public record IngredienteResponse(Long id, String nome, String sku, String unidadePadrao, BigDecimal estoqueMinimo,
                                      BigDecimal custoUnitario, String status, BigDecimal saldo, boolean abaixoDoMinimo) {}

    public record FichaItemRequest(
            @NotNull(message = "Escolha o ingrediente") Long ingredienteId,
            @NotNull(message = "Informe a quantidade") @DecimalMin(value = "0.001", message = "A quantidade deve ser maior que zero") BigDecimal quantidade,
            @NotBlank(message = "Informe a unidade") @Pattern(regexp = "G|ML|UN|KG|L", message = "Unidade deve ser G, KG, ML, L ou UN") String unidade,
            @NotNull(message = "Informe o fator de correção") @DecimalMin(value = "1.00", message = "O fator de correção deve ser no mínimo 1,00") BigDecimal fatorCorrecao) {}

    public record FichaRequest(
            @NotNull(message = "Informe o rendimento") @Min(value = 1, message = "O rendimento deve ser de pelo menos 1 porção") Integer rendimento,
            String modoPreparo,
            @NotEmpty(message = "Inclua pelo menos um ingrediente") @Valid List<FichaItemRequest> itens) {}

    public record FichaItemResponse(Long ingredienteId, String ingredienteNome, BigDecimal quantidade, String unidade,
                                    BigDecimal fatorCorrecao, BigDecimal custoLinha) {}

    public record FichaResponse(Long pratoId, Integer rendimento, String modoPreparo, List<FichaItemResponse> itens,
                                BigDecimal custoPorPorcao, BigDecimal precoVenda, BigDecimal foodCost,
                                String aviso) {}
}
