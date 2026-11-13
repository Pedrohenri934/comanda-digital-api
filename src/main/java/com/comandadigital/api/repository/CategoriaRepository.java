package com.comandadigital.api.repository;

import com.comandadigital.api.model.Categoria;
import com.comandadigital.api.model.StatusRegistro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByStatusOrderByOrdemAsc(StatusRegistro status);
}
