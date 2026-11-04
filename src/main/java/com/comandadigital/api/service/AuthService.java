package com.comandadigital.api.service;

import com.comandadigital.api.dto.AuthResponse;
import com.comandadigital.api.dto.LoginRequest;
import com.comandadigital.api.dto.RegisterRequest;
import com.comandadigital.api.dto.UsuarioResponse;
import com.comandadigital.api.exception.EmailJaCadastradoException;
import com.comandadigital.api.model.Perfil;
import com.comandadigital.api.model.StatusRegistro;
import com.comandadigital.api.model.Usuario;
import com.comandadigital.api.repository.UsuarioRepository;
import com.comandadigital.api.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras de negocio de autenticacao ficam aqui, nunca no Controller (RNF04). */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest req) {
        String email = normalizar(req.email());
        // Lanca BadCredentialsException (-> 401) se email/senha estiverem errados.
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.senha()));
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        return new AuthResponse(jwtService.gerarToken(usuario), UsuarioResponse.from(usuario));
    }

    /** Cadastro publico: sempre perfil CLIENTE (RF-039). Email unico (RN10 -> 409). */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalizar(req.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailJaCadastradoException(email);
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setSenhaHash(passwordEncoder.encode(req.senha()));
        u.setPerfil(Perfil.CLIENTE);
        u.setTelefone(req.telefone().trim());
        u.setEndereco(req.endereco().trim());
        u.setStatus(StatusRegistro.ATIVO);
        usuarioRepository.save(u);
        return new AuthResponse(jwtService.gerarToken(u), UsuarioResponse.from(u));
    }

    public UsuarioResponse me(String email) {
        return usuarioRepository.findByEmail(email)
                .map(UsuarioResponse::from)
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));
    }

    private String normalizar(String email) {
        return email.trim().toLowerCase();
    }
}
