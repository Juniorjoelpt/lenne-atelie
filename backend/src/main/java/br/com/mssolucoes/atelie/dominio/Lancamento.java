package br.com.mssolucoes.atelie.dominio;

import br.com.mssolucoes.atelie.dominio.Enums.CategoriaLancamento;
import br.com.mssolucoes.atelie.dominio.Enums.FormaPagamento;
import br.com.mssolucoes.atelie.dominio.Enums.TipoLancamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Entrada ou saída de dinheiro. */
@Entity
@Table(name = "lancamento")
@Getter
@Setter
public class Lancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoLancamento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaLancamento categoria;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_lancamento", nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private FormaPagamento formaPagamento = FormaPagamento.PIX;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encomenda_id")
    private Encomenda encomenda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inscricao_id")
    private Inscricao inscricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id")
    private Venda venda;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
