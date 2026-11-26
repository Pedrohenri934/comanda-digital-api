package com.comandadigital.api.service;

import com.comandadigital.api.dto.CategoriaResponse;
import com.comandadigital.api.dto.PratoResponse;
import com.comandadigital.api.exception.RecursoNaoEncontradoException;
import com.comandadigital.api.model.StatusPrato;
import com.comandadigital.api.model.StatusRegistro;
import com.comandadigital.api.repository.CategoriaRepository;
import com.comandadigital.api.repository.PratoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** Cardapio publico: so pratos ATIVOS de categorias ativas (RN09). */
@Service
public class CardapioService {

    private final PratoRepository pratoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ComboService comboService;
    private final EstoqueService estoqueService;

    public CardapioService(PratoRepository pratoRepository, CategoriaRepository categoriaRepository, ComboService comboService,
                           EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
        this.pratoRepository = pratoRepository;
        this.categoriaRepository = categoriaRepository;
        this.comboService = comboService;
    }

    @Transactional(readOnly = true)
    public Page<PratoResponse> listar(Long categoriaId, Pageable pageable) {
        Page<com.comandadigital.api.model.Prato> pagina = (categoriaId == null)
                ? pratoRepository.findByStatusAndCategoriaStatus(StatusPrato.ATIVO, StatusRegistro.ATIVO, pageable)
                : pratoRepository.findByStatusAndCategoriaStatusAndCategoriaId(
                        StatusPrato.ATIVO, StatusRegistro.ATIVO, categoriaId, pageable);
        // Prato com ingrediente faltando no estoque nao aparece no cardapio.
        Set<Long> semEstoque = estoqueService.pratosSemEstoque(pagina.getContent().stream().map(com.comandadigital.api.model.Prato::getId).toList());
        List<com.comandadigital.api.model.Prato> disponiveis = pagina.getContent().stream().filter(p -> !semEstoque.contains(p.getId())).toList();
        Set<Long> comOpcoes = comboService.pratosComOpcoes(disponiveis.stream().map(com.comandadigital.api.model.Prato::getId).toList());
        List<PratoResponse> itens = disponiveis.stream().map(p -> PratoResponse.from(p, comOpcoes.contains(p.getId()))).toList();
        return new PageImpl<>(itens, pageable, pagina.getTotalElements() - semEstoque.size());
    }

    @Transactional(readOnly = true)
    public PratoResponse detalhe(Long id) {
        return pratoRepository.findByIdAndStatus(id, StatusPrato.ATIVO)
                .map(p -> PratoResponse.from(p, !comboService.listar(p.getId()).isEmpty()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Prato não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> categorias() {
        return categoriaRepository.findByStatusOrderByOrdemAsc(StatusRegistro.ATIVO).stream()
                .map(CategoriaResponse::from)
                .toList();
    }
}
