package com.comandadigital.api.service;

import com.comandadigital.api.dto.AdminDtos.IngredienteResponse;
import com.comandadigital.api.dto.DashboardDtos.*;
import com.comandadigital.api.exception.RegraDeNegocioException;
import com.comandadigital.api.repository.CadastroJdbcRepository;
import com.comandadigital.api.repository.DashboardJdbcRepository;
import com.comandadigital.api.repository.DashboardJdbcRepository.Resumo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final DashboardJdbcRepository repo;
    private final CadastroJdbcRepository cadastro;

    public DashboardService(DashboardJdbcRepository repo, CadastroJdbcRepository cadastro) {
        this.repo = repo;
        this.cadastro = cadastro;
    }

    @Transactional(readOnly = true)
    public DashboardResponse montar(int dias) {
        if (dias < 1 || dias > 90) {
            throw new RegraDeNegocioException("O período deve ser de 1 a 90 dias");
        }
        LocalDate primeiroDia = LocalDate.now().minusDays(dias - 1L);
        Timestamp inicio = Timestamp.valueOf(primeiroDia.atStartOfDay());

        Resumo r = repo.resumo(inicio);
        BigDecimal ticket = r.vendas() == 0 ? BigDecimal.ZERO
                : r.faturamento().divide(BigDecimal.valueOf(r.vendas()), 2, RoundingMode.HALF_UP);
        Totais totais = new Totais(r.pedidos(), r.vendas(), r.cancelados(), repo.pedidosAbertos(), r.faturamento(), ticket);

        // Preenche com zero os dias sem venda, para o grafico nao ter buracos.
        Map<LocalDate, VendaDia> porData = new HashMap<>();
        for (VendaDia v : repo.vendasPorDia(inicio)) porData.put(v.data(), v);
        List<VendaDia> serie = new ArrayList<>();
        for (int i = 0; i < dias; i++) {
            LocalDate d = primeiroDia.plusDays(i);
            serie.add(porData.getOrDefault(d, new VendaDia(d, 0, BigDecimal.ZERO)));
        }

        List<IngredienteResponse> baixo = cadastro.listarIngredientes().stream()
                .filter(i -> i.abaixoDoMinimo() && "ATIVO".equals(i.status())).toList();

        return new DashboardResponse(dias, totais, repo.pedidosPorStatus(inicio), serie, repo.maisVendidos(inicio, 5), baixo);
    }
}
