package com.comandadigital.api.controller;

import com.comandadigital.api.dto.CriarPedidoRequest;
import com.comandadigital.api.dto.PedidoResponse;
import com.comandadigital.api.dto.PedidoStatusResponse;
import com.comandadigital.api.service.PedidoService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Pedidos do cliente logado (RF-006, RF-007 e RF-008). */
@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasRole('CLIENTE')")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse criar(@Valid @RequestBody CriarPedidoRequest req, Authentication authentication) {
        return pedidoService.criar(authentication.getName(), req);
    }

    @GetMapping("/meus")
    public Page<PedidoResponse> meus(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        return pedidoService.meusPedidos(authentication.getName(), pageable);
    }

    @GetMapping("/{id}/status")
    public PedidoStatusResponse status(@PathVariable Long id, Authentication authentication) {
        return pedidoService.status(authentication.getName(), id);
    }
}
