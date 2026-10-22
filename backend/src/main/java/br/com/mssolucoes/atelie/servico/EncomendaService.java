package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Encomenda;
import br.com.mssolucoes.atelie.dominio.Enums.*;
import br.com.mssolucoes.atelie.dominio.Lancamento;
import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.repositorio.EncomendaRepository;
import br.com.mssolucoes.atelie.repositorio.LancamentoRepository;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class EncomendaService {

    /** Encomendas que ainda não foram entregues nem canceladas. */
    public static final Set<StatusEncomenda> ATIVAS =
            EnumSet.of(StatusEncomenda.AGUARDANDO_SINAL, StatusEncomenda.EM_PRODUCAO, StatusEncomenda.PRONTA);

    private final EncomendaRepository repo;
    private final LancamentoRepository lancamentos;
    private final ClienteService clientes;

    public EncomendaService(EncomendaRepository repo, LancamentoRepository lancamentos, ClienteService clientes) {
        this.repo = repo;
        this.lancamentos = lancamentos;
        this.clientes = clientes;
    }

    @Transactional(readOnly = true)
    public List<EncomendaResp> listar(StatusEncomenda status) {
        List<Encomenda> lista = status == null
                ? repo.findAllByOrderByCriadoEmDesc()
                : repo.findByStatusInOrderByPrazoEntregaAsc(EnumSet.of(status));
        return lista.stream().map(this::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public List<EncomendaResp> doCliente(Long clienteId) {
        return repo.findByClienteIdOrderByCriadoEmDesc(clienteId).stream().map(this::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public EncomendaResp detalhar(Long id) {
        return paraResp(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<LancamentoResp> pagamentos(Long id) {
        buscar(id);
        return lancamentos.findByEncomendaIdOrderByDataAsc(id).stream().map(FinanceiroService::paraResp).toList();
    }

    @Transactional
    public EncomendaResp criar(EncomendaReq r) {
        Encomenda e = new Encomenda();
        preencher(e, r);
        if (r.status() != null) {
            e.setStatus(r.status());
        }
        return paraResp(repo.save(e));
    }

    @Transactional
    public EncomendaResp atualizar(Long id, EncomendaReq r) {
        Encomenda e = buscar(id);
        preencher(e, r);
        if (r.status() != null) {
            e.setStatus(r.status());
        }
        return paraResp(repo.save(e));
    }

    @Transactional
    public EncomendaResp mudarStatus(Long id, StatusEncomenda status) {
        Encomenda e = buscar(id);
        e.setStatus(status);
        return paraResp(repo.save(e));
    }

    @Transactional
    public EncomendaResp registrarPagamento(Long id, PagamentoReq r) {
        Encomenda e = buscar(id);
        if (e.getStatus() == StatusEncomenda.CANCELADA) {
            throw new RegraNegocioException("Esta encomenda está cancelada.");
        }
        BigDecimal pago = lancamentos.totalPagoEncomenda(id);
        BigDecimal saldo = e.getValorTotal().subtract(pago);
        if (r.valor().compareTo(saldo) > 0) {
            throw new RegraNegocioException("O valor é maior que o saldo em aberto (R$ " + saldo.setScale(2) + ").");
        }
        Lancamento l = new Lancamento();
        l.setTipo(TipoLancamento.ENTRADA);
        l.setCategoria(categoria(e.getFrente()));
        boolean primeiro = pago.signum() == 0;
        String desc = Texto.limpar(r.descricao());
        l.setDescricao(desc != null ? desc
                : (primeiro && r.valor().compareTo(e.getValorTotal()) < 0 ? "Sinal" : "Pagamento")
                + " — " + e.getDescricao() + " (" + e.getCliente().getNome() + ")");
        l.setValor(r.valor());
        l.setData(r.data() == null ? LocalDate.now() : r.data());
        l.setFormaPagamento(r.formaPagamento() == null ? FormaPagamento.PIX : r.formaPagamento());
        l.setEncomenda(e);
        lancamentos.save(l);

        BigDecimal novoPago = pago.add(r.valor());
        if (e.getStatus() == StatusEncomenda.AGUARDANDO_SINAL && novoPago.compareTo(e.getValorSinal()) >= 0) {
            e.setStatus(StatusEncomenda.EM_PRODUCAO);
            repo.save(e);
        }
        return paraResp(e);
    }

    @Transactional
    public void excluir(Long id) {
        Encomenda e = buscar(id);
        if (!lancamentos.findByEncomendaIdOrderByDataAsc(id).isEmpty()) {
            throw new RegraNegocioException("Esta encomenda tem pagamentos registrados. Cancele em vez de excluir.");
        }
        repo.delete(e);
    }

    Encomenda buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Encomenda"));
    }

    private void preencher(Encomenda e, EncomendaReq r) {
        BigDecimal sinal = r.valorSinal() == null ? BigDecimal.ZERO : r.valorSinal();
        if (sinal.compareTo(r.valorTotal()) > 0) {
            throw new RegraNegocioException("O sinal não pode ser maior que o valor total.");
        }
        e.setCliente(clientes.buscar(r.clienteId()));
        e.setDescricao(r.descricao().trim());
        e.setFrente(r.frente());
        e.setValorTotal(r.valorTotal());
        e.setValorSinal(sinal);
        e.setPrazoEntrega(r.prazoEntrega());
        e.setObservacoes(Texto.limpar(r.observacoes()));
    }

    public static CategoriaLancamento categoria(Frente f) {
        return switch (f) {
            case CASAMENTO -> CategoriaLancamento.CASAMENTO;
            case PRESENTE -> CategoriaLancamento.PRESENTE;
            default -> CategoriaLancamento.ENCOMENDA;
        };
    }

    public EncomendaResp paraResp(Encomenda e) {
        BigDecimal pago = e.getId() == null ? BigDecimal.ZERO : lancamentos.totalPagoEncomenda(e.getId());
        BigDecimal saldo = e.getStatus() == StatusEncomenda.CANCELADA
                ? BigDecimal.ZERO : e.getValorTotal().subtract(pago).max(BigDecimal.ZERO);
        var c = e.getCliente();
        return new EncomendaResp(e.getId(), c.getId(), c.getNome(), c.getWhatsapp(),
                e.getOrcamento() == null ? null : e.getOrcamento().getId(),
                e.getDescricao(), e.getFrente(), e.getValorTotal(), e.getValorSinal(), pago, saldo,
                e.getPrazoEntrega(), e.getStatus(), e.getObservacoes(), e.getCriadoEm());
    }
}
