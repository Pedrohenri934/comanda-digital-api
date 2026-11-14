package com.comandadigital.api.repository;

import com.comandadigital.api.model.Prato;
import com.comandadigital.api.model.StatusPrato;
import com.comandadigital.api.model.StatusRegistro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PratoRepository extends JpaRepository<Prato, Long> {

    @EntityGraph(attributePaths = "categoria")
    Page<Prato> findByStatusAndCategoriaStatus(StatusPrato status, StatusRegistro categoriaStatus, Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Page<Prato> findByStatusAndCategoriaStatusAndCategoriaId(StatusPrato status, StatusRegistro categoriaStatus,
                                                             Long categoriaId, Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Optional<Prato> findByIdAndStatus(Long id, StatusPrato status);

    List<Prato> findByIdIn(Collection<Long> ids);

    // ---- admin: lista todos os pratos (qualquer status), opcionalmente por status
    @EntityGraph(attributePaths = "categoria")
    Page<Prato> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Page<Prato> findByStatus(StatusPrato status, Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Optional<Prato> findWithCategoriaById(Long id);

    boolean existsByCategoriaIdAndStatus(Long categoriaId, StatusPrato status);
}
