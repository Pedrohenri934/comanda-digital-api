package com.comandadigital.api.service;

import com.comandadigital.api.dto.CategoriaResponse;
import com.comandadigital.api.dto.PratoResponse;
import com.comandadigital.api.exception.RecursoNaoEncontradoException;
import com.comandadigital.api.model.StatusPrato;
import com.comandadigital.api.model.StatusRegistro;
import com.comandadigital.api.repository.CategoriaRepository;
import com.comandadigital.api.repository.PratoRepository;
import org.springframework.data.domain.Page;
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

    public CardapioService(PratoRepository pratoRepository, CategoriaRepository categoriaRepository, ComboService comboService) {
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
        Set<Long> comOpcoes = comboService.pratosComOpcoes(pagina.getContent().stream().map(com.comandadigital.api.model.Prato::getId).toList());
        return pagina.map(p -> PratoResponse.from(p, comOpcoes.contains(p.getId())));
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
