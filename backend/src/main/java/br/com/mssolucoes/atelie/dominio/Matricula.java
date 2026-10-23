package br.com.mssolucoes.atelie.dominio;

import br.com.mssolucoes.atelie.dominio.Enums.Origem;
import br.com.mssolucoes.atelie.dominio.Enums.StatusMatricula;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Compra de um curso online. O código é o link de acesso da aluna. */
@Entity
@Table(name = "matricula")
@Getter
@Setter
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(nullable = false, length = 36, unique = true)
    private String codigo = java.util.UUID.randomUUID().toString();

    @Column(nullable = false, length = 140)
    private String nome;

    @Column(nullable = false, length = 20)
    private String whatsapp;

    @Column(length = 160)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusMatricula status = StatusMatricula.AGUARDANDO_PIX;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Origem origem = Origem.MANUAL;

    @Column(name = "liberada_em")
    private LocalDateTime liberadaEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
