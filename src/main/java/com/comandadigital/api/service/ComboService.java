package com.comandadigital.api.service;

import com.comandadigital.api.dto.ComboDtos.*;
import com.comandadigital.api.exception.RecursoNaoEncontradoException;
import com.comandadigital.api.exception.RegraDeNegocioException;
import com.comandadigital.api.model.PedidoItemOpcao;
import com.comandadigital.api.repository.ComboJdbcRepository;
import com.comandadigital.api.repository.ComboJdbcRepository.GrupoRow;
import com.comandadigital.api.repository.ComboJdbcRepository.OpcaoRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** Combos com escolhas: consulta publica, edicao pelo painel e conferencia das escolhas do pedido. */
@Service
public class ComboService {

    /** Resultado da conferencia: as escolhas prontas para gravar e o acrescimo (por unidade) que elas somam. */
    public record Escolha(List<PedidoItemOpcao> opcoes, BigDecimal acrescimo) {}

    private final ComboJdbcRepository repo;

    public ComboService(ComboJdbcRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<GrupoResponse> listar(Long pratoId) {
        List<GrupoRow> grupos = repo.grupos(List.of(pratoId));
        Map<Long, List<OpcaoRow>> porGrupo = repo.opcoes(grupos.stream().map(GrupoRow::id).toList()).stream()
                .collect(Collectors.groupingBy(OpcaoRow::grupoId, LinkedHashMap::new, Collectors.toList()));
        return grupos.stream().map(g -> new GrupoResponse(g.id(), g.nome(), g.minimo(), g.maximo(),
                porGrupo.getOrDefault(g.id(), List.of()).stream()
                        .map(o -> new OpcaoResponse(o.id(), o.nome(), o.pratoId(), o.acrescimo(), o.disponivel())).toList())).toList();
    }

    public Set<Long> pratosComOpcoes(Collection<Long> pratoIds) {
        return repo.pratosComOpcoes(pratoIds);
    }

    @Transactional
    public List<GrupoResponse> salvar(Long pratoId, OpcoesRequest req) {
        if (!repo.pratoExiste(pratoId)) throw new RecursoNaoEncontradoException("Prato não encontrado");
        for (GrupoRequest g : req.grupos()) {
            if (g.minimo() > g.maximo()) {
                throw new RegraDeNegocioException("No grupo '" + g.nome() + "' o mínimo não pode ser maior que o máximo");
            }
            for (OpcaoRequest o : g.opcoes()) {
                if (o.pratoId() != null && !repo.pratoExiste(o.pratoId())) {
                    throw new RegraDeNegocioException("A opção '" + o.nome() + "' aponta para um prato que não existe");
                }
            }
        }
        repo.substituir(pratoId, req.grupos());
        return listar(pratoId);
    }

    /** Confere as escolhas do cliente para um item: opcoes validas, disponiveis e dentro do minimo/maximo de cada grupo. */
    @Transactional(readOnly = true)
    public Escolha conferir(Long pratoId, String pratoNome, List<Long> opcaoIds) {
        List<Long> ids = opcaoIds == null ? List.of() : opcaoIds;
        List<GrupoRow> grupos = repo.grupos(List.of(pratoId));
        if (grupos.isEmpty()) {
            if (!ids.isEmpty()) throw new RegraDeNegocioException("O prato '" + pratoNome + "' não tem opções para escolher");
            return new Escolha(List.of(), BigDecimal.ZERO);
        }
        Map<Long, GrupoRow> gruposPorId = grupos.stream().collect(Collectors.toMap(GrupoRow::id, g -> g));
        Map<Long, OpcaoRow> opcoesPorId = repo.opcoes(gruposPorId.keySet()).stream()
                .collect(Collectors.toMap(OpcaoRow::id, o -> o));

        Map<Long, Integer> contagem = new HashMap<>();
        List<PedidoItemOpcao> escolhidas = new ArrayList<>();
        BigDecimal acrescimo = BigDecimal.ZERO;
        for (Long id : ids) {
            OpcaoRow o = opcoesPorId.get(id);
            if (o == null) throw new RegraDeNegocioException("Opção inválida para o prato '" + pratoNome + "'");
            if (!o.disponivel()) throw new RegraDeNegocioException("A opção '" + o.nome() + "' não está disponível no momento");
            contagem.merge(o.grupoId(), 1, Integer::sum);
            escolhidas.add(new PedidoItemOpcao(gruposPorId.get(o.grupoId()).nome(), o.nome(), o.pratoId(), o.acrescimo()));
            acrescimo = acrescimo.add(o.acrescimo());
        }
        for (GrupoRow g : grupos) {
            int n = contagem.getOrDefault(g.id(), 0);
            if (n < g.minimo() || n > g.maximo()) {
                String regra = g.minimo() == g.maximo() ? "escolha " + g.minimo()
                        : "escolha de " + g.minimo() + " a " + g.maximo();
                throw new RegraDeNegocioException("Em '" + g.nome() + "' (" + pratoNome + "): " + regra + " opção(ões)");
            }
        }
        return new Escolha(escolhidas, acrescimo);
    }
}
