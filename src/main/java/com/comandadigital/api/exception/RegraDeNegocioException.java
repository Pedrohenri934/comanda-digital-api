package com.comandadigital.api.exception;

import java.util.Map;

/** Regra de negocio violada (responde 422). "detalhes" pode listar o que falta, por exemplo no estoque. */
public class RegraDeNegocioException extends RuntimeException {

    private final Map<String, String> detalhes;

    public RegraDeNegocioException(String mensagem) {
        this(mensagem, null);
    }

    public RegraDeNegocioException(String mensagem, Map<String, String> detalhes) {
        super(mensagem);
        this.detalhes = detalhes;
    }

    public Map<String, String> getDetalhes() {
        return detalhes;
    }
}
