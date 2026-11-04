package com.comandadigital.api.dto;

import com.comandadigital.api.model.Perfil;
import com.comandadigital.api.model.Usuario;

/** Resposta publica do usuario. Nunca inclui a senha (RNF01). */
public record UsuarioResponse(Long id, String nome, String email, Perfil perfil) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil());
    }
}
