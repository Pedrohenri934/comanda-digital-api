package com.comandadigital.api.repository;

import com.comandadigital.api.dto.DashboardDtos.MaisVendido;
import com.comandadigital.api.dto.DashboardDtos.StatusQtd;
import com.comandadigital.api.dto.DashboardDtos.VendaDia;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/** Consultas agregadas do dashboard. "Venda" = pedido confirmado em diante (nao conta RECEBIDO nem CANCELADO). */
@Repository
public class DashboardJdbcRepository {

    private static final String VENDA = "('CONFIRMADO','EM_PREPARO','PRONTO','SAIU_ENTREGA','FINALIZADO')";
    private static final String ABERTO = "('RECEBIDO','CONFIRMADO','EM_PREPARO','PRONTO','SAIU_ENTREGA')";

    public record Resumo(long pedidos, long vendas, long cancelados, BigDecimal faturamento) {}

    private final NamedParameterJdbcTemplate jdbc;

    public DashboardJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static MapSqlParameterSource desde(Timestamp inicio) {
        return new MapSqlParameterSource().addValue("ini", inicio);
    }

    public Resumo resumo(Timestamp inicio) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS pedidos,
                       COALESCE(SUM(CASE WHEN status IN %s THEN 1 ELSE 0 END), 0) AS vendas,
                       COALESCE(SUM(CASE WHEN status = 'CANCELADO' THEN 1 ELSE 0 END), 0) AS cancelados,
                       COALESCE(SUM(CASE WHEN status IN %s THEN valor_total ELSE 0 END), 0) AS faturamento
                FROM pedido WHERE created_at >= :ini
                """.formatted(VENDA, VENDA), desde(inicio),
                (rs, n) -> new Resumo(rs.getLong("pedidos"), rs.getLong("vendas"), rs.getLong("cancelados"),
                        rs.getBigDecimal("faturamento")));
    }

    /** Pedidos em andamento agora (independe do periodo). */
    public long pedidosAbertos() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM pedido WHERE status IN " + ABERTO,
                new MapSqlParameterSource(), Long.class);
        return n == null ? 0 : n;
    }

    public List<StatusQtd> pedidosPorStatus(Timestamp inicio) {
        return jdbc.query("SELECT status, COUNT(*) AS qtd FROM pedido WHERE created_at >= :ini GROUP BY status ORDER BY qtd DESC",
                desde(inicio), (rs, n) -> new StatusQtd(rs.getString("status"), rs.getLong("qtd")));
    }

    public List<VendaDia> vendasPorDia(Timestamp inicio) {
        return jdbc.query("""
                SELECT DATE(created_at) AS dia, COUNT(*) AS pedidos, COALESCE(SUM(valor_total), 0) AS faturamento
                FROM pedido WHERE created_at >= :ini AND status IN %s
                GROUP BY DATE(created_at) ORDER BY dia
                """.formatted(VENDA), desde(inicio),
                (rs, n) -> new VendaDia(rs.getDate("dia").toLocalDate(), rs.getLong("pedidos"), rs.getBigDecimal("faturamento")));
    }

    public record VendaPrato(long pratoId, long quantidade, BigDecimal receita) {}

    /** Quantidade e receita por prato nas vendas do periodo (base do food cost medio ponderado). */
    public List<VendaPrato> vendasPorPrato(Timestamp inicio) {
        return jdbc.query("""
                SELECT i.prato_id AS prato_id, SUM(i.quantidade) AS qtd, SUM(i.quantidade * i.preco_unitario) AS receita
                FROM pedido_item i JOIN pedido o ON o.id = i.pedido_id
                WHERE o.created_at >= :ini AND o.status IN %s
                GROUP BY i.prato_id
                """.formatted(VENDA), desde(inicio),
                (rs, n) -> new VendaPrato(rs.getLong("prato_id"), rs.getLong("qtd"), rs.getBigDecimal("receita")));
    }

    public List<MaisVendido> maisVendidos(Timestamp inicio, int limite) {
        return jdbc.query("""
                SELECT p.nome AS prato, SUM(i.quantidade) AS qtd, SUM(i.quantidade * i.preco_unitario) AS receita
                FROM pedido_item i
                JOIN pedido o ON o.id = i.pedido_id
                JOIN prato p ON p.id = i.prato_id
                WHERE o.created_at >= :ini AND o.status IN %s
                GROUP BY p.id, p.nome ORDER BY qtd DESC LIMIT :lim
                """.formatted(VENDA), desde(inicio).addValue("lim", limite),
                (rs, n) -> new MaisVendido(rs.getString("prato"), rs.getLong("qtd"), rs.getBigDecimal("receita")));
    }
}
