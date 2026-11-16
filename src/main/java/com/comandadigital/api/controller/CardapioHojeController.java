package com.comandadigital.api.controller;

import com.comandadigital.api.dto.CardapioSemanaDtos.ItemHoje;
import com.comandadigital.api.service.CardapioSemanaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Publico (GET /api/cardapio/** ja e liberado): pratos e especiais de hoje. */
@RestController
@RequestMapping("/api/cardapio/hoje")
public class CardapioHojeController {

    private final CardapioSemanaService service;

    public CardapioHojeController(CardapioSemanaService service) {
        this.service = service;
    }

    @GetMapping
    public List<ItemHoje> hoje() {
        return service.hoje();
    }
}
