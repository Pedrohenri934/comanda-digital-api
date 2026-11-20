package com.comandadigital.api.repository;

import com.comandadigital.api.dto.CatalogoDtos.ProdutoFornecedorResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** SQL do catalogo fornecedor_produto e da cotacao. */
@Repository
public class CatalogoJdbcRepository {

    public record Oferta(Long fornecedorId, String fornecedorNome, BigDecimal preco, String unidadeVenda) {}
    public record IngredienteBasico(Long id, String nome, String unidadePadrao, BigDecimal custoUnitario) {}

    private final NamedParameterJdbcTemplate jdbc;

    public CatalogoJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProdutoFornecedorResponse> produtosDoFornecedor(Long fornecedorId) {
        return jdbc.query("""
                SELECT p.ingrediente_id, g.nome, g.unidade_padrao, p.preco, p.unidade_venda
                FROM fornecedor_produto p JOIN ingrediente g ON g.id = p.ingrediente_id
                WHERE p.fornecedor_id = :f ORDER BY g.nome
                """, Map.of("f", fornecedorId), (rs, n) -> new ProdutoFornecedorResponse(rs.getLong("ingrediente_id"),
                rs.getString("nome"), rs.getString("unidade_padrao"), rs.getBigDecimal("preco"), rs.getString("unidade_venda")));
    }

    /** Insere ou atualiza o preco do ingrediente naquele fornecedor. */
    public void gravar(Long fornecedorId, Long ingredienteId, BigDecimal preco, String unidadeVenda) {
        int n = jdbc.update("UPDATE fornecedor_produto SET preco = :p, unidade_venda = :u WHERE fornecedor_id = :f AND ingrediente_id = :i",
                new MapSqlParameterSource().addValue("p", preco).addValue("u", unidadeVenda).addValue("f", fornecedorId).addValue("i", ingredienteId));
        if (n == 0) {
            jdbc.update("INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda) VALUES (:f, :i, :p, :u)",
                    new MapSqlParameterSource().addValue("p", preco).addValue("u", unidadeVenda).addValue("f", fornecedorId).addValue("i", ingredienteId));
        }
    }

    public int remover(Long fornecedorId, Long ingredienteId) {
        return jdbc.update("DELETE FROM fornecedor_produto WHERE fornecedor_id = :f AND ingrediente_id = :i",
                Map.of("f", fornecedorId, "i", ingredienteId));
    }

    public Optional<IngredienteBasico> ingrediente(Long id) {
        return jdbc.query("SELECT id, nome, unidade_padrao, custo_unitario FROM ingrediente WHERE id = :id", Map.of("id", id),
                (rs, n) -> new IngredienteBasico(rs.getLong("id"), rs.getString("nome"), rs.getString("unidade_padrao"),
                        rs.getBigDecimal("custo_unitario"))).stream().findFirst();
    }

    /** Fornecedores ATIVOS que vendem o ingrediente, do mais barato ao mais caro. */
    public List<Oferta> ofertas(Long ingredienteId) {
        return jdbc.query("""
                SELECT f.id, f.razao_social, p.preco, p.unidade_venda
                FROM fornecedor_produto p JOIN fornecedor f ON f.id = p.fornecedor_id
                WHERE p.ingrediente_id = :i AND f.status = 'ATIVO' ORDER BY p.preco ASC, f.razao_social
                """, Map.of("i", ingredienteId), (rs, n) -> new Oferta(rs.getLong("id"), rs.getString("razao_social"),
                rs.getBigDecimal("preco"), rs.getString("unidade_venda")));
    }
}
