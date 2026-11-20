package com.comandadigital.api.controller;

import com.comandadigital.api.dto.AdminDtos.*;
import com.comandadigital.api.model.StatusPrato;
import com.comandadigital.api.model.StatusRegistro;
import com.comandadigital.api.service.CadastroAdminService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Cadastros do painel administrativo. Somente ADMIN e GERENTE (cliente e cozinheiro recebem 403). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class CadastroAdminController {

    private final CadastroAdminService service;

    public CadastroAdminController(CadastroAdminService service) {
        this.service = service;
    }

    // ------------------------------------------------------------ categorias

    @GetMapping("/categorias")
    public List<CategoriaAdminResponse> listarCategorias() { return service.listarCategorias(); }

    @PostMapping("/categorias")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaAdminResponse criarCategoria(@Valid @RequestBody CategoriaRequest r) { return service.criarCategoria(r); }

    @PutMapping("/categorias/{id}")
    public CategoriaAdminResponse atualizarCategoria(@PathVariable Long id, @Valid @RequestBody CategoriaRequest r) {
        return service.atualizarCategoria(id, r);
    }

    /** Exclusao logica (RN06): marca INATIVO. */
    @DeleteMapping("/categorias/{id}")
    public CategoriaAdminResponse inativarCategoria(@PathVariable Long id) {
        return service.alterarStatusCategoria(id, StatusRegistro.INATIVO);
    }

    @PostMapping("/categorias/{id}/reativar")
    public CategoriaAdminResponse reativarCategoria(@PathVariable Long id) {
        return service.alterarStatusCategoria(id, StatusRegistro.ATIVO);
    }

    // ------------------------------------------------------------ pratos

    @GetMapping("/pratos")
    public Page<PratoAdminResponse> listarPratos(
            @RequestParam(required = false) StatusPrato status,
            @ParameterObject @PageableDefault(size = 50, sort = "nome") Pageable pageable) {
        return service.listarPratos(status, pageable);
    }

    @GetMapping("/pratos/{id}")
    public PratoAdminResponse buscarPrato(@PathVariable Long id) { return service.buscarPrato(id); }

    @PostMapping("/pratos")
    @ResponseStatus(HttpStatus.CREATED)
    public PratoAdminResponse criarPrato(@Valid @RequestBody PratoRequest r) { return service.criarPrato(r); }

    @PutMapping("/pratos/{id}")
    public PratoAdminResponse atualizarPrato(@PathVariable Long id, @Valid @RequestBody PratoRequest r) {
        return service.atualizarPrato(id, r);
    }

    /** Ativa, pausa ou inativa o prato. Ativar exige ficha tecnica (RN09). */
    @PatchMapping("/pratos/{id}/status")
    public PratoAdminResponse alterarStatusPrato(@PathVariable Long id, @Valid @RequestBody StatusPratoRequest r) {
        return service.alterarStatusPrato(id, r.status());
    }

    @DeleteMapping("/pratos/{id}")
    public PratoAdminResponse inativarPrato(@PathVariable Long id) {
        return service.alterarStatusPrato(id, StatusPrato.INATIVO);
    }

    // ------------------------------------------------------------ ficha tecnica

    @GetMapping({"/pratos/{id}/ficha", "/pratos/{id}/custo"})
    public FichaResponse buscarFicha(@PathVariable Long id) { return service.buscarFicha(id); }

    @PutMapping("/pratos/{id}/ficha")
    public FichaResponse salvarFicha(@PathVariable Long id, @Valid @RequestBody FichaRequest r) {
        return service.salvarFicha(id, r);
    }

    // ------------------------------------------------------------ ingredientes

    @GetMapping("/ingredientes")
    public List<IngredienteResponse> listarIngredientes() { return service.listarIngredientes(); }

    @PostMapping("/ingredientes")
    @ResponseStatus(HttpStatus.CREATED)
    public IngredienteResponse criarIngrediente(@Valid @RequestBody IngredienteRequest r) { return service.criarIngrediente(r); }

    @PutMapping("/ingredientes/{id}")
    public IngredienteResponse atualizarIngrediente(@PathVariable Long id, @Valid @RequestBody IngredienteRequest r) {
        return service.atualizarIngrediente(id, r);
    }

    @DeleteMapping("/ingredientes/{id}")
    public IngredienteResponse inativarIngrediente(@PathVariable Long id) {
        return service.alterarStatusIngrediente(id, StatusRegistro.INATIVO);
    }

    @PostMapping("/ingredientes/{id}/reativar")
    public IngredienteResponse reativarIngrediente(@PathVariable Long id) {
        return service.alterarStatusIngrediente(id, StatusRegistro.ATIVO);
    }
}
