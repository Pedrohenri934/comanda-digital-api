package com.comandadigital.api.repository;

import com.comandadigital.api.dto.EntregadorDtos.Entrega;
import com.comandadigital.api.dto.EntregadorDtos.EntregadorAdmin;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class EntregadorJdbcRepository {

    public record Dados(long usuarioId, String nome, String email, String telefone, String veiculo, String placa,
                        String statusCadastro, boolean disponivel) {}

    private static final RowMapper<Dados> DADOS = (rs, n) -> new Dados(rs.getLong("id"), rs.getString("nome"),
            rs.getString("email"), rs.getString("telefone"), rs.getString("veiculo"), rs.getString("placa"),
            rs.getString("status_cadastro"), rs.getBoolean("disponivel"));

    private static final String SELECT_ENTREGA = """
            SELECT o.id, u.nome AS cliente, u.telefone, o.endereco_entrega, o.valor_total, o.pago, o.observacoes,
                   o.status, o.created_at,
                   (SELECT GROUP_CONCAT(CONCAT(i.quantidade, 'x ', p.nome) SEPARATOR ', ')
                      FROM pedido_item i JOIN prato p ON p.id = i.prato_id WHERE i.pedido_id = o.id) AS itens
            FROM pedido o
            JOIN usuario u ON u.id = o.cliente_id
            """;

    private static final RowMapper<Entrega> ENTREGA = (rs, n) -> new Entrega(rs.getLong("id"), rs.getString("cliente"),
            rs.getString("telefone"), rs.getString("endereco_entrega"), rs.getBigDecimal("valor_total"),
            rs.getBoolean("pago"), rs.getString("observacoes"), rs.getString("itens"), rs.getString("status"),
            rs.getTimestamp("created_at").toLocalDateTime());

    private final NamedParameterJdbcTemplate jdbc;

    public EntregadorJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------- cadastro e perfil ----------

    public void inserir(long usuarioId, String veiculo, String placa) {
        jdbc.update("INSERT INTO entregador (usuario_id, veiculo, placa) VALUES (:u, :v, :p)",
                new MapSqlParameterSource().addValue("u", usuarioId).addValue("v", veiculo).addValue("p", placa));
    }

    public Optional<Dados> buscar(long usuarioId) {
        return jdbc.query("""
                SELECT u.id, u.nome, u.email, u.telefone, e.veiculo, e.placa, e.status_cadastro, e.disponivel
                FROM entregador e JOIN usuario u ON u.id = e.usuario_id WHERE u.id = :id
                """, new MapSqlParameterSource("id", usuarioId), DADOS).stream().findFirst();
    }

    public int definirDisponivel(long usuarioId, boolean disponivel) {
        return jdbc.update("UPDATE entregador SET disponivel = :d WHERE usuario_id = :id",
                new MapSqlParameterSource().addValue("d", disponivel).addValue("id", usuarioId));
    }

    /** Aprovar ou recusar. Quem e recusado sai da lista de disponiveis. */
    public int atualizarStatus(long usuarioId, String status) {
        return jdbc.update("UPDATE entregador SET status_cadastro = :s, disponivel = FALSE WHERE usuario_id = :id",
                new MapSqlParameterSource().addValue("s", status).addValue("id", usuarioId));
    }

    // ---------- numeros do painel ----------

    public long pedidosDisponiveis() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM pedido WHERE status = 'PRONTO' AND entregador_id IS NULL",
                new MapSqlParameterSource(), Long.class);
        return n == null ? 0 : n;
    }

    public long emEntrega(long usuarioId) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM pedido WHERE status = 'SAIU_ENTREGA' AND entregador_id = :id",
                new MapSqlParameterSource("id", usuarioId), Long.class);
        return n == null ? 0 : n;
    }

    public long entregues(long usuarioId, Timestamp desde) {
        Long n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM pedido
                WHERE status = 'FINALIZADO' AND entregador_id = :id AND updated_at >= :desde
                """, new MapSqlParameterSource().addValue("id", usuarioId).addValue("desde", desde), Long.class);
        return n == null ? 0 : n;
    }

    public long entreguesTotal(long usuarioId) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM pedido WHERE status = 'FINALIZADO' AND entregador_id = :id",
                new MapSqlParameterSource("id", usuarioId), Long.class);
        return n == null ? 0 : n;
    }

    // ---------- entregas ----------

    public List<Entrega> listarDisponiveis() {
        return jdbc.query(SELECT_ENTREGA + " WHERE o.status = 'PRONTO' AND o.entregador_id IS NULL ORDER BY o.created_at",
                new MapSqlParameterSource(), ENTREGA);
    }

    public List<Entrega> listarAtivas(long usuarioId) {
        return jdbc.query(SELECT_ENTREGA + " WHERE o.status = 'SAIU_ENTREGA' AND o.entregador_id = :id ORDER BY o.created_at",
                new MapSqlParameterSource("id", usuarioId), ENTREGA);
    }

    /** Aceita so se o pedido ainda esta PRONTO e sem entregador (evita dois entregadores no mesmo pedido). */
    public int aceitar(long pedidoId, long usuarioId) {
        return jdbc.update("""
                UPDATE pedido SET status = 'SAIU_ENTREGA', entregador_id = :u
                WHERE id = :p AND status = 'PRONTO' AND entregador_id IS NULL
                """, new MapSqlParameterSource().addValue("u", usuarioId).addValue("p", pedidoId));
    }

    /** So quem pegou o pedido pode finalizar a entrega. */
    public int entregar(long pedidoId, long usuarioId) {
        return jdbc.update("""
                UPDATE pedido SET status = 'FINALIZADO'
                WHERE id = :p AND entregador_id = :u AND status = 'SAIU_ENTREGA'
                """, new MapSqlParameterSource().addValue("u", usuarioId).addValue("p", pedidoId));
    }

    // ---------- admin ----------

    public List<EntregadorAdmin> listarParaAdmin() {
        return jdbc.query("""
                SELECT u.id, u.nome, u.email, u.telefone, e.veiculo, e.placa, e.status_cadastro, e.disponivel,
                       (SELECT COUNT(*) FROM pedido o WHERE o.entregador_id = u.id AND o.status = 'FINALIZADO') AS concluidas,
                       (SELECT COUNT(*) FROM pedido o WHERE o.entregador_id = u.id AND o.status = 'SAIU_ENTREGA') AS andamento
                FROM entregador e JOIN usuario u ON u.id = e.usuario_id
                ORDER BY e.status_cadastro, u.nome
                """, new MapSqlParameterSource(),
                (rs, n) -> new EntregadorAdmin(rs.getLong("id"), rs.getString("nome"), rs.getString("email"),
                        rs.getString("telefone"), rs.getString("veiculo"), rs.getString("placa"),
                        rs.getString("status_cadastro"), rs.getBoolean("disponivel"),
                        rs.getLong("concluidas"), rs.getLong("andamento")));
    }
}
