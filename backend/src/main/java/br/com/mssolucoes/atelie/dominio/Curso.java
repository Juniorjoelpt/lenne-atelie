package br.com.mssolucoes.atelie.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Curso online gravado: aulas em vídeo, vendido pelo site com Pix. */
@Entity
@Table(name = "curso")
@Getter
@Setter
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String titulo;

    @Column(length = 220)
    private String resumo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    /** Foto de capa na pasta pública de uploads. */
    @Column(length = 200)
    private String capa;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor = BigDecimal.ZERO;

    /** Iniciante, intermediário… */
    @Column(length = 40)
    private String nivel;

    @Column(columnDefinition = "TEXT")
    private String materiais;

    @Column(nullable = false)
    private boolean publicado;

    @Column(nullable = false)
    private int ordem;

    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem, id")
    private List<CursoAula> aulas = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
