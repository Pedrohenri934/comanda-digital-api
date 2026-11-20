package com.comandadigital.api.controller;

import com.comandadigital.api.dto.AdminDtos.IngredienteResponse;
import com.comandadigital.api.dto.EstoqueDtos.*;
import com.comandadigital.api.service.EstoqueAdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Estoque: saldo, alertas, historico e movimentacoes manuais. ADMIN e GERENTE. */
@RestController
@RequestMapping("/api/admin/estoque")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class EstoqueAdminController {

    private final EstoqueAdminService service;

    public EstoqueAdminController(EstoqueAdminService service) {
        this.service = service;
    }

    @GetMapping("/saldo")
    public List<IngredienteResponse> saldo() { return service.saldo(); }

    /** Ingredientes abaixo do estoque minimo. */
    @GetMapping("/alertas")
    public List<IngredienteResponse> alertas() { return service.alertas(); }

    @GetMapping("/movimentacoes")
    public Page<MovimentacaoResponse> movimentacoes(@RequestParam(required = false) Long ingredienteId,
                                                    @RequestParam(required = false) String tipo,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "30") int size) {
        return service.movimentacoes(ingredienteId, tipo, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
    }

    @PostMapping("/entrada")
    @ResponseStatus(HttpStatus.CREATED)
    public void entrada(@Valid @RequestBody EntradaRequest r, Authentication auth) { service.entrada(r, auth.getName()); }

    @PostMapping("/saida")
    @ResponseStatus(HttpStatus.CREATED)
    public void saida(@Valid @RequestBody SaidaRequest r, Authentication auth) { service.saida(r, auth.getName()); }
}
