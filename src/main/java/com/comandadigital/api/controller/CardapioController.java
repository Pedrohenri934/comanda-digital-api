package com.comandadigital.api.controller;

import com.comandadigital.api.dto.CategoriaResponse;
import com.comandadigital.api.dto.PratoResponse;
import com.comandadigital.api.dto.ComboDtos.GrupoResponse;
import com.comandadigital.api.service.CardapioService;
import com.comandadigital.api.service.ComboService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Cardapio publico (RF-001 e RF-002): nao exige login. */
@RestController
@RequestMapping("/api/cardapio")
public class CardapioController {

    private final CardapioService cardapioService;
    private final ComboService comboService;

    public CardapioController(CardapioService cardapioService, ComboService comboService) {
        this.cardapioService = cardapioService;
        this.comboService = comboService;
    }

    @GetMapping
    public Page<PratoResponse> listar(@RequestParam(required = false) Long categoriaId,
                                      @ParameterObject @PageableDefault(size = 50, sort = "nome") Pageable pageable) {
        return cardapioService.listar(categoriaId, pageable);
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> categorias() {
        return cardapioService.categorias();
    }

    /** Escolhas do combo (grupos e opcoes). Lista vazia = prato sem escolhas. */
    @GetMapping("/{id}/opcoes")
    public List<GrupoResponse> opcoes(@PathVariable Long id) {
        return comboService.listar(id);
    }

    @GetMapping("/{id}")
    public PratoResponse detalhe(@PathVariable Long id) {
        return cardapioService.detalhe(id);
    }
}
