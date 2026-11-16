package com.comandadigital.api.controller;

import com.comandadigital.api.dto.AuthResponse;
import com.comandadigital.api.dto.LoginRequest;
import com.comandadigital.api.dto.RegisterRequest;
import com.comandadigital.api.dto.UsuarioResponse;
import com.comandadigital.api.dto.EntregadorDtos.RegisterEntregadorRequest;
import com.comandadigital.api.service.AuthService;
import com.comandadigital.api.service.EntregadorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** O Controller so recebe, chama o Service e devolve (RNF04). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EntregadorService entregadorService;

    public AuthController(AuthService authService, EntregadorService entregadorService) {
        this.authService = authService;
        this.entregadorService = entregadorService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    /** Cadastro de entregador: fica PENDENTE ate o administrador aprovar. Nao devolve token. */
    @PostMapping("/register-entregador")
    public ResponseEntity<Void> registerEntregador(@Valid @RequestBody RegisterEntregadorRequest req) {
        entregadorService.cadastrar(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Util para testar o token: devolve quem esta logado. */
    @GetMapping("/me")
    public UsuarioResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
