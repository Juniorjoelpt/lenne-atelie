package br.com.mssolucoes.atelie.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Amostra de nó: um fio de {@code fioCm} trabalhado no nó rendeu {@code rendeuCm} de peça. */
@Entity
@Table(name = "material_amostra")
@Getter
@Setter
public class MaterialAmostra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(name = "nome_no", nullable = false, length = 80)
    private String nomeNo;

    @Column(name = "fio_cm", nullable = false, precision = 8, scale = 2)
    private BigDecimal fioCm;

    @Column(name = "rendeu_cm", nullable = false, precision = 8, scale = 2)
    private BigDecimal rendeuCm;

    @Column(length = 200)
    private String observacao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    /** Quantos cm de fio cada cm de trabalho consome. */
    public BigDecimal getFator() {
        return fioCm.divide(rendeuCm, 2, RoundingMode.HALF_UP);
    }
}
