package com.comandadigital.api.repository;

import com.comandadigital.api.dto.AdminDtos.FichaItemRequest;
import com.comandadigital.api.dto.AdminDtos.FichaItemResponse;
import com.comandadigital.api.dto.AdminDtos.IngredienteRequest;
import com.comandadigital.api.dto.AdminDtos.IngredienteResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Acesso (SQL) a ingredientes e ficha tecnica, usado pelas telas de cadastro do admin. */
@Repository
public class CadastroJdbcRepository {

    public record FichaCabecalho(Long id, Integer rendimento, String modoPreparo) {}

    private record CustoLinha(Long pratoId, BigDecimal custo) {}

    // Converte a quantidade da ficha para a unidade padrao do ingrediente (mesma regra do EstoqueService).
    private static final String FATOR = """
            CASE WHEN fti.unidade = 'G'  AND i.unidade_padrao = 'KG' THEN 0.001
                 WHEN fti.unidade = 'KG' AND i.unidade_padrao = 'G'  THEN 1000
                 WHEN fti.unidade = 'ML' AND i.unidade_padrao = 'L'  THEN 0.001
                 WHEN fti.unidade = 'L'  AND i.unidade_padrao = 'ML' THEN 1000
                 ELSE 1 END""";

    private static final String SELECT_INGREDIENTE = """
            SELECT i.id, i.nome, i.sku, i.unidade_padrao, i.estoque_minimo, i.custo_unitario, i.status,
                   COALESCE(s.saldo, 0) AS saldo
            FROM ingrediente i
            LEFT JOIN (SELECT ingrediente_id,
                              SUM(CASE WHEN tipo = 'SAIDA' THEN -quantidade ELSE quantidade END) AS saldo
                       FROM estoque_movimentacao GROUP BY ingrediente_id) s ON s.ingrediente_id = i.id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public CadastroJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------ ingredientes

    private IngredienteResponse mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        BigDecimal saldo = rs.getBigDecimal("saldo");
        BigDecimal minimo = rs.getBigDecimal("estoque_minimo");
        return new IngredienteResponse(rs.getLong("id"), rs.getString("nome"), rs.getString("sku"),
                rs.getString("unidade_padrao"), minimo, rs.getBigDecimal("custo_unitario"), rs.getString("status"),
                saldo, saldo.compareTo(minimo) < 0);
    }

    public List<IngredienteResponse> listarIngredientes() {
        return jdbc.query(SELECT_INGREDIENTE + " ORDER BY i.nome", (rs, n) -> mapear(rs));
    }

    public Optional<IngredienteResponse> buscarIngrediente(Long id) {
        return jdbc.query(SELECT_INGREDIENTE + " WHERE i.id = :id", Map.of("id", id), (rs, n) -> mapear(rs))
                .stream().findFirst();
    }

    public Long inserirIngrediente(IngredienteRequest r) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO ingrediente (nome, sku, unidade_padrao, estoque_minimo, custo_unitario, status)
                VALUES (:nome, :sku, :un, :min, :custo, 'ATIVO')
                """, new MapSqlParameterSource().addValue("nome", r.nome().trim()).addValue("sku", r.sku().trim())
                .addValue("un", r.unidadePadrao()).addValue("min", r.estoqueMinimo()).addValue("custo", r.custoUnitario()), kh);
        return kh.getKey().longValue();
    }

    public void atualizarIngrediente(Long id, IngredienteRequest r) {
        jdbc.update("""
                UPDATE ingrediente SET nome = :nome, sku = :sku, unidade_padrao = :un,
                       estoque_minimo = :min, custo_unitario = :custo WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", id).addValue("nome", r.nome().trim())
                .addValue("sku", r.sku().trim()).addValue("un", r.unidadePadrao())
                .addValue("min", r.estoqueMinimo()).addValue("custo", r.custoUnitario()));
    }

    public void alterarStatusIngrediente(Long id, String status) {
        jdbc.update("UPDATE ingrediente SET status = :s WHERE id = :id", Map.of("s", status, "id", id));
    }

    /** Quantos pratos ATIVOS usam o ingrediente (impede inativar um ingrediente em uso). */
    public int pratosAtivosUsando(Long ingredienteId) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT p.id) FROM ficha_tecnica_item fti
                JOIN ficha_tecnica ft ON ft.id = fti.ficha_tecnica_id
                JOIN prato p ON p.id = ft.prato_id
                WHERE fti.ingrediente_id = :id AND p.status = 'ATIVO'
                """, Map.of("id", ingredienteId), Integer.class);
        return n == null ? 0 : n;
    }

    /** Unidade padrao e status dos ingredientes citados (para validar uma ficha). */
    public Map<Long, String[]> unidadesEStatus(Collection<Long> ids) {
        Map<Long, String[]> mapa = new HashMap<>();
        jdbc.query("SELECT id, unidade_padrao, status FROM ingrediente WHERE id IN (:ids)", Map.of("ids", ids),
                rs -> { mapa.put(rs.getLong("id"), new String[]{rs.getString("unidade_padrao"), rs.getString("status")}); });
        return mapa;
    }

    // ------------------------------------------------------------ ficha tecnica

    public Optional<FichaCabecalho> buscarFicha(Long pratoId) {
        return jdbc.query("SELECT id, rendimento, modo_preparo FROM ficha_tecnica WHERE prato_id = :id",
                Map.of("id", pratoId),
                (rs, n) -> new FichaCabecalho(rs.getLong("id"), rs.getInt("rendimento"), rs.getString("modo_preparo")))
                .stream().findFirst();
    }

    public List<FichaItemResponse> itensDaFicha(Long fichaId) {
        return jdbc.query("SELECT fti.ingrediente_id, i.nome, fti.quantidade, fti.unidade, fti.fator_correcao, "
                        + "fti.quantidade * " + FATOR + " * fti.fator_correcao * i.custo_unitario AS custo_linha "
                        + "FROM ficha_tecnica_item fti JOIN ingrediente i ON i.id = fti.ingrediente_id "
                        + "WHERE fti.ficha_tecnica_id = :id ORDER BY i.nome",
                Map.of("id", fichaId),
                (rs, n) -> new FichaItemResponse(rs.getLong("ingrediente_id"), rs.getString("nome"),
                        rs.getBigDecimal("quantidade"), rs.getString("unidade"), rs.getBigDecimal("fator_correcao"),
                        rs.getBigDecimal("custo_linha")));
    }

    /** Cria a ficha (ou atualiza a existente) e troca todos os itens. */
    public void salvarFicha(Long pratoId, int rendimento, String modoPreparo, List<FichaItemRequest> itens) {
        Optional<FichaCabecalho> existente = buscarFicha(pratoId);
        long fichaId;
        if (existente.isPresent()) {
            fichaId = existente.get().id();
            jdbc.update("UPDATE ficha_tecnica SET rendimento = :r, modo_preparo = :m WHERE id = :id",
                    new MapSqlParameterSource().addValue("r", rendimento).addValue("m", modoPreparo).addValue("id", fichaId));
            jdbc.update("DELETE FROM ficha_tecnica_item WHERE ficha_tecnica_id = :id", Map.of("id", fichaId));
        } else {
            KeyHolder kh = new GeneratedKeyHolder();
            jdbc.update("INSERT INTO ficha_tecnica (prato_id, rendimento, modo_preparo) VALUES (:p, :r, :m)",
                    new MapSqlParameterSource().addValue("p", pratoId).addValue("r", rendimento).addValue("m", modoPreparo), kh);
            fichaId = kh.getKey().longValue();
        }
        for (FichaItemRequest it : itens) {
            jdbc.update("""
                    INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao)
                    VALUES (:f, :i, :q, :u, :fc)
                    """, new MapSqlParameterSource().addValue("f", fichaId).addValue("i", it.ingredienteId())
                    .addValue("q", it.quantidade()).addValue("u", it.unidade()).addValue("fc", it.fatorCorrecao()));
        }
    }

    /** Custo por porcao de cada prato que tem ficha: soma(qtd x fator de correcao x custo) / rendimento. */
    public Map<Long, BigDecimal> custosPorPrato(Collection<Long> pratoIds) {
        if (pratoIds.isEmpty()) return Map.of();
        List<CustoLinha> linhas = jdbc.query("SELECT ft.prato_id, "
                        + "SUM(fti.quantidade * " + FATOR + " * fti.fator_correcao * i.custo_unitario) / ft.rendimento AS custo "
                        + "FROM ficha_tecnica ft JOIN ficha_tecnica_item fti ON fti.ficha_tecnica_id = ft.id "
                        + "JOIN ingrediente i ON i.id = fti.ingrediente_id "
                        + "WHERE ft.prato_id IN (:ids) GROUP BY ft.prato_id, ft.rendimento",
                Map.of("ids", pratoIds),
                (rs, n) -> new CustoLinha(rs.getLong("prato_id"), rs.getBigDecimal("custo")));
        Map<Long, BigDecimal> mapa = new HashMap<>();
        for (CustoLinha l : linhas) mapa.put(l.pratoId(), l.custo());
        return mapa;
    }
}
