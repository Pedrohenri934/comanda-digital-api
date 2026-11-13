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

/** Cardapio publico: so pratos ATIVOS de categorias ativas (RN09). */
@Service
public class CardapioService {

    private final PratoRepository pratoRepository;
    private final CategoriaRepository categoriaRepository;

    public CardapioService(PratoRepository pratoRepository, CategoriaRepository categoriaRepository) {
        this.pratoRepository = pratoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public Page<PratoResponse> listar(Long categoriaId, Pageable pageable) {
        Page<com.comandadigital.api.model.Prato> pagina = (categoriaId == null)
                ? pratoRepository.findByStatusAndCategoriaStatus(StatusPrato.ATIVO, StatusRegistro.ATIVO, pageable)
                : pratoRepository.findByStatusAndCategoriaStatusAndCategoriaId(
                        StatusPrato.ATIVO, StatusRegistro.ATIVO, categoriaId, pageable);
        return pagina.map(PratoResponse::from);
    }

    @Transactional(readOnly = true)
    public PratoResponse detalhe(Long id) {
        return pratoRepository.findByIdAndStatus(id, StatusPrato.ATIVO)
                .map(PratoResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Prato não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> categorias() {
        return categoriaRepository.findByStatusOrderByOrdemAsc(StatusRegistro.ATIVO).stream()
                .map(CategoriaResponse::from)
                .toList();
    }
}
