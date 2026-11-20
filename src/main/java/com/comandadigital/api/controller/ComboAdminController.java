package com.comandadigital.api.controller;

import com.comandadigital.api.dto.ComboDtos.GrupoResponse;
import com.comandadigital.api.dto.ComboDtos.OpcoesRequest;
import com.comandadigital.api.service.ComboService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Opcoes de escolha dos combos (grupos e opcoes). Somente ADMIN e GERENTE. */
@RestController
@RequestMapping("/api/admin/pratos/{pratoId}/opcoes")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class ComboAdminController {

    private final ComboService service;

    public ComboAdminController(ComboService service) {
        this.service = service;
    }

    @GetMapping
    public List<GrupoResponse> listar(@PathVariable Long pratoId) { return service.listar(pratoId); }

    /** Troca todos os grupos e opcoes do prato. Lista vazia remove as escolhas. */
    @PutMapping
    public List<GrupoResponse> salvar(@PathVariable Long pratoId, @Valid @RequestBody OpcoesRequest req) {
        return service.salvar(pratoId, req);
    }
}
