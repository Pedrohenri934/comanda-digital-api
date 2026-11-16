package com.comandadigital.api.dto;

import java.math.BigDecimal;
import java.util.List;

/** Vendas por prato e Matriz BCG (engenharia de cardapio). */
public final class RelatorioDtos {

    private RelatorioDtos() {}

    /** quadrante: ESTRELA, VACA_LEITEIRA, INTERROGACAO ou ABACAXI. */
    public record PratoBcg(Long pratoId, String nome, String categoria, String fotoUrl, BigDecimal preco,
                           BigDecimal custo, BigDecimal margemPct, long quantidade, BigDecimal receita,
                           String quadrante) {}

    public record RelatorioBcgResponse(int dias, BigDecimal mediaMargemPct, BigDecimal limiarVendas,
                                       List<PratoBcg> pratos, List<PratoBcg> maisVendidos,
                                       List<PratoBcg> menosVendidos) {}
}
