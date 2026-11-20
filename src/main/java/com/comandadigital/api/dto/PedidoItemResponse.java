package com.comandadigital.api.dto;

import com.comandadigital.api.model.PedidoItem;
import com.comandadigital.api.model.PedidoItemOpcao;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** "escolhas" traz o que foi escolhido no combo, ja em texto: "Bebida: Guaraná". */
public record PedidoItemResponse(Long pratoId, String pratoNome, Integer quantidade, BigDecimal precoUnitario,
                                 BigDecimal subtotal, String observacoes, List<String> escolhas) {
    public static PedidoItemResponse from(PedidoItem i) {
        return new PedidoItemResponse(i.getPrato().getId(), i.getPrato().getNome(), i.getQuantidade(),
                i.getPrecoUnitario(), i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())),
                i.getObservacoes(), escolhasEmTexto(i));
    }

    private static List<String> escolhasEmTexto(PedidoItem i) {
        Map<String, Map<String, Integer>> porGrupo = new LinkedHashMap<>();
        for (PedidoItemOpcao o : i.getOpcoes()) {
            porGrupo.computeIfAbsent(o.getGrupoNome(), k -> new LinkedHashMap<>()).merge(o.getOpcaoNome(), 1, Integer::sum);
        }
        return porGrupo.entrySet().stream().map(g -> g.getKey() + ": " + g.getValue().entrySet().stream()
                .map(e -> e.getValue() > 1 ? e.getKey() + " ×" + e.getValue() : e.getKey())
                .reduce((a, b) -> a + ", " + b).orElse("")).toList();
    }
}
