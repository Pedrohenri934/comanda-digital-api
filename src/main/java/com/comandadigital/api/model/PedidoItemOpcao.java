package com.comandadigital.api.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Escolha feita pelo cliente dentro de um item de combo (ex.: Bebida = Guaraná). */
@Entity
@Table(name = "pedido_item_opcao")
public class PedidoItemOpcao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_item_id", nullable = false)
    private PedidoItem pedidoItem;

    @Column(name = "grupo_nome", nullable = false, length = 80)
    private String grupoNome;

    @Column(name = "opcao_nome", nullable = false, length = 80)
    private String opcaoNome;

    @Column(name = "prato_id")
    private Long pratoId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal acrescimo = BigDecimal.ZERO;

    public PedidoItemOpcao() {}

    public PedidoItemOpcao(String grupoNome, String opcaoNome, Long pratoId, BigDecimal acrescimo) {
        this.grupoNome = grupoNome;
        this.opcaoNome = opcaoNome;
        this.pratoId = pratoId;
        this.acrescimo = acrescimo;
    }

    public Long getId() { return id; }
    public PedidoItem getPedidoItem() { return pedidoItem; }
    public void setPedidoItem(PedidoItem pedidoItem) { this.pedidoItem = pedidoItem; }
    public String getGrupoNome() { return grupoNome; }
    public String getOpcaoNome() { return opcaoNome; }
    public Long getPratoId() { return pratoId; }
    public BigDecimal getAcrescimo() { return acrescimo; }
}
