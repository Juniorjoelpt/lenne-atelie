package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Enums.*;
import br.com.mssolucoes.atelie.dominio.Lancamento;
import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.repositorio.EncomendaRepository;
import br.com.mssolucoes.atelie.repositorio.InscricaoRepository;
import br.com.mssolucoes.atelie.repositorio.LancamentoRepository;
import br.com.mssolucoes.atelie.repositorio.OrcamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class PainelService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final OrcamentoRepository orcamentos;
    private final EncomendaRepository encomendas;
    private final InscricaoRepository inscricoes;
    private final LancamentoRepository lancamentos;
    private final EncomendaService encomendaService;
    private final FinanceiroService financeiro;
    private final OficinaService oficinas;
    private final MaterialService materiais;

    public PainelService(OrcamentoRepository orcamentos, EncomendaRepository encomendas, InscricaoRepository inscricoes,
                         LancamentoRepository lancamentos, EncomendaService encomendaService,
                         FinanceiroService financeiro, OficinaService oficinas, MaterialService materiais) {
        this.orcamentos = orcamentos;
        this.encomendas = encomendas;
        this.inscricoes = inscricoes;
        this.lancamentos = lancamentos;
        this.encomendaService = encomendaService;
        this.financeiro = financeiro;
        this.oficinas = oficinas;
        this.materiais = materiais;
    }

    @Transactional(readOnly = true)
    public Painel montar() {
        LocalDate hoje = LocalDate.now();
        YearMonth mes = YearMonth.from(hoje);

        long abertos = orcamentos.countByStatusIn(EnumSet.of(StatusOrcamento.NOVO, StatusOrcamento.RESPONDIDO));
        long novos = orcamentos.countByStatusIn(EnumSet.of(StatusOrcamento.NOVO));
        long emProducao = encomendas.countByStatusIn(EnumSet.of(StatusEncomenda.EM_PRODUCAO));
        long entregasSemana = encomendas.countByStatusInAndPrazoEntregaBetween(
                EncomendaService.ATIVAS, hoje, hoje.plusDays(7));

        List<EncomendaResp> aReceber = financeiro.contasAReceber();
        BigDecimal totalAReceber = aReceber.stream().map(EncomendaResp::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);

        long pix = oficinas.pixAConferir().size();

        List<Lancamento> doMes = lancamentos.findByTipoAndDataBetween(TipoLancamento.ENTRADA,
                mes.atDay(1), mes.atEndOfMonth());

        List<Barra> semanas = new ArrayList<>();
        int[][] faixas = {{1, 7}, {8, 14}, {15, 21}, {22, mes.lengthOfMonth()}};
        for (int s = 0; s < faixas.length; s++) {
            int de = faixas[s][0], ate = faixas[s][1];
            BigDecimal v = doMes.stream()
                    .filter(l -> l.getData().getDayOfMonth() >= de && l.getData().getDayOfMonth() <= ate)
                    .map(Lancamento::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
            semanas.add(new Barra("Sem " + (s + 1), v));
        }

        Map<CategoriaLancamento, BigDecimal> porFrente = new EnumMap<>(CategoriaLancamento.class);
        doMes.forEach(l -> porFrente.merge(l.getCategoria(), l.getValor(), BigDecimal::add));
        BigDecimal totalMes = doMes.stream().map(Lancamento::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<EncomendaResp> proximas = encomendas.findByStatusInOrderByPrazoEntregaAsc(EncomendaService.ATIVAS).stream()
                .filter(e -> e.getPrazoEntrega() != null)
                .limit(5).map(encomendaService::paraResp).toList();

        String nomeMes = mes.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        nomeMes = nomeMes.substring(0, 1).toUpperCase(PT_BR) + nomeMes.substring(1);

        return new Painel(saudacao(), abertos, novos, emProducao, entregasSemana, totalAReceber, aReceber.size(), pix,
                oficinas.proxima().orElse(null), semanas, FinanceiroService.percentuais(porFrente, totalMes),
                proximas, materiais.abaixoDoMinimo(), nomeMes);
    }

    private String saudacao() {
        int h = LocalTime.now().getHour();
        return h < 12 ? "Bom dia" : h < 18 ? "Boa tarde" : "Boa noite";
    }
}
