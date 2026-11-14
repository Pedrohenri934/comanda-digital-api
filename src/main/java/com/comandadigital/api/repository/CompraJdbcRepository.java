package com.comandadigital.api.repository;

import com.comandadigital.api.dto.CompraDtos.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** SQL de fornecedores e pedidos de compra. */
@Repository
public class CompraJdbcRepository {

    public record CompraCabecalho(Long id, String status) {}

    private final NamedParameterJdbcTemplate jdbc;

    public CompraJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------ fornecedores

    private static FornecedorResponse fornecedor(ResultSet rs) throws SQLException {
        return new FornecedorResponse(rs.getLong("id"), rs.getString("razao_social"), rs.getString("cnpj"),
                rs.getString("telefone"), rs.getString("email"), rs.getString("status"));
    }

    public List<FornecedorResponse> listarFornecedores() {
        return jdbc.query("SELECT id, razao_social, cnpj, telefone, email, status FROM fornecedor ORDER BY razao_social",
                (rs, n) -> fornecedor(rs));
    }

    public Optional<FornecedorResponse> buscarFornecedor(Long id) {
        return jdbc.query("SELECT id, razao_social, cnpj, telefone, email, status FROM fornecedor WHERE id = :id",
                Map.of("id", id), (rs, n) -> fornecedor(rs)).stream().findFirst();
    }

    public Long inserirFornecedor(String razao, String cnpj, String telefone, String email) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO fornecedor (razao_social, cnpj, telefone, email, status) VALUES (:r, :c, :t, :e, 'ATIVO')",
                new MapSqlParameterSource().addValue("r", razao).addValue("c", cnpj).addValue("t", telefone).addValue("e", email), kh);
        return kh.getKey().longValue();
    }

    public void atualizarFornecedor(Long id, String razao, String cnpj, String telefone, String email) {
        jdbc.update("UPDATE fornecedor SET razao_social = :r, cnpj = :c, telefone = :t, email = :e WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("r", razao).addValue("c", cnpj)
                        .addValue("t", telefone).addValue("e", email));
    }

    public void statusFornecedor(Long id, String status) {
        jdbc.update("UPDATE fornecedor SET status = :s WHERE id = :id", Map.of("s", status, "id", id));
    }

    /** Compras ainda abertas (rascunho ou enviadas) do fornecedor: impedem inativa-lo. */
    public int comprasAbertasDoFornecedor(Long fornecedorId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pedido_compra WHERE fornecedor_id = :id AND status IN ('RASCUNHO','ENVIADO')",
                Map.of("id", fornecedorId), Integer.class);
        return n == null ? 0 : n;
    }

    // ------------------------------------------------------------ compras

    private static final String SELECT_COMPRA = """
            SELECT c.id, c.fornecedor_id, f.razao_social, c.status, c.valor_total, c.created_at
            FROM pedido_compra c JOIN fornecedor f ON f.id = c.fornecedor_id
            """;

    private record Cab(Long id, Long fornecedorId, String fornecedorNome, String status, BigDecimal total,
                       java.time.LocalDateTime criadoEm) {}

    private static Cab cab(ResultSet rs) throws SQLException {
        return new Cab(rs.getLong("id"), rs.getLong("fornecedor_id"), rs.getString("razao_social"),
                rs.getString("status"), rs.getBigDecimal("valor_total"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private List<CompraResponse> comItens(List<Cab> cabs) {
        if (cabs.isEmpty()) return List.of();
        List<Long> ids = cabs.stream().map(Cab::id).toList();
        Map<Long, List<CompraItemResponse>> porCompra = new LinkedHashMap<>();
        jdbc.query("""
                SELECT i.pedido_compra_id, i.ingrediente_id, g.nome, g.unidade_padrao, i.quantidade, i.preco_unitario, i.subtotal
                FROM pedido_compra_item i JOIN ingrediente g ON g.id = i.ingrediente_id
                WHERE i.pedido_compra_id IN (:ids) ORDER BY g.nome
                """, Map.of("ids", ids), rs -> {
            porCompra.computeIfAbsent(rs.getLong("pedido_compra_id"), k -> new ArrayList<>())
                    .add(new CompraItemResponse(rs.getLong("ingrediente_id"), rs.getString("nome"), rs.getString("unidade_padrao"),
                            rs.getBigDecimal("quantidade"), rs.getBigDecimal("preco_unitario"), rs.getBigDecimal("subtotal")));
        });
        return cabs.stream().map(c -> new CompraResponse(c.id(), c.fornecedorId(), c.fornecedorNome(), c.status(), c.total(),
                c.criadoEm(), porCompra.getOrDefault(c.id(), List.of()))).toList();
    }

    public List<CompraResponse> listarCompras(String status) {
        List<Cab> cabs;
        if (status == null) {
            cabs = jdbc.query(SELECT_COMPRA + " ORDER BY c.id DESC", (rs, n) -> cab(rs));
        } else {
            cabs = jdbc.query(SELECT_COMPRA + " WHERE c.status = :s ORDER BY c.id DESC", Map.of("s", status), (rs, n) -> cab(rs));
        }
        return comItens(cabs);
    }

    public Optional<CompraResponse> buscarCompra(Long id) {
        List<Cab> cabs = jdbc.query(SELECT_COMPRA + " WHERE c.id = :id", Map.of("id", id), (rs, n) -> cab(rs));
        return comItens(cabs).stream().findFirst();
    }

    /** Trava a linha da compra (FOR UPDATE) para evitar receber a mesma compra duas vezes. */
    public Optional<CompraCabecalho> buscarCompraComTrava(Long id) {
        return jdbc.query("SELECT id, status FROM pedido_compra WHERE id = :id FOR UPDATE", Map.of("id", id),
                (rs, n) -> new CompraCabecalho(rs.getLong("id"), rs.getString("status"))).stream().findFirst();
    }

    public Long inserirCompra(Long fornecedorId, Long usuarioId) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO pedido_compra (fornecedor_id, status, valor_total, usuario_id) VALUES (:f, 'RASCUNHO', 0, :u)",
                new MapSqlParameterSource().addValue("f", fornecedorId).addValue("u", usuarioId), kh);
        return kh.getKey().longValue();
    }

    /** Troca os itens da compra e recalcula o total (sempre no servidor). */
    public void gravarItens(Long compraId, Long fornecedorId, List<CompraItemRequest> itens) {
        jdbc.update("DELETE FROM pedido_compra_item WHERE pedido_compra_id = :id", Map.of("id", compraId));
        BigDecimal total = BigDecimal.ZERO;
        for (CompraItemRequest it : itens) {
            BigDecimal subtotal = it.quantidade().multiply(it.precoUnitario()).setScale(2, java.math.RoundingMode.HALF_UP);
            total = total.add(subtotal);
            jdbc.update("""
                    INSERT INTO pedido_compra_item (pedido_compra_id, ingrediente_id, quantidade, preco_unitario, subtotal)
                    VALUES (:c, :i, :q, :p, :s)
                    """, new MapSqlParameterSource().addValue("c", compraId).addValue("i", it.ingredienteId())
                    .addValue("q", it.quantidade()).addValue("p", it.precoUnitario()).addValue("s", subtotal));
        }
        jdbc.update("UPDATE pedido_compra SET valor_total = :t, fornecedor_id = :f WHERE id = :id",
                new MapSqlParameterSource().addValue("t", total).addValue("f", fornecedorId).addValue("id", compraId));
    }

    public void statusCompra(Long id, String status) {
        jdbc.update("UPDATE pedido_compra SET status = :s WHERE id = :id", Map.of("s", status, "id", id));
    }

    /** Da entrada (ENTRADA / COMPRA) de cada item recebido no estoque. */
    public void darEntradaNoEstoque(Long compraId, Long usuarioId) {
        jdbc.update("""
                INSERT INTO estoque_movimentacao (ingrediente_id, tipo, quantidade, motivo, custo_unitario, pedido_compra_id, usuario_id)
                SELECT ingrediente_id, 'ENTRADA', quantidade, 'COMPRA', preco_unitario, pedido_compra_id, :u
                FROM pedido_compra_item WHERE pedido_compra_id = :c
                """, new MapSqlParameterSource().addValue("u", usuarioId).addValue("c", compraId));
    }

    /** Ingredientes (id -> status) para validar uma compra. */
    public Map<Long, String> statusDosIngredientes(Collection<Long> ids) {
        Map<Long, String> mapa = new LinkedHashMap<>();
        jdbc.query("SELECT id, status FROM ingrediente WHERE id IN (:ids)", Map.of("ids", ids),
                rs -> { mapa.put(rs.getLong("id"), rs.getString("status")); });
        return mapa;
    }
}
