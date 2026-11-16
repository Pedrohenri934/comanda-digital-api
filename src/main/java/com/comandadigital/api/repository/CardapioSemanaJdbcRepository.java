package com.comandadigital.api.repository;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
public class CardapioSemanaJdbcRepository {

    public record Linha(int dia, long pratoId, String nome, String descricao, String categoria, String fotoUrl,
                        BigDecimal preco, String status, boolean destaque, int tempoPreparoMin) {}

    private static final String SELECT = """
            SELECT cs.dia_semana, cs.prato_id, p.nome, p.descricao, c.nome AS categoria, p.foto_url,
                   p.preco_venda, p.status, cs.destaque, p.tempo_preparo_min
            FROM cardapio_semana cs
            JOIN prato p ON p.id = cs.prato_id
            JOIN categoria c ON c.id = p.categoria_id
            """;

    private static final RowMapper<Linha> MAPA = (rs, n) -> new Linha(
            rs.getInt("dia_semana"), rs.getLong("prato_id"), rs.getString("nome"), rs.getString("descricao"),
            rs.getString("categoria"), rs.getString("foto_url"), rs.getBigDecimal("preco_venda"),
            rs.getString("status"), rs.getBoolean("destaque"), rs.getInt("tempo_preparo_min"));

    private final NamedParameterJdbcTemplate jdbc;

    public CardapioSemanaJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Linha> listarTodos() {
        return jdbc.query(SELECT + " ORDER BY cs.dia_semana, cs.destaque DESC, c.ordem, p.nome",
                new MapSqlParameterSource(), MAPA);
    }

    /** Pratos de um dia que o cliente pode pedir: prato e categoria ativos. */
    public List<Linha> listarDiaAtivos(int dia) {
        return jdbc.query(SELECT + " WHERE cs.dia_semana = :dia AND p.status = 'ATIVO' AND c.status = 'ATIVO' "
                        + "ORDER BY cs.destaque DESC, c.ordem, p.nome",
                new MapSqlParameterSource("dia", dia), MAPA);
    }

    public int contarPratos(Collection<Long> ids) {
        if (ids.isEmpty()) return 0;
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM prato WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", ids), Integer.class);
        return n == null ? 0 : n;
    }

    public void limparDia(int dia) {
        jdbc.update("DELETE FROM cardapio_semana WHERE dia_semana = :dia", Map.of("dia", dia));
    }

    public void inserir(int dia, long pratoId, boolean destaque) {
        jdbc.update("INSERT INTO cardapio_semana (dia_semana, prato_id, destaque) VALUES (:dia, :prato, :dest)",
                new MapSqlParameterSource().addValue("dia", dia).addValue("prato", pratoId).addValue("dest", destaque));
    }
}
