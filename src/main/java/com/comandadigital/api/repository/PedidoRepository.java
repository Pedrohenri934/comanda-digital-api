package com.comandadigital.api.repository;

import com.comandadigital.api.model.Pedido;
import com.comandadigital.api.model.StatusPedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Page<Pedido> findByClienteEmail(String email, Pageable pageable);
    Optional<Pedido> findByIdAndClienteEmail(Long id, String email);

    Page<Pedido> findByStatusIn(Collection<StatusPedido> status, Pageable pageable);

    /** Trava a linha do pedido durante a troca de status, para dois cliques nao darem baixa duas vezes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> buscarComTrava(@Param("id") Long id);
}
