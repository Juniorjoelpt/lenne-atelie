package br.com.mssolucoes.atelie.dominio;

import br.com.mssolucoes.atelie.dominio.Enums.StatusOficina;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "oficina")
@Getter
@Setter
public class Oficina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    private LocalDateTime inicio;

    private LocalDateTime fim;

    @Column(name = "local_evento", length = 160)
    private String local;

    @Column(nullable = false)
    private int vagas;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOficina status = StatusOficina.ABERTA;

    @Column(nullable = false)
    private boolean publicada = true;

    /** Horas que saem no certificado. Vazio = calculado pelo início e fim. */
    @Column(name = "carga_horaria", precision = 5, scale = 1)
    private BigDecimal cargaHoraria;

    /** O que foi ensinado (sai no verso do certificado). */
    @Column(name = "conteudo_programatico", columnDefinition = "TEXT")
    private String conteudoProgramatico;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
