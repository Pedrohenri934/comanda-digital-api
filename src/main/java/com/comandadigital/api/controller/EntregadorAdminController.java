package com.comandadigital.api.controller;

import com.comandadigital.api.dto.EntregadorDtos.EntregadorAdmin;
import com.comandadigital.api.dto.EntregadorDtos.StatusEntregadorRequest;
import com.comandadigital.api.service.EntregadorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestao de entregadores: aprovar, recusar ou remover. Somente ADMIN e GERENTE. */
@RestController
@RequestMapping("/api/admin/entregadores")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class EntregadorAdminController {

    private final EntregadorService service;

    public EntregadorAdminController(EntregadorService service) {
        this.service = service;
    }

    @GetMapping
    public List<EntregadorAdmin> listar() {
        return service.listarParaAdmin();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> alterarStatus(@PathVariable Long id, @Valid @RequestBody StatusEntregadorRequest req) {
        service.alterarStatus(id, req.status());
        return ResponseEntity.noContent().build();
    }
}
