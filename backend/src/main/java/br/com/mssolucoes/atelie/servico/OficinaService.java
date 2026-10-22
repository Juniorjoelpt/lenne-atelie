package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Configuracao;
import br.com.mssolucoes.atelie.dominio.Enums.*;
import br.com.mssolucoes.atelie.dominio.Inscricao;
import br.com.mssolucoes.atelie.dominio.Lancamento;
import br.com.mssolucoes.atelie.dominio.Oficina;
import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.repositorio.InscricaoRepository;
import br.com.mssolucoes.atelie.repositorio.LancamentoRepository;
import br.com.mssolucoes.atelie.repositorio.OficinaRepository;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class OficinaService {

    /** Inscrições que ocupam vaga. */
    public static final Set<StatusInscricao> OCUPAM_VAGA =
            EnumSet.of(StatusInscricao.AGUARDANDO_PIX, StatusInscricao.A_CONFERIR, StatusInscricao.CONFIRMADA);

    private final OficinaRepository repo;
    private final InscricaoRepository inscricoes;
    private final LancamentoRepository lancamentos;
    private final ClienteService clientes;
    private final ConfiguracaoService configuracao;

    public OficinaService(OficinaRepository repo, InscricaoRepository inscricoes, LancamentoRepository lancamentos,
                          ClienteService clientes, ConfiguracaoService configuracao) {
        this.repo = repo;
        this.inscricoes = inscricoes;
        this.lancamentos = lancamentos;
        this.clientes = clientes;
        this.configuracao = configuracao;
    }

    // ---------- Oficinas ----------

    @Transactional(readOnly = true)
    public List<OficinaResp> listar() {
        return repo.findAllByOrderByInicioDesc().stream().map(this::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public OficinaResp detalhar(Long id) {
        return paraResp(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<OficinaPublica> abertasNoSite() {
        return repo.findByPublicadaTrueAndStatusAndInicioAfterOrderByInicioAsc(StatusOficina.ABERTA, LocalDateTime.now())
                .stream().map(o -> {
                    long restantes = vagasRestantes(o);
                    return new OficinaPublica(o.getId(), o.getTitulo(), o.getDescricao(), o.getInicio(), o.getFim(),
                            o.getLocal(), o.getValor(), restantes, restantes <= 0);
                }).toList();
    }

    @Transactional(readOnly = true)
    public Optional<OficinaResp> proxima() {
        return repo.findFirstByStatusAndInicioAfterOrderByInicioAsc(StatusOficina.ABERTA, LocalDateTime.now())
                .map(this::paraResp);
    }

    @Transactional
    public OficinaResp criar(OficinaReq r) {
        Oficina o = new Oficina();
        preencher(o, r);
        return paraResp(repo.save(o));
    }

    @Transactional
    public OficinaResp atualizar(Long id, OficinaReq r) {
        Oficina o = buscar(id);
        preencher(o, r);
        long ocupadas = inscricoes.countByOficinaIdAndStatusIn(id, OCUPAM_VAGA);
        if (o.getVagas() < ocupadas) {
            throw new RegraNegocioException("Já existem " + ocupadas + " inscrições ocupando vaga. Aumente o número de vagas.");
        }
        return paraResp(repo.save(o));
    }

    @Transactional
    public void excluir(Long id) {
        Oficina o = buscar(id);
        if (!inscricoes.findByOficinaIdOrderByCriadoEmAsc(id).isEmpty()) {
            throw new RegraNegocioException("Esta oficina tem inscrições. Cancele em vez de excluir.");
        }
        repo.delete(o);
    }

    // ---------- Inscrições ----------

    @Transactional(readOnly = true)
    public List<InscricaoResp> inscricoes(Long oficinaId) {
        buscar(oficinaId);
        return inscricoes.findByOficinaIdOrderByCriadoEmAsc(oficinaId).stream().map(OficinaService::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public List<InscricaoResp> pixAConferir() {
        return inscricoes.findByStatusInOrderByCriadoEmAsc(EnumSet.of(StatusInscricao.A_CONFERIR, StatusInscricao.AGUARDANDO_PIX))
                .stream().filter(i -> i.getOficina().getStatus() == StatusOficina.ABERTA)
                .map(OficinaService::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public List<InscricaoResp> doCliente(Long clienteId) {
        return inscricoes.findByClienteIdOrderByCriadoEmDesc(clienteId).stream().map(OficinaService::paraResp).toList();
    }

    /** Inscrição feita pelo site: ocupa vaga aguardando o Pix, ou entra na lista de espera. */
    @Transactional
    public InscricaoPublicaResp inscreverPeloSite(Long oficinaId, InscricaoPublicaReq r) {
        if (Texto.limpar(r.site()) != null) {
            throw new RegraNegocioException("Não foi possível concluir a inscrição.");
        }
        Oficina o = buscar(oficinaId);
        if (!o.isPublicada() || o.getStatus() != StatusOficina.ABERTA || o.getInicio().isBefore(LocalDateTime.now())) {
            throw new RegraNegocioException("As inscrições para esta oficina estão encerradas.");
        }
        String fone = Texto.whatsapp(r.whatsapp());
        if (inscricoes.existsByOficinaIdAndWhatsappAndStatusNot(oficinaId, fone, StatusInscricao.CANCELADA)) {
            throw new RegraNegocioException("Já existe uma inscrição com este WhatsApp nesta oficina. "
                    + "Fale com a gente pelo WhatsApp se precisar de ajuda.");
        }
        Inscricao i = new Inscricao();
        i.setOficina(o);
        i.setNome(r.nome().trim());
        i.setWhatsapp(fone);
        i.setOrigem(Origem.SITE);
        i.setStatus(vagasRestantes(o) > 0 ? StatusInscricao.AGUARDANDO_PIX : StatusInscricao.LISTA_ESPERA);
        inscricoes.save(i);
        return respostaPublica(i);
    }

    /** A aluna avisa pelo site que enviou o comprovante. */
    @Transactional
    public InscricaoPublicaResp avisarComprovante(String codigo) {
        Inscricao i = inscricoes.findByCodigo(codigo).orElseThrow(() -> new NaoEncontradoException("Inscrição"));
        if (i.getStatus() == StatusInscricao.AGUARDANDO_PIX) {
            i.setStatus(StatusInscricao.A_CONFERIR);
            inscricoes.save(i);
        }
        return respostaPublica(i);
    }

    @Transactional
    public InscricaoResp inscreverManual(Long oficinaId, InscricaoReq r) {
        Oficina o = buscar(oficinaId);
        Inscricao i = new Inscricao();
        i.setOficina(o);
        i.setNome(r.nome().trim());
        i.setWhatsapp(Texto.whatsapp(r.whatsapp()));
        i.setOrigem(Origem.MANUAL);
        StatusInscricao desejado = r.status() == null ? StatusInscricao.AGUARDANDO_PIX : r.status();
        if (OCUPAM_VAGA.contains(desejado) && vagasRestantes(o) <= 0) {
            throw new RegraNegocioException("A oficina está lotada. Coloque na lista de espera ou aumente as vagas.");
        }
        i.setStatus(StatusInscricao.AGUARDANDO_PIX);
        inscricoes.save(i);
        if (desejado != StatusInscricao.AGUARDANDO_PIX) {
            aplicarStatus(i, desejado, FormaPagamento.PIX);
        }
        return paraResp(i);
    }

    @Transactional
    public InscricaoResp mudarStatus(Long inscricaoId, StatusInscricaoReq r) {
        Inscricao i = inscricoes.findById(inscricaoId).orElseThrow(() -> new NaoEncontradoException("Inscrição"));
        aplicarStatus(i, r.status(), r.formaPagamento());
        return paraResp(i);
    }

    private void aplicarStatus(Inscricao i, StatusInscricao novo, FormaPagamento forma) {
        StatusInscricao antigo = i.getStatus();
        if (antigo == novo) {
            return;
        }
        Oficina o = i.getOficina();
        if (OCUPAM_VAGA.contains(novo) && !OCUPAM_VAGA.contains(antigo) && vagasRestantes(o) <= 0) {
            throw new RegraNegocioException("Não há vaga livre nesta oficina.");
        }
        if (antigo == StatusInscricao.CONFIRMADA) {
            lancamentos.deleteByInscricaoId(i.getId());
        }
        if (novo == StatusInscricao.CONFIRMADA) {
            if (i.getCliente() == null) {
                i.setCliente(clientes.encontrarOuCriar(i.getNome(), i.getWhatsapp(), TipoCliente.ALUNA));
            }
            if (o.getValor().signum() > 0 && !lancamentos.existsByInscricaoId(i.getId())) {
                Lancamento l = new Lancamento();
                l.setTipo(TipoLancamento.ENTRADA);
                l.setCategoria(CategoriaLancamento.OFICINA);
                l.setDescricao("Inscrição — " + o.getTitulo() + " (" + i.getNome() + ")");
                l.setValor(o.getValor());
                l.setData(LocalDate.now());
                l.setFormaPagamento(forma == null ? FormaPagamento.PIX : forma);
                l.setInscricao(i);
                lancamentos.save(l);
            }
        }
        i.setStatus(novo);
        inscricoes.save(i);
    }

    // ---------- Apoio ----------

    private long vagasRestantes(Oficina o) {
        if (o.getId() == null) {
            return o.getVagas();
        }
        return Math.max(0, o.getVagas() - inscricoes.countByOficinaIdAndStatusIn(o.getId(), OCUPAM_VAGA));
    }

    private Oficina buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Oficina"));
    }

    private void preencher(Oficina o, OficinaReq r) {
        if (r.fim() != null && r.fim().isBefore(r.inicio())) {
            throw new RegraNegocioException("O horário de término é antes do início.");
        }
        o.setTitulo(r.titulo().trim());
        o.setDescricao(Texto.limpar(r.descricao()));
        o.setInicio(r.inicio());
        o.setFim(r.fim());
        o.setLocal(Texto.limpar(r.local()));
        o.setVagas(r.vagas());
        o.setValor(r.valor());
        o.setStatus(r.status() == null ? StatusOficina.ABERTA : r.status());
        o.setPublicada(r.publicada() == null || r.publicada());
    }

    private InscricaoPublicaResp respostaPublica(Inscricao i) {
        Configuracao c = configuracao.atual();
        Oficina o = i.getOficina();
        return new InscricaoPublicaResp(i.getCodigo(), i.getStatus(), o.getTitulo(), o.getInicio(), o.getValor(),
                c.getChavePix(), c.getTitularPix(), c.getWhatsapp());
    }

    public OficinaResp paraResp(Oficina o) {
        Long id = o.getId();
        long confirmadas = inscricoes.countByOficinaIdAndStatus(id, StatusInscricao.CONFIRMADA);
        long aConferir = inscricoes.countByOficinaIdAndStatus(id, StatusInscricao.A_CONFERIR);
        long aguardando = inscricoes.countByOficinaIdAndStatus(id, StatusInscricao.AGUARDANDO_PIX);
        long espera = inscricoes.countByOficinaIdAndStatus(id, StatusInscricao.LISTA_ESPERA);
        long restantes = Math.max(0, o.getVagas() - confirmadas - aConferir - aguardando);
        return new OficinaResp(id, o.getTitulo(), o.getDescricao(), o.getInicio(), o.getFim(), o.getLocal(),
                o.getVagas(), o.getValor(), o.getStatus(), o.isPublicada(), confirmadas, aConferir, aguardando,
                espera, restantes);
    }

    public static InscricaoResp paraResp(Inscricao i) {
        Oficina o = i.getOficina();
        return new InscricaoResp(i.getId(), o.getId(), o.getTitulo(), o.getInicio(),
                i.getCliente() == null ? null : i.getCliente().getId(), i.getNome(), i.getWhatsapp(),
                i.getStatus(), i.getOrigem(), i.getCriadoEm());
    }
}
