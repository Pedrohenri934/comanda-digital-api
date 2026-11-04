package com.comandadigital.api.controller;

import com.comandadigital.api.dto.AuthResponse;
import com.comandadigital.api.dto.LoginRequest;
import com.comandadigital.api.dto.RegisterRequest;
import com.comandadigital.api.dto.UsuarioResponse;
import com.comandadigital.api.service.AuthService;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    /** Util para testar o token: devolve quem esta logado. */
    @GetMapping("/me")
    public UsuarioResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
