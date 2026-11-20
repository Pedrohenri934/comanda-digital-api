package com.comandadigital.api.dto;

import com.comandadigital.api.model.Perfil;
import com.comandadigital.api.model.Usuario;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/** Cadastro de usuarios internos (ADMIN, GERENTE e COZINHEIRO) pelo administrador (RF-041). */
public final class UsuarioAdminDtos {

    private UsuarioAdminDtos() {}

    public record UsuarioAdminResponse(Long id, String nome, String email, String perfil, String telefone, String status,
                                       LocalDateTime criadoEm) {
        public static UsuarioAdminResponse from(Usuario u) {
            return new UsuarioAdminResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil().name(), u.getTelefone(),
                    u.getStatus().name(), u.getCreatedAt());
        }
    }

    public record UsuarioCriarRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 120, message = "O nome pode ter até 120 caracteres") String nome,
            @NotBlank(message = "Informe o email") @Email(message = "Email inválido") @Size(max = 160, message = "O email pode ter até 160 caracteres") String email,
            @NotBlank(message = "Informe a senha") @Size(min = 6, max = 72, message = "A senha deve ter de 6 a 72 caracteres") String senha,
            @NotNull(message = "Escolha o perfil") Perfil perfil,
            @Size(max = 20, message = "O telefone pode ter até 20 caracteres") String telefone) {}

    /** A senha e opcional: em branco mantem a atual. */
    public record UsuarioAtualizarRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 120, message = "O nome pode ter até 120 caracteres") String nome,
            @NotNull(message = "Escolha o perfil") Perfil perfil,
            @Size(max = 20, message = "O telefone pode ter até 20 caracteres") String telefone,
            @Size(max = 72, message = "A senha pode ter até 72 caracteres") String senha) {}

    public record UsuarioStatusRequest(@NotNull(message = "Informe o status") @Pattern(regexp = "ATIVO|INATIVO", message = "Status deve ser ATIVO ou INATIVO") String status) {}
}
