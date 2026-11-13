package com.comandadigital.api.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Consultas de leitura usadas para conferir se ha estoque para um pedido (RN03). */
@Repository
public class EstoqueConsultaRepository {

    public record FichaLinha(Long pratoId, Long ingredienteId, String ingredienteNome, String unidadePadrao,
                             String unidadeItem, BigDecimal quantidade, BigDecimal fatorCorrecao, int rendimento) {}

    private record SaldoLinha(Long ingredienteId, BigDecimal saldo) {}

    private final NamedParameterJdbcTemplate jdbc;

    public EstoqueConsultaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Ingredientes de cada prato (ficha tecnica). */
    public List<FichaLinha> fichas(Collection<Long> pratoIds) {
        String sql = """
                SELECT ft.prato_id, fti.ingrediente_id, i.nome AS ingrediente_nome, i.unidade_padrao,
                       fti.unidade AS unidade_item, fti.quantidade, fti.fator_correcao, ft.rendimento
                FROM ficha_tecnica ft
                JOIN ficha_tecnica_item fti ON fti.ficha_tecnica_id = ft.id
                JOIN ingrediente i ON i.id = fti.ingrediente_id
                WHERE ft.prato_id IN (:ids)
                """;
        return jdbc.query(sql, Map.of("ids", pratoIds), (rs, n) -> new FichaLinha(
                rs.getLong("prato_id"),
                rs.getLong("ingrediente_id"),
                rs.getString("ingrediente_nome"),
                rs.getString("unidade_padrao"),
                rs.getString("unidade_item"),
                rs.getBigDecimal("quantidade"),
                rs.getBigDecimal("fator_correcao"),
                rs.getInt("rendimento")));
    }

    /** Saldo atual = entradas e estornos menos saidas. */
    public Map<Long, BigDecimal> saldos(Collection<Long> ingredienteIds) {
        String sql = """
                SELECT ingrediente_id,
                       SUM(CASE WHEN tipo = 'SAIDA' THEN -quantidade ELSE quantidade END) AS saldo
                FROM estoque_movimentacao
                WHERE ingrediente_id IN (:ids)
                GROUP BY ingrediente_id
                """;
        List<SaldoLinha> linhas = jdbc.query(sql, Map.of("ids", ingredienteIds),
                (rs, n) -> new SaldoLinha(rs.getLong("ingrediente_id"), rs.getBigDecimal("saldo")));
        Map<Long, BigDecimal> mapa = new HashMap<>();
        for (SaldoLinha l : linhas) mapa.put(l.ingredienteId(), l.saldo());
        return mapa;
    }
}
