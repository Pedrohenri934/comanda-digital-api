package com.comandadigital.api.dto;

import com.comandadigital.api.model.Prato;

import java.math.BigDecimal;

/** Dados publicos do prato. Nao inclui custo nem food cost. */
public record PratoResponse(Long id, String nome, String descricao, String fotoUrl, BigDecimal precoVenda,
                            Integer tempoPreparoMin, Long categoriaId, String categoria, boolean temOpcoes) {
    public static PratoResponse from(Prato p) { return from(p, false); }

    public static PratoResponse from(Prato p, boolean temOpcoes) {
        return new PratoResponse(p.getId(), p.getNome(), p.getDescricao(), p.getFotoUrl(), p.getPrecoVenda(),
                p.getTempoPreparoMin(), p.getCategoria().getId(), p.getCategoria().getNome(), temOpcoes);
    }
}
