package com.comandadigital.api.service;

import com.comandadigital.api.dto.CatalogoDtos.*;
import com.comandadigital.api.exception.RecursoNaoEncontradoException;
import com.comandadigital.api.exception.RegraDeNegocioException;
import com.comandadigital.api.repository.CatalogoJdbcRepository;
import com.comandadigital.api.repository.CatalogoJdbcRepository.IngredienteBasico;
import com.comandadigital.api.repository.CatalogoJdbcRepository.Oferta;
import com.comandadigital.api.repository.CompraJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Catalogo de produtos por fornecedor e cotacao comparativa (menor preco). */
@Service
public class CatalogoService {

    private final CatalogoJdbcRepository repo;
    private final CompraJdbcRepository compras;

    public CatalogoService(CatalogoJdbcRepository repo, CompraJdbcRepository compras) {
        this.repo = repo;
        this.compras = compras;
    }

    @Transactional(readOnly = true)
    public List<ProdutoFornecedorResponse> produtos(Long fornecedorId) {
        existeFornecedor(fornecedorId);
        return repo.produtosDoFornecedor(fornecedorId);
    }

    @Transactional
    public List<ProdutoFornecedorResponse> gravar(Long fornecedorId, ProdutoFornecedorRequest r) {
        existeFornecedor(fornecedorId);
        IngredienteBasico ing = repo.ingrediente(r.ingredienteId())
                .orElseThrow(() -> new RegraDeNegocioException("Ingrediente não encontrado"));
        String unidade = r.unidadeVenda() == null || r.unidadeVenda().isBlank() ? ing.unidadePadrao() : r.unidadeVenda().trim().toUpperCase();
        repo.gravar(fornecedorId, ing.id(), r.preco(), unidade);
        return repo.produtosDoFornecedor(fornecedorId);
    }

    @Transactional
    public List<ProdutoFornecedorResponse> remover(Long fornecedorId, Long ingredienteId) {
        existeFornecedor(fornecedorId);
        if (repo.remover(fornecedorId, ingredienteId) == 0) {
            throw new RecursoNaoEncontradoException("Esse ingrediente não está no catálogo do fornecedor");
        }
        return repo.produtosDoFornecedor(fornecedorId);
    }

    @Transactional(readOnly = true)
    public CotacaoResponse cotar(Long ingredienteId) {
        IngredienteBasico ing = repo.ingrediente(ingredienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ingrediente não encontrado"));
        List<Oferta> ofertas = repo.ofertas(ingredienteId);
        BigDecimal menor = ofertas.isEmpty() ? BigDecimal.ZERO : ofertas.get(0).preco();
        List<CotacaoLinha> linhas = ofertas.stream().map(o -> new CotacaoLinha(o.fornecedorId(), o.fornecedorNome(), o.preco(),
                o.unidadeVenda(), diferenca(o.preco(), menor), o.preco().compareTo(menor) == 0)).toList();
        return new CotacaoResponse(ing.id(), ing.nome(), ing.unidadePadrao(), ing.custoUnitario(), linhas);
    }

    /** Quanto o preco e mais caro que o menor, em %. */
    private static BigDecimal diferenca(BigDecimal preco, BigDecimal menor) {
        if (menor.signum() == 0) return BigDecimal.ZERO;
        return preco.subtract(menor).multiply(BigDecimal.valueOf(100)).divide(menor, 1, RoundingMode.HALF_UP);
    }

    private void existeFornecedor(Long id) {
        compras.buscarFornecedor(id).orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado"));
    }
}
