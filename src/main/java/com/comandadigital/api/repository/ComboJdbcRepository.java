package com.comandadigital.api.repository;

import com.comandadigital.api.dto.ComboDtos.GrupoRequest;
import com.comandadigital.api.dto.ComboDtos.OpcaoRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Grupos de escolha e opcoes dos combos. */
@Repository
public class ComboJdbcRepository {

    public record GrupoRow(Long id, Long pratoId, String nome, int minimo, int maximo) {}

    public record OpcaoRow(Long id, Long grupoId, String nome, Long pratoId, BigDecimal acrescimo, boolean disponivel) {}

    private final NamedParameterJdbcTemplate jdbc;

    public ComboJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<GrupoRow> grupos(Collection<Long> pratoIds) {
        if (pratoIds.isEmpty()) return List.of();
        return jdbc.query("SELECT id, prato_id, nome, minimo, maximo FROM combo_grupo WHERE prato_id IN (:ids) ORDER BY ordem, id",
                Map.of("ids", pratoIds),
                (rs, n) -> new GrupoRow(rs.getLong("id"), rs.getLong("prato_id"), rs.getString("nome"),
                        rs.getInt("minimo"), rs.getInt("maximo")));
    }

    public List<OpcaoRow> opcoes(Collection<Long> grupoIds) {
        if (grupoIds.isEmpty()) return List.of();
        return jdbc.query("""
                SELECT o.id, o.grupo_id, o.nome, o.prato_id, o.acrescimo,
                       (o.prato_id IS NULL OR pr.status = 'ATIVO') AS disponivel
                FROM combo_opcao o LEFT JOIN prato pr ON pr.id = o.prato_id
                WHERE o.grupo_id IN (:ids) ORDER BY o.ordem, o.id
                """, Map.of("ids", grupoIds),
                (rs, n) -> new OpcaoRow(rs.getLong("id"), rs.getLong("grupo_id"), rs.getString("nome"),
                        rs.getObject("prato_id") == null ? null : rs.getLong("prato_id"),
                        rs.getBigDecimal("acrescimo"), rs.getBoolean("disponivel")));
    }

    public Set<Long> pratosComOpcoes(Collection<Long> pratoIds) {
        if (pratoIds.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.queryForList("SELECT DISTINCT prato_id FROM combo_grupo WHERE prato_id IN (:ids)",
                Map.of("ids", pratoIds), Long.class));
    }

    public boolean pratoExiste(Long id) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM prato WHERE id = :id", Map.of("id", id), Integer.class);
        return n != null && n > 0;
    }

    /** Troca todos os grupos e opcoes do prato pelos enviados (o historico dos pedidos nao e afetado). */
    public void substituir(Long pratoId, List<GrupoRequest> grupos) {
        jdbc.update("DELETE FROM combo_opcao WHERE grupo_id IN (SELECT id FROM combo_grupo WHERE prato_id = :p)", Map.of("p", pratoId));
        jdbc.update("DELETE FROM combo_grupo WHERE prato_id = :p", Map.of("p", pratoId));
        int ordemG = 1;
        for (GrupoRequest g : grupos) {
            KeyHolder kh = new GeneratedKeyHolder();
            jdbc.update("INSERT INTO combo_grupo (prato_id, nome, minimo, maximo, ordem) VALUES (:p, :n, :mi, :ma, :o)",
                    new MapSqlParameterSource().addValue("p", pratoId).addValue("n", g.nome().trim())
                            .addValue("mi", g.minimo()).addValue("ma", g.maximo()).addValue("o", ordemG++), kh);
            long grupoId = kh.getKey().longValue();
            int ordemO = 1;
            for (OpcaoRequest o : g.opcoes()) {
                jdbc.update("INSERT INTO combo_opcao (grupo_id, nome, prato_id, acrescimo, ordem) VALUES (:g, :n, :p, :a, :o)",
                        new MapSqlParameterSource().addValue("g", grupoId).addValue("n", o.nome().trim())
                                .addValue("p", o.pratoId()).addValue("a", o.acrescimo()).addValue("o", ordemO++));
            }
        }
    }
}
