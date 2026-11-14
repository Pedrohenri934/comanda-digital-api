package com.comandadigital.api.controller;

import com.comandadigital.api.dto.DashboardDtos.DashboardResponse;
import com.comandadigital.api.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Indicadores do negocio. Somente ADMIN e GERENTE. */
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    /** ?dias=7 (padrao), de 1 a 90. */
    @GetMapping
    public DashboardResponse dashboard(@RequestParam(defaultValue = "7") int dias) {
        return service.montar(dias);
    }
}
