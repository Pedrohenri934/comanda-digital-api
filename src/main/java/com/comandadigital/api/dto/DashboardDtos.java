package com.comandadigital.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Dados do dashboard do admin/gerente. Faturamento considera pedidos confirmados em diante (sem cancelados). */
public final class DashboardDtos {

    private DashboardDtos() {}

    public record Totais(long pedidos, long vendas, long cancelados, long pedidosAbertos,
                         BigDecimal faturamento, BigDecimal ticketMedio, BigDecimal foodCostMedio) {}

    public record StatusQtd(String status, long quantidade) {}

    public record VendaDia(LocalDate data, long pedidos, BigDecimal faturamento) {}

    public record MaisVendido(String prato, long quantidade, BigDecimal receita) {}

    public record DashboardResponse(int dias, Totais totais, List<StatusQtd> pedidosPorStatus, List<VendaDia> vendasPorDia,
                                    List<MaisVendido> maisVendidos, List<AdminDtos.IngredienteResponse> estoqueBaixo) {}
}
