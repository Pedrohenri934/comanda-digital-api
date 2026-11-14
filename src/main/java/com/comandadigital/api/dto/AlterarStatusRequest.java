package com.comandadigital.api.dto;

import com.comandadigital.api.model.StatusPedido;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlterarStatusRequest(@NotNull(message = "Informe o novo status") StatusPedido status,
                                   @Size(max = 255, message = "O motivo pode ter no máximo 255 caracteres") String motivo) {}
