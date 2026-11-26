package com.comandadigital.api.service;

import com.comandadigital.api.dto.AlterarStatusRequest;
import com.comandadigital.api.dto.PedidoGestaoResponse;
import com.comandadigital.api.exception.RecursoNaoEncontradoException;
import com.comandadigital.api.exception.RegraDeNegocioException;
import com.comandadigital.api.model.Pedido;
import com.comandadigital.api.model.PedidoItem;
import com.comandadigital.api.model.Perfil;
import com.comandadigital.api.model.StatusPedido;
import com.comandadigital.api.model.Usuario;
import com.comandadigital.api.repository.PedidoRepository;
import com.comandadigital.api.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.comandadigital.api.model.StatusPedido.*;

/**
 * Gestao dos pedidos pela cozinha e gerencia (RF-016 a RF-020).
 * Fluxo: RECEBIDO > CONFIRMADO > EM_PREPARO > PRONTO > SAIU_ENTREGA > FINALIZADO (ou CANCELADO).
 * O estoque so baixa na confirmacao (RN03) e volta (estorno) se um pedido ja confirmado for cancelado.
 */
@Service
public class GestaoPedidoService {

    private static final Map<StatusPedido, Set<StatusPedido>> TRANSICOES = Map.of(
            RECEBIDO,     EnumSet.of(CONFIRMADO, CANCELADO),
            CONFIRMADO,   EnumSet.of(EM_PREPARO, CANCELADO),
            EM_PREPARO,   EnumSet.of(PRONTO, CANCELADO),
            // PRONTO > SAIU_ENTREGA so acontece quando um entregador aceita (painel do entregador), para o pedido ter dono.
            PRONTO,       EnumSet.of(CANCELADO),
            SAIU_ENTREGA, EnumSet.of(FINALIZADO),
            FINALIZADO,   EnumSet.noneOf(StatusPedido.class),
            CANCELADO,    EnumSet.noneOf(StatusPedido.class));

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstoqueService estoqueService;

    public GestaoPedidoService(PedidoRepository pedidoRepository, UsuarioRepository usuarioRepository,
                               EstoqueService estoqueService) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.estoqueService = estoqueService;
    }

    @Transactional(readOnly = true)
    public Page<PedidoGestaoResponse> listar(List<StatusPedido> status, LocalDate de, LocalDate ate, String canal,
                                             Pageable pageable) {
        if (de != null && ate != null && ate.isBefore(de)) {
            throw new RegraDeNegocioException("A data final não pode ser anterior à data inicial");
        }
        Specification<Pedido> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (status != null && !status.isEmpty()) ps.add(root.get("status").in(status));
            if (de != null) ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), de.atStartOfDay()));
            if (ate != null) ps.add(cb.lessThan(root.get("createdAt"), ate.plusDays(1).atStartOfDay()));
            if (canal != null && !canal.isBlank()) ps.add(cb.equal(root.get("canal"), canal.trim().toUpperCase()));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return pedidoRepository.findAll(spec, pageable).map(PedidoGestaoResponse::from);
    }

    @Transactional(readOnly = true)
    public PedidoGestaoResponse buscar(Long id) {
        return pedidoRepository.findById(id).map(PedidoGestaoResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
    }

    @Transactional
    public PedidoGestaoResponse alterarStatus(Long id, AlterarStatusRequest req, String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        Pedido pedido = pedidoRepository.buscarComTrava(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));

        StatusPedido atual = pedido.getStatus();
        StatusPedido novo = req.status();

        if (!TRANSICOES.get(atual).contains(novo)) {
            throw new RegraDeNegocioException("Não é possível mudar o pedido de " + atual + " para " + novo);
        }

        if (novo == CANCELADO) {
            // RN04: cozinheiro so cancela antes do preparo (RECEBIDO ou CONFIRMADO); depois, so gerente/admin.
            if (usuario.getPerfil() == Perfil.COZINHEIRO && atual != RECEBIDO && atual != CONFIRMADO) {
                throw new RegraDeNegocioException("Após o início do preparo, somente gerente ou administrador pode cancelar o pedido");
            }
            if (req.motivo() == null || req.motivo().isBlank()) {
                throw new RegraDeNegocioException("Informe o motivo do cancelamento");
            }
            pedido.setMotivoCancelamento(req.motivo().trim());
            // RECEBIDO nunca deu baixa; qualquer outro status ja deu e precisa estornar.
            if (atual != RECEBIDO) estoqueService.estornar(pedido.getId(), usuario.getId());
        }

        if (novo == CONFIRMADO) {
            // Confere de novo (o estoque pode ter mudado desde a criacao) e da baixa. Falta de insumo -> 422.
            estoqueService.darBaixa(pedido.getId(), usuario.getId(), pedido.consumoPorPrato());
        }

        pedido.setStatus(novo);
        pedidoRepository.save(pedido);
        return PedidoGestaoResponse.from(pedido);
    }
}
