package com.comandadigital.api.service;

import com.comandadigital.api.dto.CompraDtos.*;
import com.comandadigital.api.exception.RecursoNaoEncontradoException;
import com.comandadigital.api.exception.RegraDeNegocioException;
import com.comandadigital.api.repository.CompraJdbcRepository;
import com.comandadigital.api.repository.CompraJdbcRepository.CompraCabecalho;
import com.comandadigital.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fornecedores e pedidos de compra (RF-021 a RF-024).
 * Fluxo da compra: RASCUNHO > ENVIADO > RECEBIDO (ou CANCELADO). Receber da entrada no estoque.
 */
@Service
public class CompraAdminService {

    private static final Map<String, Set<String>> TRANSICOES = Map.of(
            "RASCUNHO", Set.of("ENVIADO", "CANCELADO"),
            "ENVIADO", Set.of("RECEBIDO", "CANCELADO"),
            "RECEBIDO", Set.of(),
            "CANCELADO", Set.of());

    private final CompraJdbcRepository repo;
    private final UsuarioRepository usuarioRepository;

    public CompraAdminService(CompraJdbcRepository repo, UsuarioRepository usuarioRepository) {
        this.repo = repo;
        this.usuarioRepository = usuarioRepository;
    }

    // ================================================================ fornecedores

    @Transactional(readOnly = true)
    public List<FornecedorResponse> listarFornecedores() {
        return repo.listarFornecedores();
    }

    @Transactional
    public FornecedorResponse criarFornecedor(FornecedorRequest r) {
        Long id = repo.inserirFornecedor(r.razaoSocial().trim(), cnpjValido(r.cnpj()), limpo(r.telefone()), limpo(r.email()));
        return repo.buscarFornecedor(id).orElseThrow();
    }

    @Transactional
    public FornecedorResponse atualizarFornecedor(Long id, FornecedorRequest r) {
        fornecedor(id);
        repo.atualizarFornecedor(id, r.razaoSocial().trim(), cnpjValido(r.cnpj()), limpo(r.telefone()), limpo(r.email()));
        return repo.buscarFornecedor(id).orElseThrow();
    }

    @Transactional
    public FornecedorResponse alterarStatusFornecedor(Long id, boolean ativar) {
        fornecedor(id);
        if (!ativar && repo.comprasAbertasDoFornecedor(id) > 0) {
            throw new RegraDeNegocioException("Este fornecedor tem compras em aberto. Receba ou cancele antes de inativar.");
        }
        repo.statusFornecedor(id, ativar ? "ATIVO" : "INATIVO");
        return repo.buscarFornecedor(id).orElseThrow();
    }

    private FornecedorResponse fornecedor(Long id) {
        return repo.buscarFornecedor(id).orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado"));
    }

    private static String limpo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Confere os digitos verificadores e devolve o CNPJ formatado (00.000.000/0000-00). */
    static String cnpjValido(String bruto) {
        String d = bruto == null ? "" : bruto.replaceAll("\\D", "");
        if (d.length() != 14 || d.chars().distinct().count() == 1
                || digito(d, 12) != d.charAt(12) - '0' || digito(d, 13) != d.charAt(13) - '0') {
            throw new RegraDeNegocioException("CNPJ inválido");
        }
        return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "/" + d.substring(8, 12) + "-" + d.substring(12);
    }

    private static int digito(String d, int tamanho) {
        int soma = 0;
        int peso = tamanho - 7; // 5 para 12 digitos, 6 para 13
        for (int i = 0; i < tamanho; i++) {
            soma += (d.charAt(i) - '0') * peso--;
            if (peso < 2) peso = 9;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    // ================================================================ compras

    @Transactional(readOnly = true)
    public List<CompraResponse> listarCompras(String status) {
        return repo.listarCompras(status == null || status.isBlank() ? null : status.toUpperCase());
    }

    @Transactional(readOnly = true)
    public CompraResponse buscarCompra(Long id) {
        return repo.buscarCompra(id).orElseThrow(() -> new RecursoNaoEncontradoException("Compra não encontrada"));
    }

    @Transactional
    public CompraResponse criarCompra(CompraRequest r, String emailUsuario) {
        validar(r);
        Long usuarioId = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado")).getId();
        Long id = repo.inserirCompra(r.fornecedorId(), usuarioId);
        repo.gravarItens(id, r.fornecedorId(), r.itens());
        return buscarCompra(id);
    }

    /** Edicao so enquanto for rascunho. */
    @Transactional
    public CompraResponse atualizarCompra(Long id, CompraRequest r) {
        CompraCabecalho c = repo.buscarCompraComTrava(id).orElseThrow(() -> new RecursoNaoEncontradoException("Compra não encontrada"));
        if (!"RASCUNHO".equals(c.status())) {
            throw new RegraDeNegocioException("Só é possível editar compras em rascunho");
        }
        validar(r);
        repo.gravarItens(id, r.fornecedorId(), r.itens());
        return buscarCompra(id);
    }

    @Transactional
    public CompraResponse alterarStatus(Long id, String novoBruto, String emailUsuario) {
        String novo = novoBruto.trim().toUpperCase();
        CompraCabecalho c = repo.buscarCompraComTrava(id).orElseThrow(() -> new RecursoNaoEncontradoException("Compra não encontrada"));
        if (!TRANSICOES.containsKey(novo) || !TRANSICOES.get(c.status()).contains(novo)) {
            throw new RegraDeNegocioException("Não é possível mudar a compra de " + c.status() + " para " + novoBruto);
        }
        if ("RECEBIDO".equals(novo)) {
            Long usuarioId = usuarioRepository.findByEmail(emailUsuario)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado")).getId();
            repo.darEntradaNoEstoque(id, usuarioId);
        }
        repo.statusCompra(id, novo);
        return buscarCompra(id);
    }

    private void validar(CompraRequest r) {
        FornecedorResponse f = fornecedor(r.fornecedorId());
        if (!"ATIVO".equals(f.status())) {
            throw new RegraDeNegocioException("O fornecedor está inativo");
        }
        Set<Long> vistos = new HashSet<>();
        for (CompraItemRequest it : r.itens()) {
            if (!vistos.add(it.ingredienteId())) {
                throw new RegraDeNegocioException("Há ingrediente repetido na compra. Junte as quantidades em uma linha só.");
            }
        }
        Map<Long, String> status = repo.statusDosIngredientes(vistos);
        for (Long id : vistos) {
            if (!status.containsKey(id)) throw new RegraDeNegocioException("Ingrediente não encontrado: " + id);
            if (!"ATIVO".equals(status.get(id))) throw new RegraDeNegocioException("Há ingrediente inativo na compra");
        }
    }
}
