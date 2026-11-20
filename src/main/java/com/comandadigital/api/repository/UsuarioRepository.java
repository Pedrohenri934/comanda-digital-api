package com.comandadigital.api.repository;

import com.comandadigital.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import com.comandadigital.api.model.Perfil;
import com.comandadigital.api.model.StatusRegistro;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Usuario> findByPerfilInOrderByNome(Collection<Perfil> perfis);
    long countByPerfilAndStatus(Perfil perfil, StatusRegistro status);
}
