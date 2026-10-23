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
            @Size(max = 4000) String observacoes,
            @Size(max = 30) String documento,
            @Size(max = 255) String endereco) {
    }

    public record ClienteResp(Long id, String nome, String whatsapp, String email, String cidade,
                              LocalDate aniversario, TipoCliente tipo, String observacoes, LocalDateTime criadoEm,
                              String documento, String endereco) {
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
            Long produtoId,
            @Min(value = 1, message = "A quantidade mínima é 1.") Integer quantidade,
            @NotNull(message = "Informe o valor total.") @PositiveOrZero BigDecimal valorTotal,
            @PositiveOrZero BigDecimal valorSinal,
            LocalDate prazoEntrega) {
    }

    // ---------- Encomendas ----------

    public record EncomendaReq(
            @NotNull(message = "Escolha a cliente.") Long clienteId,
            @NotBlank(message = "Descreva a encomenda.") @Size(max = 200) String descricao,
            @NotNull(message = "Escolha a frente.") Frente frente,
            Long produtoId,
            @Min(value = 1, message = "A quantidade mínima é 1.") Integer quantidade,
            @PositiveOrZero(message = "Horas inválidas.") BigDecimal horasTrabalhadas,
            @NotNull(message = "Informe o valor total.") @PositiveOrZero BigDecimal valorTotal,
            @PositiveOrZero BigDecimal valorSinal,
            LocalDate prazoEntrega,
            StatusEncomenda status,
            @Size(max = 4000) String observacoes) {
    }

    public record EncomendaResp(Long id, Long clienteId, String clienteNome, String clienteWhatsapp, Long orcamentoId,
                                String descricao, Frente frente, Long produtoId, String produtoNome, int quantidade,
                                BigDecimal horasTrabalhadas, boolean materiaisBaixados,
                                BigDecimal valorTotal, BigDecimal valorSinal,
                                BigDecimal valorPago, BigDecimal saldo, LocalDate prazoEntrega,
                                StatusEncomenda status, String observacoes, LocalDateTime criadoEm,
                                CustosEncomenda custos, LocalDate entregueEm) {
    }

    /**
     * Quanto a encomenda custou e quanto sobrou.
     * "ganho" = valor cobrado − materiais − custos fixos − outros (o que remunera o trabalho dela + lucro).
     * "estimado" = materiais ainda não baixados; o custo vem da ficha técnica.
     */
    public record CustosEncomenda(BigDecimal custoMateriais, BigDecimal horas, BigDecimal custoFixo,
                                  BigDecimal outrosCustos, BigDecimal ganho, BigDecimal ganhoPorHora,
                                  BigDecimal valorHoraDesejado, BigDecimal lucro, boolean estimado) {
    }

    public record ConsumoReq(
            @NotNull(message = "Escolha o material.") Long materialId,
            @NotNull @Positive(message = "A quantidade precisa ser maior que zero.") BigDecimal quantidade) {
    }

    public record ConsumoResp(Long id, Long materialId, String materialNome, String unidade, BigDecimal quantidade,
                              BigDecimal custoUnitario, BigDecimal custoTotal, OrigemConsumo origem, LocalDate data) {
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
                                 Long encomendaId, Long inscricaoId, Long vendaId, boolean automatico) {
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
            @NotNull(message = "Informe a quantidade em estoque.") BigDecimal quantidade,
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

    // ---------- Precificação ----------

    public record PrecificacaoReq(
            @NotNull @PositiveOrZero(message = "Valor inválido.") BigDecimal salarioDesejado,
            @NotNull @Min(value = 1, message = "Informe as horas de produção por mês.") @Max(value = 400, message = "Horas por mês acima de 400.") Integer horasMes,
            @NotNull @PositiveOrZero(message = "Valor inválido.") BigDecimal custosFixosMensais,
            @NotNull @PositiveOrZero @Max(value = 500, message = "Margem acima de 500%.") BigDecimal margemPadrao) {
    }

    public record PrecificacaoResp(BigDecimal salarioDesejado, int horasMes, BigDecimal custosFixosMensais,
                                   BigDecimal margemPadrao, BigDecimal valorHora, BigDecimal custoFixoHora,
                                   boolean configurada) {
    }

    // ---------- Produtos / fichas técnicas ----------

    public record ItemFichaReq(
            @NotNull(message = "Escolha o material.") Long materialId,
            @NotNull @Positive(message = "Informe a quantidade de cada material.") BigDecimal quantidade) {
    }

    public record ProdutoReq(
            @NotBlank(message = "Informe o nome da peça.") @Size(max = 140) String nome,
            @Size(max = 4000) String descricao,
            @NotNull(message = "Escolha a frente.") Frente frente,
            @NotNull @PositiveOrZero(message = "Horas inválidas.") BigDecimal horasMaoObra,
            @PositiveOrZero(message = "Valor inválido.") BigDecimal outrosCustos,
            @PositiveOrZero @Max(value = 500, message = "Margem acima de 500%.") BigDecimal margem,
            @PositiveOrZero(message = "Preço inválido.") BigDecimal precoVenda,
            Boolean prontaEntrega,
            @PositiveOrZero(message = "Estoque inválido.") Integer estoque,
            Boolean ativo,
            List<@jakarta.validation.Valid ItemFichaReq> materiais) {
    }

    public record ItemFichaResp(Long materialId, String materialNome, String unidade, BigDecimal quantidade,
                                BigDecimal custoUnitario, BigDecimal custoLinha) {
    }

    /** Composição do preço de UMA peça. */
    public record CustoFicha(BigDecimal custoMateriais, BigDecimal custoMaoObra, BigDecimal custoFixo,
                             BigDecimal outrosCustos, BigDecimal custoTotal, BigDecimal margem,
                             BigDecimal precoSugerido, BigDecimal precoAtual, BigDecimal lucroUnitario,
                             BigDecimal ganhoPorHora) {
    }

    public record ProdutoResp(Long id, String nome, String descricao, Frente frente, BigDecimal horasMaoObra,
                              BigDecimal outrosCustos, BigDecimal margem, BigDecimal precoVenda, boolean prontaEntrega,
                              int estoque, boolean ativo, List<ItemFichaResp> materiais, CustoFicha custo) {
    }

    public record ProducaoReq(@NotNull @Min(value = 1, message = "Informe quantas peças ficaram prontas.") Integer quantidade) {
    }

    // ---------- Vendas (pronta-entrega) ----------

    public record VendaItemReq(
            Long produtoId,
            @Size(max = 160) String descricao,
            @NotNull @Min(value = 1, message = "A quantidade mínima é 1.") Integer quantidade,
            @PositiveOrZero(message = "Preço inválido.") BigDecimal precoUnitario) {
    }

    public record VendaReq(
            LocalDate data,
            Long clienteId,
            CanalVenda canal,
            FormaPagamento formaPagamento,
            @PositiveOrZero(message = "Desconto inválido.") BigDecimal desconto,
            @Size(max = 255) String observacao,
            @NotEmpty(message = "Adicione pelo menos uma peça.") List<@jakarta.validation.Valid VendaItemReq> itens) {
    }

    public record VendaItemResp(Long produtoId, String descricao, int quantidade, BigDecimal precoUnitario,
                                BigDecimal custoUnitario) {
    }

    public record VendaResp(Long id, LocalDate data, Long clienteId, String clienteNome, CanalVenda canal,
                            FormaPagamento formaPagamento, BigDecimal desconto, BigDecimal total, BigDecimal custo,
                            String observacao, StatusVenda status, List<VendaItemResp> itens) {
    }

    public record ResumoVendas(int ano, int mes, BigDecimal totalVendido, BigDecimal custoDireto, BigDecimal sobra,
                               int pecas, int quantidadeVendas, List<ValorPorNome> porCanal, List<VendaResp> vendas) {
    }

    // ---------- Proposta e contrato ----------

    public record DocumentosConfig(
            @Size(max = 120) String responsavel,
            @Size(max = 30) String documento,
            @NotNull @PositiveOrZero @Max(value = 100, message = "O sinal vai de 0 a 100%.") BigDecimal sinalPercentual,
            @NotNull @Min(value = 1, message = "Validade mínima de 1 dia.") @Max(value = 365) Integer validadePropostaDias,
            @Size(max = 4000) String condicoesProposta,
            @Size(max = 4000) String politicaCancelamento) {
    }

    // ---------- Agenda ----------

    public record CompromissoReq(
            @NotBlank(message = "Informe o título.") @Size(max = 160) String titulo,
            TipoCompromisso tipo,
            @NotNull(message = "Informe data e hora.") LocalDateTime inicio,
            LocalDateTime fim,
            @Size(max = 160) String local,
            Long clienteId,
            Long encomendaId,
            @Size(max = 4000) String observacoes,
            Boolean concluido) {
    }

    public record CompromissoResp(Long id, String titulo, TipoCompromisso tipo, LocalDateTime inicio,
                                  LocalDateTime fim, String local, Long clienteId, String clienteNome,
                                  Long encomendaId, String encomendaDescricao, String observacoes, boolean concluido) {
    }

    /**
     * Item da agenda. tipo: ENTREGA, OFICINA, COMPROMISSO, ANIVERSARIO.
     * hora vazia = o dia todo.
     */
    public record EventoAgenda(String tipo, Long id, LocalDate data, java.time.LocalTime hora, String titulo,
                               String detalhe, String status, boolean concluido) {
    }

    // ---------- Lembretes ----------

    /** Lembrete pronto para enviar pelo WhatsApp (a mensagem já vem escrita). */
    public record LembreteResp(String chave, String tipo, String titulo, String detalhe, String nome,
                               String whatsapp, String mensagem, LocalDate data) {
    }

    public record MarcarLembreteReq(@NotBlank @Size(max = 120) String chave) {
    }

    // ---------- Fotos da encomenda ----------

    public record FotoResp(Long id, Long encomendaId, TipoFoto tipo, String legenda, String url,
                           Long portfolioItemId, LocalDateTime criadoEm) {
    }

    public record PublicarFotoReq(@Size(max = 140) String titulo, CategoriaPortfolio categoria) {
    }

    // ---------- Relatórios ----------

    public record Comparativo(BigDecimal atual, BigDecimal anterior, Integer variacaoPercentual) {
    }

    public record MesValores(String mes, BigDecimal entradas, BigDecimal saidas) {
    }

    /** Resultado das encomendas entregues, agrupadas pela ficha técnica. */
    public record LinhaPeca(String nome, int pecas, BigDecimal faturamento, BigDecimal materiais,
                            BigDecimal ganho, BigDecimal horas, BigDecimal ganhoPorHora) {
    }

    public record LinhaVenda(String nome, int pecas, BigDecimal faturamento, BigDecimal custo, BigDecimal sobra) {
    }

    public record LinhaRanking(String nome, int pecas, int emEncomendas, int emVendas, BigDecimal faturamento) {
    }

    public record LinhaMaterial(String nome, String unidade, BigDecimal quantidade, BigDecimal custo) {
    }

    public record Relatorio(LocalDate de, LocalDate ate, LocalDate deAnterior, LocalDate ateAnterior,
                            Comparativo entradas, Comparativo saidas, Comparativo resultado,
                            int encomendasEntregues, BigDecimal ticketMedio, int oficinas, int alunas,
                            List<MesValores> evolucao, List<ValorPorNome> porFrente, List<LinhaPeca> porPeca,
                            List<LinhaVenda> prontaEntrega, BigDecimal descontosVendas,
                            List<LinhaRanking> maisVendidos, List<LinhaMaterial> materiais,
                            List<LancamentoResp> lancamentos) {
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
            String mesReferencia,
            int lembretes) {
    }
}
