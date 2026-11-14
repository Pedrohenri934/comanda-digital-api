package com.comandadigital.api.controller;

import com.comandadigital.api.dto.AlterarStatusRequest;
import com.comandadigital.api.dto.PedidoGestaoResponse;
import com.comandadigital.api.model.StatusPedido;
import com.comandadigital.api.service.GestaoPedidoService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Pedidos vistos pela cozinha e pela gerencia. Cliente nao acessa (403). */
@RestController
@RequestMapping("/api/gestao/pedidos")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE','COZINHEIRO')")
public class GestaoPedidoController {

    private final GestaoPedidoService service;

    public GestaoPedidoController(GestaoPedidoService service) {
        this.service = service;
    }

    /** Lista pedidos. Filtro opcional: ?status=CONFIRMADO&status=EM_PREPARO. Mais antigos primeiro por padrao. */
    @GetMapping
    public Page<PedidoGestaoResponse> listar(
            @RequestParam(required = false) List<StatusPedido> status,
            @ParameterObject @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listar(status, pageable);
    }

    @GetMapping("/{id}")
    public PedidoGestaoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    /** Avanca ou cancela o pedido. Cancelar exige "motivo" e so gerente/admin. */
    @PatchMapping("/{id}/status")
    public PedidoGestaoResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody AlterarStatusRequest req,
                                              Authentication authentication) {
        return service.alterarStatus(id, req, authentication.getName());
    }
}
