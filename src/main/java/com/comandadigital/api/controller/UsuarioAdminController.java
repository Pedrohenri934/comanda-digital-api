package com.comandadigital.api.controller;

import com.comandadigital.api.dto.UsuarioAdminDtos.*;
import com.comandadigital.api.service.UsuarioAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Usuarios internos (RF-041). Somente ADMIN. */
@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioAdminController {

    private final UsuarioAdminService service;

    public UsuarioAdminController(UsuarioAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioAdminResponse> listar() { return service.listar(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioAdminResponse criar(@Valid @RequestBody UsuarioCriarRequest r) { return service.criar(r); }

    @PutMapping("/{id}")
    public UsuarioAdminResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioAtualizarRequest r,
                                          Authentication auth) {
        return service.atualizar(id, r, auth.getName());
    }

    @PatchMapping("/{id}/status")
    public UsuarioAdminResponse status(@PathVariable Long id, @Valid @RequestBody UsuarioStatusRequest r,
                                       Authentication auth) {
        return service.alterarStatus(id, r.status(), auth.getName());
    }
}
