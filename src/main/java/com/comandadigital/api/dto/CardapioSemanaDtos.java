package com.comandadigital.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** Cardapio da semana (admin edita, cliente ve o de hoje). dia: 1 = segunda ... 7 = domingo. */
public final class CardapioSemanaDtos {

    private CardapioSemanaDtos() {}

    public record ItemDia(Long pratoId, String nome, String categoria, String fotoUrl, BigDecimal preco,
                          String status, boolean destaque) {}

    public record DiaCardapio(int dia, String nome, boolean hoje, List<ItemDia> itens) {}

    public record ItemRequest(@NotNull Long pratoId, boolean destaque) {}

    public record SalvarDiaRequest(@NotNull @Valid List<ItemRequest> itens) {}

    /** Versao publica (cliente): so pratos ativos, com o necessario para abrir o detalhe. */
    public record ItemHoje(Long pratoId, String nome, String descricao, String categoria, String fotoUrl,
                           BigDecimal preco, Integer tempoPreparoMin, boolean destaque) {}
}
