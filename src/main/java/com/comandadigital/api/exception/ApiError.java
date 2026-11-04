package com.comandadigital.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato padrao de erro (RNF06): nada de stack trace para o cliente. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(LocalDateTime timestamp, int status, String error, String message, Map<String, String> campos) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(LocalDateTime.now(), status, error, message, null);
    }
    public static ApiError of(int status, String error, String message, Map<String, String> campos) {
        return new ApiError(LocalDateTime.now(), status, error, message, campos);
    }
}
