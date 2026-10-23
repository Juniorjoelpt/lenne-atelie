package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Cliente;
import br.com.mssolucoes.atelie.dominio.Encomenda;
import br.com.mssolucoes.atelie.dominio.Enums.*;
import br.com.mssolucoes.atelie.dominio.Orcamento;
import br.com.mssolucoes.atelie.dto.Dtos.*;
import br.com.mssolucoes.atelie.repositorio.EncomendaRepository;
import br.com.mssolucoes.atelie.repositorio.OrcamentoRepository;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

@Service
public class OrcamentoService {

    private final OrcamentoRepository repo;
    private final EncomendaRepository encomendas;
    private final EncomendaService encomendaService;
    private final ClienteService clientes;
    private final br.com.mssolucoes.atelie.repositorio.ProdutoRepository produtos;

    public OrcamentoService(OrcamentoRepository repo, EncomendaRepository encomendas,
                            EncomendaService encomendaService, ClienteService clientes,
                            br.com.mssolucoes.atelie.repositorio.ProdutoRepository produtos) {
        this.produtos = produtos;
        this.repo = repo;
        this.encomendas = encomendas;
        this.encomendaService = encomendaService;
        this.clientes = clientes;
    }

    @Transactional(readOnly = true)
    public List<OrcamentoResp> listar(StatusOrcamento status) {
        List<Orcamento> lista = status == null
                ? repo.findAllByOrderByCriadoEmDesc()
                : repo.findByStatusInOrderByCriadoEmDesc(EnumSet.of(status));
        return lista.stream().map(this::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public List<OrcamentoResp> doCliente(Long clienteId) {
        return repo.findByClienteIdOrderByCriadoEmDesc(clienteId).stream().map(this::paraResp).toList();
    }

    /** Pedido feito pelo formulário do site. */
    @Transactional
    public void receberDoSite(OrcamentoPublicoReq r) {
        if (Texto.limpar(r.site()) != null) {
            return; // robô preencheu o campo escondido: ignora em silêncio
        }
        if (r.dataDesejada() != null && r.dataDesejada().isBefore(LocalDate.now())) {
            throw new RegraNegocioException("A data desejada já passou.");
        }
        Orcamento o = new Orcamento();
        o.setNome(r.nome().trim());
        o.setWhatsapp(Texto.whatsapp(r.whatsapp()));
        o.setTipoPeca(r.tipoPeca().trim());
        o.setMedidas(Texto.limpar(r.medidas()));
        o.setCores(Texto.limpar(r.cores()));
        o.setDataDesejada(r.dataDesejada());
        o.setDetalhes(Texto.limpar(r.detalhes()));
        o.setOrigem(Origem.SITE);
        o.setStatus(StatusOrcamento.NOVO);
        repo.save(o);
    }

    @Transactional
    public OrcamentoResp criar(OrcamentoReq r) {
        Orcamento o = new Orcamento();
        preencher(o, r);
        o.setOrigem(Origem.MANUAL);
        return paraResp(repo.save(o));
    }

    @Transactional
    public OrcamentoResp atualizar(Long id, OrcamentoReq r) {
        Orcamento o = buscar(id);
        preencher(o, r);
        return paraResp(repo.save(o));
    }

    @Transactional
    public OrcamentoResp mudarStatus(Long id, StatusOrcamento status) {
        Orcamento o = buscar(id);
        if (status == StatusOrcamento.APROVADO) {
            throw new RegraNegocioException("Use \"Aprovar e criar encomenda\" para aprovar.");
        }
        if (o.getStatus() == StatusOrcamento.APROVADO) {
            throw new RegraNegocioException("Este orçamento já virou encomenda.");
        }
        o.setStatus(status);
        return paraResp(repo.save(o));
    }

    /** Aprova o orçamento: cadastra (ou reaproveita) a cliente e cria a encomenda. */
    @Transactional
    public EncomendaResp aprovar(Long id, AprovarOrcamentoReq r) {
        Orcamento o = buscar(id);
        if (o.getStatus() == StatusOrcamento.APROVADO || encomendas.existsByOrcamentoId(id)) {
            throw new RegraNegocioException("Este orçamento já virou encomenda.");
        }
        BigDecimal sinal = r.valorSinal() == null ? BigDecimal.ZERO : r.valorSinal();
        if (sinal.compareTo(r.valorTotal()) > 0) {
            throw new RegraNegocioException("O sinal não pode ser maior que o valor total.");
        }
        Cliente c = o.getCliente() != null ? o.getCliente()
                : clientes.encontrarOuCriar(o.getNome(), o.getWhatsapp(),
                r.frente() == Frente.CASAMENTO ? TipoCliente.NOIVA : TipoCliente.CLIENTE);
        o.setCliente(c);
        o.setStatus(StatusOrcamento.APROVADO);
        if (o.getValorProposto() == null) {
            o.setValorProposto(r.valorTotal());
        }
        repo.save(o);

        Encomenda e = new Encomenda();
        e.setCliente(c);
        e.setOrcamento(o);
        e.setDescricao(r.descricao().trim());
        e.setFrente(r.frente());
        if (r.produtoId() != null) {
            e.setProduto(produtos.findById(r.produtoId()).orElseThrow(() -> new NaoEncontradoException("Peça")));
        }
        e.setQuantidade(r.quantidade() == null ? 1 : r.quantidade());
        e.setValorTotal(r.valorTotal());
        e.setValorSinal(sinal);
        e.setPrazoEntrega(r.prazoEntrega() != null ? r.prazoEntrega() : o.getDataDesejada());
        e.setStatus(sinal.signum() == 0 ? StatusEncomenda.EM_PRODUCAO : StatusEncomenda.AGUARDANDO_SINAL);
        e.setObservacoes(o.getDetalhes());
        return encomendaService.salvarEProcessar(e);
    }

    @Transactional
    public void excluir(Long id) {
        Orcamento o = buscar(id);
        if (encomendas.existsByOrcamentoId(id)) {
            throw new RegraNegocioException("Este orçamento já virou encomenda e não pode ser excluído.");
        }
        repo.delete(o);
    }

    private Orcamento buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Orçamento"));
    }

    private void preencher(Orcamento o, OrcamentoReq r) {
        o.setCliente(r.clienteId() == null ? null : clientes.buscar(r.clienteId()));
        o.setNome(r.nome().trim());
        o.setWhatsapp(Texto.whatsapp(r.whatsapp()));
        o.setTipoPeca(r.tipoPeca().trim());
        o.setMedidas(Texto.limpar(r.medidas()));
        o.setCores(Texto.limpar(r.cores()));
        o.setDataDesejada(r.dataDesejada());
        o.setDetalhes(Texto.limpar(r.detalhes()));
        o.setValorProposto(r.valorProposto());
    }

    private OrcamentoResp paraResp(Orcamento o) {
        Long encomendaId = null;
        if (o.getStatus() == StatusOrcamento.APROVADO) {
            encomendaId = encomendas.findFirstByOrcamentoId(o.getId()).map(Encomenda::getId).orElse(null);
        }
        return new OrcamentoResp(o.getId(), o.getCliente() == null ? null : o.getCliente().getId(), o.getNome(),
                o.getWhatsapp(), o.getTipoPeca(), o.getMedidas(), o.getCores(), o.getDataDesejada(), o.getDetalhes(),
                o.getValorProposto(), o.getStatus(), o.getOrigem(), o.getCriadoEm(), encomendaId);
    }
}
