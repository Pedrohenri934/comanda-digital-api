package com.comandadigital.api.repository;

import com.comandadigital.api.model.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Page<Pedido> findByClienteEmail(String email, Pageable pageable);
    Optional<Pedido> findByIdAndClienteEmail(Long id, String email);
}
