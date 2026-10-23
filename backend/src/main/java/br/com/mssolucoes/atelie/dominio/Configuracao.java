package br.com.mssolucoes.atelie.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Dados do ateliê exibidos no site (registro único, id = 1). */
@Entity
@Table(name = "configuracao")
@Getter
@Setter
public class Configuracao {

    public static final long ID_UNICO = 1L;

    @Id
    private Long id = ID_UNICO;

    @Column(name = "nome_atelie", nullable = false, length = 120)
    private String nomeAtelie;

    /** Somente dígitos, com DDI: 5598988793891 */
    @Column(nullable = false, length = 20)
    private String whatsapp;

    @Column(length = 80)
    private String instagram;

    @Column(name = "chave_pix", length = 140)
    private String chavePix;

    @Column(name = "titular_pix", length = 120)
    private String titularPix;

    @Column(length = 255)
    private String endereco;

    @Column(length = 255)
    private String horario;

    @Column(length = 120)
    private String cidade;

    @Column(name = "sobre_texto", columnDefinition = "TEXT")
    private String sobreTexto;

    // ---- Precificação (só no sistema, nunca no site) ----

    /** Quanto ela quer tirar por mês com o próprio trabalho. */
    @Column(name = "salario_desejado", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal salarioDesejado = java.math.BigDecimal.ZERO;

    /** Horas dedicadas à produção por mês. */
    @Column(name = "horas_mes", nullable = false)
    private int horasMes = 160;

    /** Aluguel, energia, internet, MEI… */
    @Column(name = "custos_fixos_mensais", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal custosFixosMensais = java.math.BigDecimal.ZERO;

    /** Margem de lucro padrão, em %, aplicada sobre o custo total. */
    @Column(name = "margem_padrao", nullable = false, precision = 5, scale = 2)
    private java.math.BigDecimal margemPadrao = java.math.BigDecimal.valueOf(30);

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
