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

    /** Registra uma movimentacao do estoque (SAIDA na confirmacao da venda, ESTORNO no cancelamento). */
    public void registrarMovimentacao(Long ingredienteId, String tipo, BigDecimal quantidade, String motivo,
                                      Long pedidoId, Long usuarioId) {
        jdbc.update("""
                INSERT INTO estoque_movimentacao (ingrediente_id, tipo, quantidade, motivo, pedido_id, usuario_id)
                VALUES (:ing, :tipo, :qtd, :motivo, :pedido, :usuario)
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("ing", ingredienteId).addValue("tipo", tipo).addValue("qtd", quantidade)
                .addValue("motivo", motivo).addValue("pedido", pedidoId).addValue("usuario", usuarioId));
    }

    /** Movimentacoes de SAIDA ja feitas por um pedido (para estornar exatamente o que saiu). */
    public Map<Long, BigDecimal> saidasDoPedido(Long pedidoId) {
        List<SaldoLinha> linhas = jdbc.query("""
                SELECT ingrediente_id, SUM(quantidade) AS saldo FROM estoque_movimentacao
                WHERE pedido_id = :id AND tipo = 'SAIDA' AND motivo = 'VENDA'
                GROUP BY ingrediente_id
                """, Map.of("id", pedidoId),
                (rs, n) -> new SaldoLinha(rs.getLong("ingrediente_id"), rs.getBigDecimal("saldo")));
        Map<Long, BigDecimal> mapa = new HashMap<>();
        for (SaldoLinha l : linhas) mapa.put(l.ingredienteId(), l.saldo());
        return mapa;
    }

    /** Estornos ja feitos por um pedido (evita estornar duas vezes). */
    public boolean jaEstornado(Long pedidoId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM estoque_movimentacao WHERE pedido_id = :id AND tipo = 'ESTORNO'",
                Map.of("id", pedidoId), Integer.class);
        return n != null && n > 0;
    }
}
