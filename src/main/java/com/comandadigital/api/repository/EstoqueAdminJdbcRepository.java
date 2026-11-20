package com.comandadigital.api.repository;

import com.comandadigital.api.dto.EstoqueDtos.MovimentacaoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/** Movimentacoes manuais e historico do estoque. */
@Repository
public class EstoqueAdminJdbcRepository {

    public record IngredienteBasico(Long id, String nome, String unidade, String status) {}

    private final NamedParameterJdbcTemplate jdbc;

    public EstoqueAdminJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<IngredienteBasico> ingrediente(Long id) {
        return jdbc.query("SELECT id, nome, unidade_padrao, status FROM ingrediente WHERE id = :id", Map.of("id", id),
                (rs, n) -> new IngredienteBasico(rs.getLong("id"), rs.getString("nome"), rs.getString("unidade_padrao"),
                        rs.getString("status"))).stream().findFirst();
    }

    public void inserir(Long ingredienteId, String tipo, BigDecimal quantidade, String motivo, String lote,
                        LocalDate validade, BigDecimal custoUnitario, String observacao, Long usuarioId) {
        jdbc.update("""
                INSERT INTO estoque_movimentacao (ingrediente_id, tipo, quantidade, motivo, lote, validade, custo_unitario, observacao, usuario_id)
                VALUES (:ing, :tipo, :qtd, :motivo, :lote, :validade, :custo, :obs, :usuario)
                """, new MapSqlParameterSource().addValue("ing", ingredienteId).addValue("tipo", tipo)
                .addValue("qtd", quantidade).addValue("motivo", motivo).addValue("lote", lote)
                .addValue("validade", validade == null ? null : Date.valueOf(validade)).addValue("custo", custoUnitario)
                .addValue("obs", observacao).addValue("usuario", usuarioId));
    }

    public Page<MovimentacaoResponse> listar(Long ingredienteId, String tipo, Pageable pageable) {
        String where = " WHERE (:ing IS NULL OR m.ingrediente_id = :ing) AND (:tipo IS NULL OR m.tipo = :tipo) ";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("ing", ingredienteId).addValue("tipo", tipo)
                .addValue("lim", pageable.getPageSize()).addValue("off", pageable.getOffset());
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM estoque_movimentacao m" + where, p, Long.class);
        var linhas = jdbc.query("""
                SELECT m.id, m.ingrediente_id, i.nome AS ingrediente_nome, i.unidade_padrao, m.tipo, m.motivo, m.quantidade,
                       m.lote, m.validade, m.custo_unitario, m.observacao, u.nome AS usuario_nome, m.pedido_id, m.created_at
                FROM estoque_movimentacao m
                JOIN ingrediente i ON i.id = m.ingrediente_id
                JOIN usuario u ON u.id = m.usuario_id
                """ + where + " ORDER BY m.created_at DESC, m.id DESC LIMIT :lim OFFSET :off", p,
                (rs, n) -> new MovimentacaoResponse(rs.getLong("id"), rs.getLong("ingrediente_id"),
                        rs.getString("ingrediente_nome"), rs.getString("unidade_padrao"), rs.getString("tipo"),
                        rs.getString("motivo"), rs.getBigDecimal("quantidade"), rs.getString("lote"),
                        rs.getDate("validade") == null ? null : rs.getDate("validade").toLocalDate(),
                        rs.getBigDecimal("custo_unitario"), rs.getString("observacao"), rs.getString("usuario_nome"),
                        rs.getObject("pedido_id") == null ? null : rs.getLong("pedido_id"),
                        rs.getTimestamp("created_at").toLocalDateTime()));
        return new PageImpl<>(linhas, pageable, total == null ? 0 : total);
    }
}
