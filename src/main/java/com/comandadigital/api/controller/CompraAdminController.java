package com.comandadigital.api.controller;

import com.comandadigital.api.dto.CompraDtos.*;
import com.comandadigital.api.service.CompraAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Fornecedores e compras. Somente ADMIN e GERENTE. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class CompraAdminController {

    private final CompraAdminService service;

    public CompraAdminController(CompraAdminService service) {
        this.service = service;
    }

    // ------------------------------------------------------------ fornecedores

    @GetMapping("/fornecedores")
    public List<FornecedorResponse> listarFornecedores() { return service.listarFornecedores(); }

    @PostMapping("/fornecedores")
    @ResponseStatus(HttpStatus.CREATED)
    public FornecedorResponse criarFornecedor(@Valid @RequestBody FornecedorRequest r) { return service.criarFornecedor(r); }

    @PutMapping("/fornecedores/{id}")
    public FornecedorResponse atualizarFornecedor(@PathVariable Long id, @Valid @RequestBody FornecedorRequest r) {
        return service.atualizarFornecedor(id, r);
    }

    /** Exclusao logica (RN06). */
    @DeleteMapping("/fornecedores/{id}")
    public FornecedorResponse inativarFornecedor(@PathVariable Long id) { return service.alterarStatusFornecedor(id, false); }

    @PostMapping("/fornecedores/{id}/reativar")
    public FornecedorResponse reativarFornecedor(@PathVariable Long id) { return service.alterarStatusFornecedor(id, true); }

    // ------------------------------------------------------------ compras

    @GetMapping("/compras")
    public List<CompraResponse> listarCompras(@RequestParam(required = false) String status) { return service.listarCompras(status); }

    @GetMapping("/compras/{id}")
    public CompraResponse buscarCompra(@PathVariable Long id) { return service.buscarCompra(id); }

    @PostMapping("/compras")
    @ResponseStatus(HttpStatus.CREATED)
    public CompraResponse criarCompra(@Valid @RequestBody CompraRequest r, Authentication auth) {
        return service.criarCompra(r, auth.getName());
    }

    @PutMapping("/compras/{id}")
    public CompraResponse atualizarCompra(@PathVariable Long id, @Valid @RequestBody CompraRequest r) {
        return service.atualizarCompra(id, r);
    }

    /** Atalho do SRS: marca a compra como RECEBIDA (da entrada no estoque). */
    @PatchMapping("/compras/{id}/receber")
    public CompraResponse receber(@PathVariable Long id, Authentication auth) {
        return service.alterarStatus(id, "RECEBIDO", auth.getName());
    }

    /** RASCUNHO > ENVIADO > RECEBIDO (da entrada no estoque) ou CANCELADO. */
    @PatchMapping("/compras/{id}/status")
    public CompraResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody CompraStatusRequest r, Authentication auth) {
        return service.alterarStatus(id, r.status(), auth.getName());
    }
}
