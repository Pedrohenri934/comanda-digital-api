package com.comandadigital.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
        @NotBlank(message = "Informe o email") @Email(message = "Email inválido") @Size(max = 160) String email,
        @NotBlank(message = "Informe a senha") @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres") String senha,
        @NotBlank(message = "Informe o telefone") @Size(max = 20) String telefone,
        @NotBlank(message = "Informe o endereço de entrega") @Size(max = 255) String endereco
) {}
