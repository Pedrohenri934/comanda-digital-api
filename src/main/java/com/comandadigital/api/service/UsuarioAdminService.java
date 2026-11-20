package com.comandadigital.api.service;

import com.comandadigital.api.dto.UsuarioAdminDtos.*;
import com.comandadigital.api.exception.EmailJaCadastradoException;
import com.comandadigital.api.exception.RecursoNaoEncontradoException;
import com.comandadigital.api.exception.RegraDeNegocioException;
import com.comandadigital.api.model.Perfil;
import com.comandadigital.api.model.StatusRegistro;
import com.comandadigital.api.model.Usuario;
import com.comandadigital.api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** RF-041: o ADMIN cria, edita e inativa usuarios internos. Clientes e entregadores tem fluxo proprio. */
@Service
public class UsuarioAdminService {

    private static final Set<Perfil> INTERNOS = EnumSet.of(Perfil.ADMIN, Perfil.GERENTE, Perfil.COZINHEIRO);

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    public UsuarioAdminService(UsuarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdminResponse> listar() {
        return repo.findByPerfilInOrderByNome(INTERNOS).stream().map(UsuarioAdminResponse::from).toList();
    }

    @Transactional
    public UsuarioAdminResponse criar(UsuarioCriarRequest r) {
        exigirInterno(r.perfil());
        String email = r.email().trim().toLowerCase();
        if (repo.existsByEmail(email)) throw new EmailJaCadastradoException(email);
        Usuario u = new Usuario();
        u.setNome(r.nome().trim());
        u.setEmail(email);
        u.setSenhaHash(encoder.encode(r.senha()));
        u.setPerfil(r.perfil());
        u.setTelefone(vazioParaNulo(r.telefone()));
        u.setStatus(StatusRegistro.ATIVO);
        return UsuarioAdminResponse.from(repo.save(u));
    }

    @Transactional
    public UsuarioAdminResponse atualizar(Long id, UsuarioAtualizarRequest r, String emailLogado) {
        Usuario u = buscarInterno(id);
        exigirInterno(r.perfil());
        if (u.getPerfil() != r.perfil()) {
            if (u.getEmail().equalsIgnoreCase(emailLogado)) {
                throw new RegraDeNegocioException("Você não pode alterar o seu próprio perfil");
            }
            if (u.getPerfil() == Perfil.ADMIN) exigirOutroAdminAtivo(u);
        }
        String senha = r.senha() == null ? "" : r.senha().trim();
        if (!senha.isEmpty()) {
            if (senha.length() < 6) throw new RegraDeNegocioException("A senha deve ter de 6 a 72 caracteres");
            u.setSenhaHash(encoder.encode(senha));
        }
        u.setNome(r.nome().trim());
        u.setPerfil(r.perfil());
        u.setTelefone(vazioParaNulo(r.telefone()));
        return UsuarioAdminResponse.from(repo.save(u));
    }

    @Transactional
    public UsuarioAdminResponse alterarStatus(Long id, String status, String emailLogado) {
        Usuario u = buscarInterno(id);
        StatusRegistro novo = StatusRegistro.valueOf(status);
        if (novo == StatusRegistro.INATIVO && u.getStatus() == StatusRegistro.ATIVO) {
            if (u.getEmail().equalsIgnoreCase(emailLogado)) {
                throw new RegraDeNegocioException("Você não pode inativar a sua própria conta");
            }
            if (u.getPerfil() == Perfil.ADMIN) exigirOutroAdminAtivo(u);
        }
        u.setStatus(novo);
        return UsuarioAdminResponse.from(repo.save(u));
    }

    private Usuario buscarInterno(Long id) {
        Usuario u = repo.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        if (!INTERNOS.contains(u.getPerfil())) {
            throw new RegraDeNegocioException("Este usuário não é da equipe interna (clientes e entregadores têm tela própria)");
        }
        return u;
    }

    private void exigirInterno(Perfil p) {
        if (!INTERNOS.contains(p)) throw new RegraDeNegocioException("Perfil deve ser ADMIN, GERENTE ou COZINHEIRO");
    }

    /** Evita ficar sem nenhum administrador ativo. */
    private void exigirOutroAdminAtivo(Usuario alvo) {
        long ativos = repo.countByPerfilAndStatus(Perfil.ADMIN, StatusRegistro.ATIVO);
        boolean alvoContaComoAtivo = alvo.getStatus() == StatusRegistro.ATIVO;
        if (ativos - (alvoContaComoAtivo ? 1 : 0) < 1) {
            throw new RegraDeNegocioException("O sistema precisa de pelo menos um administrador ativo");
        }
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
