package com.comandadigital.api.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class RelatorioJdbcRepository {

    public record VendaPrato(long pratoId, String nome, String categoria, String fotoUrl, BigDecimal preco,
                             long quantidade, BigDecimal receita) {}

    private final NamedParameterJdbcTemplate jdbc;

    public RelatorioJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Todos os pratos ativos (mesmo sem venda), com o que venderam no periodo. Venda = pedido confirmado em diante. */
    public List<VendaPrato> vendasPorPrato(Timestamp inicio) {
        return jdbc.query("""
                SELECT p.id, p.nome, c.nome AS categoria, p.foto_url, p.preco_venda,
                       COALESCE(SUM(CASE WHEN o.id IS NOT NULL THEN i.quantidade ELSE 0 END), 0) AS qtd,
                       COALESCE(SUM(CASE WHEN o.id IS NOT NULL THEN i.quantidade * i.preco_unitario ELSE 0 END), 0) AS receita
                FROM prato p
                JOIN categoria c ON c.id = p.categoria_id
                LEFT JOIN pedido_item i ON i.prato_id = p.id
                LEFT JOIN pedido o ON o.id = i.pedido_id AND o.created_at >= :ini
                     AND o.status IN ('CONFIRMADO','EM_PREPARO','PRONTO','SAIU_ENTREGA','FINALIZADO')
                WHERE p.status = 'ATIVO'
                GROUP BY p.id, p.nome, c.nome, p.foto_url, p.preco_venda
                ORDER BY p.nome
                """, new MapSqlParameterSource("ini", inicio),
                (rs, n) -> new VendaPrato(rs.getLong("id"), rs.getString("nome"), rs.getString("categoria"),
                        rs.getString("foto_url"), rs.getBigDecimal("preco_venda"), rs.getLong("qtd"),
                        rs.getBigDecimal("receita")));
    }
}
