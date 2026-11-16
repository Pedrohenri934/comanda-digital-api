package com.comandadigital.api.controller;

import com.comandadigital.api.dto.RelatorioDtos.RelatorioBcgResponse;
import com.comandadigital.api.service.RelatorioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Vendas por prato e Matriz BCG. Somente ADMIN e GERENTE. */
@RestController
@RequestMapping("/api/admin/relatorios")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    /** ?dias=30 (padrao), de 1 a 365. */
    @GetMapping("/bcg")
    public RelatorioBcgResponse bcg(@RequestParam(defaultValue = "30") int dias) {
        return service.bcg(dias);
    }
}
