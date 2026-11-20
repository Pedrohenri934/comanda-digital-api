package com.comandadigital.api.controller;

import com.comandadigital.api.dto.CatalogoDtos.*;
import com.comandadigital.api.service.CatalogoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Catalogo de produtos por fornecedor e cotacao comparativa. ADMIN e GERENTE. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class CatalogoAdminController {

    private final CatalogoService service;

    public CatalogoAdminController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping("/fornecedores/{id}/produtos")
    public List<ProdutoFornecedorResponse> produtos(@PathVariable Long id) { return service.produtos(id); }

    /** Adiciona o ingrediente ao catalogo ou atualiza o preco dele. */
    @PutMapping("/fornecedores/{id}/produtos")
    public List<ProdutoFornecedorResponse> gravar(@PathVariable Long id, @Valid @RequestBody ProdutoFornecedorRequest r) {
        return service.gravar(id, r);
    }

    @DeleteMapping("/fornecedores/{id}/produtos/{ingredienteId}")
    public List<ProdutoFornecedorResponse> remover(@PathVariable Long id, @PathVariable Long ingredienteId) {
        return service.remover(id, ingredienteId);
    }

    /** Cotacao: quem vende o ingrediente e por quanto, do mais barato ao mais caro. */
    @GetMapping("/cotacao/{ingredienteId}")
    public CotacaoResponse cotar(@PathVariable Long ingredienteId) { return service.cotar(ingredienteId); }
}
