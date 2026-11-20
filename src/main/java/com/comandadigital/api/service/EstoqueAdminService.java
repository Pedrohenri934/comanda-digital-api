package com.comandadigital.api.service;

import com.comandadigital.api.dto.AdminDtos.IngredienteResponse;
import com.comandadigital.api.dto.EstoqueDtos.*;
import com.comandadigital.api.exception.RecursoNaoEncontradoException;
import com.comandadigital.api.exception.RegraDeNegocioException;
import com.comandadigital.api.model.StatusRegistro;
import com.comandadigital.api.repository.EstoqueAdminJdbcRepository;
import com.comandadigital.api.repository.EstoqueAdminJdbcRepository.IngredienteBasico;
import com.comandadigital.api.repository.EstoqueConsultaRepository;
import com.comandadigital.api.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Entradas manuais (lote, validade, custo), baixas por perda, historico e alertas de estoque minimo. */
@Service
public class EstoqueAdminService {

    private final EstoqueAdminJdbcRepository repo;
    private final EstoqueConsultaRepository consulta;
    private final CadastroAdminService cadastro;
    private final UsuarioRepository usuarios;

    public EstoqueAdminService(EstoqueAdminJdbcRepository repo, EstoqueConsultaRepository consulta,
                               CadastroAdminService cadastro, UsuarioRepository usuarios) {
        this.repo = repo;
        this.consulta = consulta;
        this.cadastro = cadastro;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<IngredienteResponse> saldo() {
        return cadastro.listarIngredientes();
    }

    /** Ingredientes ativos com saldo abaixo do minimo. */
    @Transactional(readOnly = true)
    public List<IngredienteResponse> alertas() {
        return cadastro.listarIngredientes().stream()
                .filter(i -> i.abaixoDoMinimo() && StatusRegistro.ATIVO.name().equals(i.status())).toList();
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> movimentacoes(Long ingredienteId, String tipo, Pageable pageable) {
        return repo.listar(ingredienteId, tipo == null || tipo.isBlank() ? null : tipo.trim().toUpperCase(), pageable);
    }

    @Transactional
    public void entrada(EntradaRequest r, String emailLogado) {
        IngredienteBasico ing = ingredienteAtivo(r.ingredienteId());
        if (r.validade() != null && r.validade().isBefore(LocalDate.now())) {
            throw new RegraDeNegocioException("A validade não pode estar no passado");
        }
        repo.inserir(ing.id(), "ENTRADA", r.quantidade(), "AJUSTE", vazioParaNulo(r.lote()), r.validade(),
                r.custoUnitario(), vazioParaNulo(r.observacao()), usuarioId(emailLogado));
    }

    @Transactional
    public void saida(SaidaRequest r, String emailLogado) {
        IngredienteBasico ing = ingredienteAtivo(r.ingredienteId());
        BigDecimal saldo = consulta.saldos(List.of(ing.id())).getOrDefault(ing.id(), BigDecimal.ZERO);
        if (saldo.compareTo(r.quantidade()) < 0) {
            throw new RegraDeNegocioException("Saldo insuficiente de " + ing.nome() + ": há " + saldo.stripTrailingZeros().toPlainString()
                    + " " + ing.unidade() + " e a baixa pede " + r.quantidade().stripTrailingZeros().toPlainString());
        }
        repo.inserir(ing.id(), "SAIDA", r.quantidade(), r.motivo(), null, null, null,
                vazioParaNulo(r.observacao()), usuarioId(emailLogado));
    }

    private IngredienteBasico ingredienteAtivo(Long id) {
        IngredienteBasico ing = repo.ingrediente(id).orElseThrow(() -> new RecursoNaoEncontradoException("Ingrediente não encontrado"));
        if (!StatusRegistro.ATIVO.name().equals(ing.status())) {
            throw new RegraDeNegocioException("O ingrediente " + ing.nome() + " está inativo");
        }
        return ing;
    }

    private Long usuarioId(String email) {
        return usuarios.findByEmail(email).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado")).getId();
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
