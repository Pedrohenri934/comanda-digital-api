package com.comandadigital.api.service;

import com.comandadigital.api.dto.AdminDtos.*;
import com.comandadigital.api.exception.RecursoNaoEncontradoException;
import com.comandadigital.api.exception.RegraDeNegocioException;
import com.comandadigital.api.model.Categoria;
import com.comandadigital.api.model.Prato;
import com.comandadigital.api.model.StatusPrato;
import com.comandadigital.api.model.StatusRegistro;
import com.comandadigital.api.repository.CadastroJdbcRepository;
import com.comandadigital.api.repository.CadastroJdbcRepository.FichaCabecalho;
import com.comandadigital.api.repository.CategoriaRepository;
import com.comandadigital.api.repository.PratoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cadastros do admin/gerente: categorias, pratos, ingredientes e ficha tecnica (RF-009 a RF-015).
 * Nada e apagado de verdade: "excluir" marca INATIVO (RN06).
 */
@Service
public class CadastroAdminService {

    private final CategoriaRepository categoriaRepository;
    private final PratoRepository pratoRepository;
    private final CadastroJdbcRepository jdbc;

    public CadastroAdminService(CategoriaRepository categoriaRepository, PratoRepository pratoRepository,
                                CadastroJdbcRepository jdbc) {
        this.categoriaRepository = categoriaRepository;
        this.pratoRepository = pratoRepository;
        this.jdbc = jdbc;
    }

    // ================================================================ categorias

    private static CategoriaAdminResponse respostaCategoria(Categoria c) {
        return new CategoriaAdminResponse(c.getId(), c.getNome(), c.getDescricao(), c.getOrdem(), c.getStatus().name());
    }

    private Categoria categoria(Long id) {
        return categoriaRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
    }

    @Transactional(readOnly = true)
    public List<CategoriaAdminResponse> listarCategorias() {
        return categoriaRepository.findAllByOrderByOrdemAscNomeAsc().stream().map(CadastroAdminService::respostaCategoria).toList();
    }

    @Transactional
    public CategoriaAdminResponse criarCategoria(CategoriaRequest r) {
        Categoria c = new Categoria();
        aplicar(c, r);
        return respostaCategoria(categoriaRepository.save(c));
    }

    @Transactional
    public CategoriaAdminResponse atualizarCategoria(Long id, CategoriaRequest r) {
        Categoria c = categoria(id);
        aplicar(c, r);
        return respostaCategoria(categoriaRepository.save(c));
    }

    private static void aplicar(Categoria c, CategoriaRequest r) {
        c.setNome(r.nome().trim());
        c.setDescricao(r.descricao() == null || r.descricao().isBlank() ? null : r.descricao().trim());
        c.setOrdem(r.ordem() == null ? 0 : r.ordem());
    }

    @Transactional
    public CategoriaAdminResponse alterarStatusCategoria(Long id, StatusRegistro status) {
        Categoria c = categoria(id);
        if (status == StatusRegistro.INATIVO && pratoRepository.existsByCategoriaIdAndStatus(id, StatusPrato.ATIVO)) {
            throw new RegraDeNegocioException("Esta categoria ainda tem pratos ativos. Inative os pratos antes.");
        }
        c.setStatus(status);
        return respostaCategoria(categoriaRepository.save(c));
    }

    // ================================================================ pratos

    private PratoAdminResponse respostaPrato(Prato p, Map<Long, BigDecimal> custos) {
        BigDecimal custo = custos.get(p.getId());
        BigDecimal foodCost = (custo == null || p.getPrecoVenda().signum() == 0) ? null
                : custo.multiply(BigDecimal.valueOf(100)).divide(p.getPrecoVenda(), 2, RoundingMode.HALF_UP);
        return new PratoAdminResponse(p.getId(), p.getNome(), p.getDescricao(), p.getFotoUrl(), p.getPrecoVenda(),
                p.getTempoPreparoMin(), p.getCategoria().getId(), p.getCategoria().getNome(), p.getStatus(),
                custo == null ? null : custo.setScale(2, RoundingMode.HALF_UP), foodCost, custo != null);
    }

    private PratoAdminResponse respostaPrato(Prato p) {
        return respostaPrato(p, jdbc.custosPorPrato(List.of(p.getId())));
    }

    private Prato prato(Long id) {
        return pratoRepository.findWithCategoriaById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Prato não encontrado"));
    }

    @Transactional(readOnly = true)
    public Page<PratoAdminResponse> listarPratos(StatusPrato status, Pageable pageable) {
        Page<Prato> pagina = status == null ? pratoRepository.findAllBy(pageable) : pratoRepository.findByStatus(status, pageable);
        Map<Long, BigDecimal> custos = jdbc.custosPorPrato(pagina.getContent().stream().map(Prato::getId).toList());
        return pagina.map(p -> respostaPrato(p, custos));
    }

    @Transactional(readOnly = true)
    public PratoAdminResponse buscarPrato(Long id) {
        return respostaPrato(prato(id));
    }

    @Transactional
    public PratoAdminResponse criarPrato(PratoRequest r) {
        Prato p = new Prato();
        aplicar(p, r);
        p.setStatus(StatusPrato.INATIVO); // so entra no cardapio depois da ficha tecnica e da ativacao
        return respostaPrato(pratoRepository.save(p));
    }

    @Transactional
    public PratoAdminResponse atualizarPrato(Long id, PratoRequest r) {
        Prato p = prato(id);
        aplicar(p, r);
        return respostaPrato(pratoRepository.save(p));
    }

    private void aplicar(Prato p, PratoRequest r) {
        p.setCategoria(categoria(r.categoriaId()));
        p.setNome(r.nome().trim());
        p.setDescricao(r.descricao() == null || r.descricao().isBlank() ? null : r.descricao().trim());
        p.setFotoUrl(r.fotoUrl() == null || r.fotoUrl().isBlank() ? null : r.fotoUrl().trim());
        p.setPrecoVenda(r.precoVenda());
        p.setTempoPreparoMin(r.tempoPreparoMin());
    }

    /** ATIVO so e permitido com ficha tecnica e categoria ativa (RN09). */
    @Transactional
    public PratoAdminResponse alterarStatusPrato(Long id, StatusPrato status) {
        Prato p = prato(id);
        if (status == StatusPrato.ATIVO) {
            if (p.getCategoria().getStatus() != StatusRegistro.ATIVO) {
                throw new RegraDeNegocioException("A categoria deste prato está inativa");
            }
            if (!jdbc.custosPorPrato(List.of(id)).containsKey(id)) {
                throw new RegraDeNegocioException("Cadastre a ficha técnica do prato antes de ativá-lo");
            }
        }
        p.setStatus(status);
        return respostaPrato(pratoRepository.save(p));
    }

    // ================================================================ ingredientes

    @Transactional(readOnly = true)
    public List<IngredienteResponse> listarIngredientes() {
        return jdbc.listarIngredientes();
    }

    @Transactional
    public IngredienteResponse criarIngrediente(IngredienteRequest r) {
        Long id = jdbc.inserirIngrediente(r);
        return jdbc.buscarIngrediente(id).orElseThrow();
    }

    @Transactional
    public IngredienteResponse atualizarIngrediente(Long id, IngredienteRequest r) {
        ingrediente(id);
        jdbc.atualizarIngrediente(id, r);
        return jdbc.buscarIngrediente(id).orElseThrow();
    }

    @Transactional
    public IngredienteResponse alterarStatusIngrediente(Long id, StatusRegistro status) {
        ingrediente(id);
        if (status == StatusRegistro.INATIVO && jdbc.pratosAtivosUsando(id) > 0) {
            throw new RegraDeNegocioException("Este ingrediente é usado na ficha de pratos ativos. Ajuste as fichas antes.");
        }
        jdbc.alterarStatusIngrediente(id, status.name());
        return jdbc.buscarIngrediente(id).orElseThrow();
    }

    private IngredienteResponse ingrediente(Long id) {
        return jdbc.buscarIngrediente(id).orElseThrow(() -> new RecursoNaoEncontradoException("Ingrediente não encontrado"));
    }

    // ================================================================ ficha tecnica

    @Transactional(readOnly = true)
    public FichaResponse buscarFicha(Long pratoId) {
        Prato p = prato(pratoId);
        FichaCabecalho f = jdbc.buscarFicha(pratoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Este prato ainda não tem ficha técnica"));
        return montarFicha(p, f);
    }

    @Transactional
    public FichaResponse salvarFicha(Long pratoId, FichaRequest r) {
        Prato p = prato(pratoId);

        Set<Long> vistos = new HashSet<>();
        for (FichaItemRequest it : r.itens()) {
            if (!vistos.add(it.ingredienteId())) {
                throw new RegraDeNegocioException("Há ingrediente repetido na ficha. Junte as quantidades em uma linha só.");
            }
        }
        Map<Long, String[]> dados = jdbc.unidadesEStatus(vistos);
        for (FichaItemRequest it : r.itens()) {
            String[] d = dados.get(it.ingredienteId());
            if (d == null) throw new RegraDeNegocioException("Ingrediente não encontrado: " + it.ingredienteId());
            if (!"ATIVO".equals(d[1])) throw new RegraDeNegocioException("Há ingrediente inativo na ficha");
            if (!compativeis(it.unidade(), d[0])) {
                throw new RegraDeNegocioException("A unidade " + it.unidade() + " não combina com a unidade padrão do ingrediente (" + d[0] + ")");
            }
        }

        String modo = r.modoPreparo() == null || r.modoPreparo().isBlank() ? null : r.modoPreparo().trim();
        jdbc.salvarFicha(pratoId, r.rendimento(), modo, r.itens());
        return montarFicha(p, jdbc.buscarFicha(pratoId).orElseThrow());
    }

    private FichaResponse montarFicha(Prato p, FichaCabecalho f) {
        List<FichaItemResponse> itens = jdbc.itensDaFicha(f.id());
        BigDecimal custo = jdbc.custosPorPrato(List.of(p.getId())).get(p.getId());
        BigDecimal foodCost = (custo == null || p.getPrecoVenda().signum() == 0) ? null
                : custo.multiply(BigDecimal.valueOf(100)).divide(p.getPrecoVenda(), 2, RoundingMode.HALF_UP);
        return new FichaResponse(p.getId(), f.rendimento(), f.modoPreparo(), itens,
                custo == null ? null : custo.setScale(2, RoundingMode.HALF_UP), p.getPrecoVenda(), foodCost);
    }

    private static boolean compativeis(String a, String b) {
        if (a.equals(b)) return true;
        return (a.equals("G") && b.equals("KG")) || (a.equals("KG") && b.equals("G"))
                || (a.equals("ML") && b.equals("L")) || (a.equals("L") && b.equals("ML"));
    }
}
