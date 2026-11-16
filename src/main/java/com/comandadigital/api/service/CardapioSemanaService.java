package com.comandadigital.api.service;

import com.comandadigital.api.dto.CardapioSemanaDtos.*;
import com.comandadigital.api.exception.RegraDeNegocioException;
import com.comandadigital.api.repository.CardapioSemanaJdbcRepository;
import com.comandadigital.api.repository.CardapioSemanaJdbcRepository.Linha;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CardapioSemanaService {

    private static final String[] NOMES = {"Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"};
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final CardapioSemanaJdbcRepository repo;

    public CardapioSemanaService(CardapioSemanaJdbcRepository repo) {
        this.repo = repo;
    }

    private static int diaDeHoje() {
        return LocalDate.now(FUSO).getDayOfWeek().getValue(); // segunda = 1 ... domingo = 7
    }

    private static ItemDia paraItem(Linha l) {
        return new ItemDia(l.pratoId(), l.nome(), l.categoria(), l.fotoUrl(), l.preco(), l.status(), l.destaque());
    }

    private static void validarDia(int dia) {
        if (dia < 1 || dia > 7) throw new RegraDeNegocioException("Dia da semana inválido (use 1 a 7)");
    }

    @Transactional(readOnly = true)
    public List<DiaCardapio> listar() {
        Map<Integer, List<ItemDia>> porDia = new LinkedHashMap<>();
        for (int d = 1; d <= 7; d++) porDia.put(d, new ArrayList<>());
        for (Linha l : repo.listarTodos()) porDia.get(l.dia()).add(paraItem(l));

        int hoje = diaDeHoje();
        List<DiaCardapio> saida = new ArrayList<>();
        for (int d = 1; d <= 7; d++) saida.add(new DiaCardapio(d, NOMES[d - 1], d == hoje, porDia.get(d)));
        return saida;
    }

    /** Troca todo o cardapio de um dia pela lista enviada. */
    @Transactional
    public DiaCardapio salvarDia(int dia, SalvarDiaRequest req) {
        validarDia(dia);
        Map<Long, Boolean> pratos = new LinkedHashMap<>();
        for (ItemRequest i : req.itens()) pratos.put(i.pratoId(), i.destaque());

        if (repo.contarPratos(pratos.keySet()) != pratos.size()) {
            throw new RegraDeNegocioException("Algum prato informado não existe");
        }
        repo.limparDia(dia);
        pratos.forEach((pratoId, destaque) -> repo.inserir(dia, pratoId, destaque));

        return listar().get(dia - 1);
    }

    /** Pratos do dia de hoje que o cliente pode pedir. */
    @Transactional(readOnly = true)
    public List<ItemHoje> hoje() {
        return repo.listarDiaAtivos(diaDeHoje()).stream()
                .map(l -> new ItemHoje(l.pratoId(), l.nome(), l.descricao(), l.categoria(), l.fotoUrl(), l.preco(),
                        l.tempoPreparoMin(), l.destaque()))
                .toList();
    }
}
