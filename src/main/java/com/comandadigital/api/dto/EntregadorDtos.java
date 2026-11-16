package com.comandadigital.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Entregadores: cadastro, painel do entregador e gestao pelo admin. */
public final class EntregadorDtos {

    private EntregadorDtos() {}

    public record RegisterEntregadorRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
            @NotBlank(message = "Informe o email") @Email(message = "Email inválido") @Size(max = 160) String email,
            @NotBlank(message = "Informe a senha") @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres") String senha,
            @NotBlank(message = "Informe o telefone") @Size(max = 20) String telefone,
            @NotBlank(message = "Informe o veículo") @Pattern(regexp = "Moto|Bicicleta|Carro", message = "Veículo deve ser Moto, Bicicleta ou Carro") String veiculo,
            @Size(max = 10) String placa) {}

    /** Dados e numeros do painel do proprio entregador. statusCadastro: PENDENTE, APROVADO ou RECUSADO. */
    public record PerfilEntregador(String nome, String email, String veiculo, String placa, String statusCadastro,
                                   boolean online, long disponiveis, long emEntrega, long entreguesHoje,
                                   long entreguesTotal) {}

    public record Entrega(Long pedidoId, String cliente, String telefone, String endereco, BigDecimal valorTotal,
                          boolean pago, String observacoes, String itens, String status, LocalDateTime criadoEm) {}

    public record OnlineRequest(@NotNull Boolean online) {}

    public record EntregadorAdmin(Long id, String nome, String email, String telefone, String veiculo, String placa,
                                  String statusCadastro, boolean online, long entregasConcluidas, long emAndamento) {}

    public record StatusEntregadorRequest(
            @NotBlank @Pattern(regexp = "APROVADO|RECUSADO", message = "Status deve ser APROVADO ou RECUSADO") String status) {}
}
