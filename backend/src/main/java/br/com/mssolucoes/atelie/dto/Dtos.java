package br.com.mssolucoes.atelie.dto;

import br.com.mssolucoes.atelie.dominio.Enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Objetos de entrada e saída da API, agrupados por módulo. */
public final class Dtos {

    private Dtos() {
    }

    // ---------- Autenticação ----------

    public record LoginReq(
            @NotBlank(message = "Informe o e-mail.") String email,
            @NotBlank(message = "Informe a senha.") String senha) {
    }

    public record LoginResp(String token, String nome, String email) {
    }

    public record TrocarSenhaReq(
            @NotBlank(message = "Informe a senha atual.") String senhaAtual,
            @NotBlank @Size(min = 8, message = "A nova senha precisa ter pelo menos 8 caracteres.") String novaSenha) {
    }

    // ---------- Configurações do ateliê ----------

    public record ConfiguracaoDto(
            @NotBlank(message = "Informe o nome do ateliê.") @Size(max = 120) String nomeAtelie,
            @NotBlank(message = "Informe o WhatsApp.") @Size(max = 20) String whatsapp,
            @Size(max = 80) String instagram,
            @Size(max = 140) String chavePix,
            @Size(max = 120) String titularPix,
            @Size(max = 255) String endereco,
            @Size(max = 255) String horario,
            @Size(max = 120) String cidade,
            @Size(max = 4000) String sobreTexto) {
    }

    // ---------- Clientes ----------

    public record ClienteReq(
            @NotBlank(message = "Informe o nome.") @Size(max = 140) String nome,
            @Size(max = 20) String whatsapp,
            @Email(message = "E-mail inválido.") @Size(max = 160) String email,
            @Size(max = 120) String cidade,
            LocalDate aniversario,
            TipoCliente tipo,
            @Size(max = 4000) String observacoes) {
    }

    public record ClienteResp(Long id, String nome, String whatsapp, String email, String cidade,
                              LocalDate aniversario, TipoCliente tipo, String observacoes, LocalDateTime criadoEm) {
    }

    public record ClienteDetalhe(ClienteResp cliente, List<OrcamentoResp> orcamentos, List<EncomendaResp> encomendas,
                                 List<InscricaoResp> inscricoes, BigDecimal totalPago) {
    }

    // ---------- Orçamentos ----------

    public record OrcamentoPublicoReq(
            @NotBlank(message = "Informe seu nome.") @Size(max = 140) String nome,
            @NotBlank(message = "Informe seu WhatsApp.") @Size(max = 20) String whatsapp,
            @NotBlank(message = "Escolha o tipo de peça.") @Size(max = 80) String tipoPeca,
            @Size(max = 160) String medidas,
            @Size(max = 160) String cores,
            LocalDate dataDesejada,
            @Size(max = 2000) String detalhes,
            /** Campo invisível no formulário: se vier preenchido, é robô. */
            String site) {
    }

    public record OrcamentoReq(
            Long clienteId,
            @NotBlank(message = "Informe o nome.") @Size(max = 140) String nome,
            @Size(max = 20) String whatsapp,
            @NotBlank(message = "Informe o tipo de peça.") @Size(max = 80) String tipoPeca,
            @Size(max = 160) String medidas,
            @Size(max = 160) String cores,
            LocalDate dataDesejada,
            @Size(max = 4000) String detalhes,
            @PositiveOrZero(message = "Valor inválido.") BigDecimal valorProposto) {
    }

    public record OrcamentoResp(Long id, Long clienteId, String nome, String whatsapp, String tipoPeca, String medidas,
                                String cores, LocalDate dataDesejada, String detalhes, BigDecimal valorProposto,
                                StatusOrcamento status, Origem origem, LocalDateTime criadoEm, Long encomendaId) {
    }

    public record StatusOrcamentoReq(@NotNull(message = "Informe o status.") StatusOrcamento status) {
    }

    public record AprovarOrcamentoReq(
            @NotBlank(message = "Descreva a encomenda.") @Size(max = 200) String descricao,
            @NotNull(message = "Escolha a frente.") Frente frente,
            @NotNull(message = "Informe o valor total.") @PositiveOrZero BigDecimal valorTotal,
            @PositiveOrZero BigDecimal valorSinal,
            LocalDate prazoEntrega) {
    }

    // ---------- Encomendas ----------

    public record EncomendaReq(
            @NotNull(message = "Escolha a cliente.") Long clienteId,
            @NotBlank(message = "Descreva a encomenda.") @Size(max = 200) String descricao,
            @NotNull(message = "Escolha a frente.") Frente frente,
            @NotNull(message = "Informe o valor total.") @PositiveOrZero BigDecimal valorTotal,
            @PositiveOrZero BigDecimal valorSinal,
            LocalDate prazoEntrega,
            StatusEncomenda status,
            @Size(max = 4000) String observacoes) {
    }

    public record EncomendaResp(Long id, Long clienteId, String clienteNome, String clienteWhatsapp, Long orcamentoId,
                                String descricao, Frente frente, BigDecimal valorTotal, BigDecimal valorSinal,
                                BigDecimal valorPago, BigDecimal saldo, LocalDate prazoEntrega,
                                StatusEncomenda status, String observacoes, LocalDateTime criadoEm) {
    }

    public record StatusEncomendaReq(@NotNull(message = "Informe o status.") StatusEncomenda status) {
    }

    public record PagamentoReq(
            @NotNull(message = "Informe o valor.") @Positive(message = "O valor precisa ser maior que zero.") BigDecimal valor,
            LocalDate data,
            FormaPagamento formaPagamento,
            @Size(max = 200) String descricao) {
    }

    // ---------- Oficinas ----------

    public record OficinaReq(
            @NotBlank(message = "Informe o título.") @Size(max = 140) String titulo,
            @Size(max = 4000) String descricao,
            @NotNull(message = "Informe data e hora de início.") LocalDateTime inicio,
            LocalDateTime fim,
            @Size(max = 160) String local,
            @NotNull @Min(value = 1, message = "Informe pelo menos 1 vaga.") Integer vagas,
            @NotNull(message = "Informe o valor.") @PositiveOrZero BigDecimal valor,
            StatusOficina status,
            Boolean publicada) {
    }

    public record OficinaResp(Long id, String titulo, String descricao, LocalDateTime inicio, LocalDateTime fim,
                              String local, int vagas, BigDecimal valor, StatusOficina status, boolean publicada,
                              long confirmadas, long aConferir, long aguardandoPix, long listaEspera,
                              long vagasRestantes) {
    }

    public record OficinaPublica(Long id, String titulo, String descricao, LocalDateTime inicio, LocalDateTime fim,
                                 String local, BigDecimal valor, long vagasRestantes, boolean esgotada) {
    }

    public record InscricaoPublicaReq(
            @NotBlank(message = "Informe seu nome.") @Size(max = 140) String nome,
            @NotBlank(message = "Informe seu WhatsApp.") @Size(max = 20) String whatsapp,
            String site) {
    }

    public record InscricaoPublicaResp(String codigo, StatusInscricao status, String oficina, LocalDateTime inicio,
                                       BigDecimal valor, String chavePix, String titularPix, String whatsappAtelie) {
    }

    public record InscricaoReq(
            @NotBlank(message = "Informe o nome.") @Size(max = 140) String nome,
            @NotBlank(message = "Informe o WhatsApp.") @Size(max = 20) String whatsapp,
            StatusInscricao status) {
    }

    public record InscricaoResp(Long id, Long oficinaId, String oficina, LocalDateTime inicio, Long clienteId,
                                String nome, String whatsapp, StatusInscricao status, Origem origem,
                                LocalDateTime criadoEm) {
    }

    public record StatusInscricaoReq(
            @NotNull(message = "Informe o status.") StatusInscricao status,
            FormaPagamento formaPagamento) {
    }

    // ---------- Financeiro ----------

    public record LancamentoReq(
            @NotNull(message = "Escolha entrada ou saída.") TipoLancamento tipo,
            @NotNull(message = "Escolha a categoria.") CategoriaLancamento categoria,
            @NotBlank(message = "Descreva o lançamento.") @Size(max = 200) String descricao,
            @NotNull(message = "Informe o valor.") @Positive(message = "O valor precisa ser maior que zero.") BigDecimal valor,
            @NotNull(message = "Informe a data.") LocalDate data,
            FormaPagamento formaPagamento) {
    }

    public record LancamentoResp(Long id, TipoLancamento tipo, CategoriaLancamento categoria, String descricao,
                                 BigDecimal valor, LocalDate data, FormaPagamento formaPagamento,
                                 Long encomendaId, Long inscricaoId, boolean automatico) {
    }

    public record ValorPorNome(String nome, BigDecimal valor, int percentual) {
    }

    public record ResumoFinanceiro(int ano, int mes, BigDecimal entradas, BigDecimal saidas, BigDecimal saldo,
                                   BigDecimal aReceber, List<ValorPorNome> entradasPorCategoria,
                                   List<LancamentoResp> lancamentos, List<EncomendaResp> contasAReceber) {
    }

    // ---------- Materiais ----------

    public record MaterialReq(
            @NotBlank(message = "Informe o nome.") @Size(max = 140) String nome,
            @NotBlank(message = "Informe a unidade.") @Size(max = 20) String unidade,
            @NotNull @PositiveOrZero BigDecimal quantidade,
            @NotNull @PositiveOrZero BigDecimal estoqueMinimo,
            @NotNull @PositiveOrZero BigDecimal custoUnitario) {
    }

    public record MaterialResp(Long id, String nome, String unidade, BigDecimal quantidade, BigDecimal estoqueMinimo,
                               BigDecimal custoUnitario, boolean abaixoDoMinimo) {
    }

    public record AjusteEstoqueReq(
            @NotNull(message = "Escolha entrada ou saída.") TipoLancamento tipo,
            @NotNull @Positive(message = "A quantidade precisa ser maior que zero.") BigDecimal quantidade,
            /** Na entrada (compra), registra a despesa no financeiro. */
            boolean registrarDespesa,
            @PositiveOrZero BigDecimal valorCompra) {
    }

    // ---------- Portfólio ----------

    public record PortfolioResp(Long id, String titulo, CategoriaPortfolio categoria, String url, int ordem,
                                boolean publicado) {
    }

    public record PortfolioReq(
            @NotBlank(message = "Informe o título.") @Size(max = 140) String titulo,
            @NotNull(message = "Escolha a categoria.") CategoriaPortfolio categoria,
            Integer ordem,
            Boolean publicado) {
    }

    // ---------- Site e painel ----------

    public record SitePublico(ConfiguracaoDto atelie, List<OficinaPublica> oficinas, List<PortfolioResp> portfolio) {
    }

    public record Barra(String rotulo, BigDecimal valor) {
    }

    public record Painel(
            String saudacao,
            long orcamentosAbertos,
            long orcamentosNovos,
            long emProducao,
            long entregasSemana,
            BigDecimal aReceber,
            long contasAReceber,
            long pixAConferir,
            OficinaResp proximaOficina,
            List<Barra> recebidoPorSemana,
            List<ValorPorNome> receitaPorFrente,
            List<EncomendaResp> proximasEntregas,
            List<MaterialResp> materiaisBaixos,
            String mesReferencia) {
    }
}
