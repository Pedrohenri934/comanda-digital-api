package com.comandadigital.api.controller;

import com.comandadigital.api.dto.CardapioSemanaDtos.DiaCardapio;
import com.comandadigital.api.dto.CardapioSemanaDtos.SalvarDiaRequest;
import com.comandadigital.api.service.CardapioSemanaService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Edicao do cardapio da semana. Somente ADMIN e GERENTE. */
@RestController
@RequestMapping("/api/admin/cardapio-semana")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class CardapioSemanaAdminController {

    private final CardapioSemanaService service;

    public CardapioSemanaAdminController(CardapioSemanaService service) {
        this.service = service;
    }

    @GetMapping
    public List<DiaCardapio> listar() {
        return service.listar();
    }

    /** dia: 1 = segunda ... 7 = domingo. Substitui os pratos daquele dia. */
    @PutMapping("/{dia}")
    public DiaCardapio salvarDia(@PathVariable int dia, @Valid @RequestBody SalvarDiaRequest req) {
        return service.salvarDia(dia, req);
    }
}
