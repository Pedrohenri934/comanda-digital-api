package com.comandadigital.api.controller;

import com.comandadigital.api.dto.EntregadorDtos.Entrega;
import com.comandadigital.api.dto.EntregadorDtos.OnlineRequest;
import com.comandadigital.api.dto.EntregadorDtos.PerfilEntregador;
import com.comandadigital.api.service.EntregadorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Painel do entregador (somente perfil ENTREGADOR). */
@RestController
@RequestMapping("/api/entregador")
@PreAuthorize("hasRole('ENTREGADOR')")
public class EntregadorController {

    private final EntregadorService service;

    public EntregadorController(EntregadorService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public PerfilEntregador me(Authentication auth) {
        return service.perfil(auth.getName());
    }

    @PatchMapping("/online")
    public PerfilEntregador online(@Valid @RequestBody OnlineRequest req, Authentication auth) {
        return service.definirOnline(auth.getName(), req.online());
    }

    @GetMapping("/pedidos/disponiveis")
    public List<Entrega> disponiveis(Authentication auth) {
        return service.disponiveis(auth.getName());
    }

    @GetMapping("/pedidos/ativos")
    public List<Entrega> ativos(Authentication auth) {
        return service.ativas(auth.getName());
    }

    @PostMapping("/pedidos/{id}/aceitar")
    public ResponseEntity<Void> aceitar(@PathVariable Long id, Authentication auth) {
        service.aceitar(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pedidos/{id}/entregar")
    public ResponseEntity<Void> entregar(@PathVariable Long id, Authentication auth) {
        service.entregar(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
