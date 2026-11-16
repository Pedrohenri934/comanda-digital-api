package com.comandadigital.api.service;

import com.comandadigital.api.dto.EntregadorDtos.*;
import com.comandadigital.api.exception.EmailJaCadastradoException;
import com.comandadigital.api.exception.RecursoNaoEncontradoException;
import com.comandadigital.api.exception.RegraDeNegocioException;
import com.comandadigital.api.model.Perfil;
import com.comandadigital.api.model.StatusRegistro;
import com.comandadigital.api.model.Usuario;
import com.comandadigital.api.repository.EntregadorJdbcRepository;
import com.comandadigital.api.repository.EntregadorJdbcRepository.Dados;
import com.comandadigital.api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Entregadores: cadastro publico (fica PENDENTE ate o admin aprovar), painel de entregas e gestao.
 * Fluxo da entrega: pedido PRONTO > entregador aceita (SAIU_ENTREGA) > entregador confirma (FINALIZADO).
 */
@Service
public class EntregadorService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final EntregadorJdbcRepository repo;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public EntregadorService(EntregadorJdbcRepository repo, UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.repo = repo;
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    // ---------- cadastro ----------

    @Transactional
    public void cadastrar(RegisterEntregadorRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarios.existsByEmail(email)) {
            throw new EmailJaCadastradoException(email);
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setSenhaHash(encoder.encode(req.senha()));
        u.setPerfil(Perfil.ENTREGADOR);
        u.setTelefone(req.telefone().trim());
        u.setStatus(StatusRegistro.ATIVO);
        usuarios.save(u);
        String placa = req.placa() == null || req.placa().isBlank() ? null : req.placa().trim().toUpperCase();
        repo.inserir(u.getId(), req.veiculo(), placa);
    }

    // ---------- painel do entregador ----------

    @Transactional(readOnly = true)
    public PerfilEntregador perfil(String email) {
        Dados d = dadosDe(email);
        Timestamp inicioDoDia = Timestamp.valueOf(LocalDate.now(FUSO).atStartOfDay());
        return new PerfilEntregador(d.nome(), d.email(), d.veiculo(), d.placa(), d.statusCadastro(), d.disponivel(),
                repo.pedidosDisponiveis(), repo.emEntrega(d.usuarioId()), repo.entregues(d.usuarioId(), inicioDoDia),
                repo.entreguesTotal(d.usuarioId()));
    }

    @Transactional
    public PerfilEntregador definirOnline(String email, boolean online) {
        Dados d = aprovado(email);
        repo.definirDisponivel(d.usuarioId(), online);
        return perfil(email);
    }

    /** Pedidos prontos esperando entregador. Offline nao enxerga nada. */
    @Transactional(readOnly = true)
    public List<Entrega> disponiveis(String email) {
        Dados d = aprovado(email);
        return d.disponivel() ? repo.listarDisponiveis() : List.of();
    }

    @Transactional(readOnly = true)
    public List<Entrega> ativas(String email) {
        return repo.listarAtivas(aprovado(email).usuarioId());
    }

    @Transactional
    public void aceitar(String email, Long pedidoId) {
        Dados d = aprovado(email);
        if (!d.disponivel()) {
            throw new RegraDeNegocioException("Fique online para aceitar entregas");
        }
        if (repo.aceitar(pedidoId, d.usuarioId()) == 0) {
            throw new RegraDeNegocioException("Este pedido não está mais disponível (outro entregador já pegou ou mudou de status)");
        }
    }

    @Transactional
    public void entregar(String email, Long pedidoId) {
        Dados d = aprovado(email);
        if (repo.entregar(pedidoId, d.usuarioId()) == 0) {
            throw new RegraDeNegocioException("Não há uma entrega sua em andamento com este pedido");
        }
    }

    // ---------- admin ----------

    @Transactional(readOnly = true)
    public List<EntregadorAdmin> listarParaAdmin() {
        return repo.listarParaAdmin();
    }

    @Transactional
    public void alterarStatus(Long usuarioId, String status) {
        if (repo.atualizarStatus(usuarioId, status) == 0) {
            throw new RecursoNaoEncontradoException("Entregador não encontrado");
        }
    }

    // ---------- apoio ----------

    private Dados dadosDe(String email) {
        Usuario u = usuarios.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        return repo.buscar(u.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cadastro de entregador não encontrado"));
    }

    private Dados aprovado(String email) {
        Dados d = dadosDe(email);
        if (!"APROVADO".equals(d.statusCadastro())) {
            throw new RegraDeNegocioException("PENDENTE".equals(d.statusCadastro())
                    ? "Seu cadastro ainda está aguardando aprovação do administrador"
                    : "Seu cadastro de entregador foi recusado");
        }
        return d;
    }
}
