package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Enums.CategoriaLancamento;
import br.com.mssolucoes.atelie.dominio.Enums.FormaPagamento;
import br.com.mssolucoes.atelie.dominio.Enums.TipoLancamento;
import br.com.mssolucoes.atelie.dominio.Lancamento;
import br.com.mssolucoes.atelie.dominio.Material;
import br.com.mssolucoes.atelie.dto.Dtos.AjusteEstoqueReq;
import br.com.mssolucoes.atelie.dto.Dtos.MaterialReq;
import br.com.mssolucoes.atelie.dto.Dtos.MaterialResp;
import br.com.mssolucoes.atelie.repositorio.LancamentoRepository;
import br.com.mssolucoes.atelie.repositorio.MaterialRepository;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class MaterialService {

    private final MaterialRepository repo;
    private final LancamentoRepository lancamentos;

    public MaterialService(MaterialRepository repo, LancamentoRepository lancamentos) {
        this.repo = repo;
        this.lancamentos = lancamentos;
    }

    @Transactional(readOnly = true)
    public List<MaterialResp> listar() {
        return repo.findAllByOrderByNomeAsc().stream().map(MaterialService::paraResp).toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialResp> abaixoDoMinimo() {
        return repo.findAllByOrderByNomeAsc().stream().filter(Material::isAbaixoDoMinimo)
                .map(MaterialService::paraResp).toList();
    }

    @Transactional
    public MaterialResp criar(MaterialReq r) {
        Material m = new Material();
        preencher(m, r);
        return paraResp(repo.save(m));
    }

    @Transactional
    public MaterialResp atualizar(Long id, MaterialReq r) {
        Material m = buscar(id);
        preencher(m, r);
        return paraResp(repo.save(m));
    }

    @Transactional
    public MaterialResp ajustar(Long id, AjusteEstoqueReq r) {
        Material m = buscar(id);
        if (r.tipo() == TipoLancamento.SAIDA) {
            if (r.quantidade().compareTo(m.getQuantidade()) > 0) {
                throw new RegraNegocioException("Só há " + m.getQuantidade().stripTrailingZeros().toPlainString()
                        + " " + m.getUnidade() + " em estoque.");
            }
            m.setQuantidade(m.getQuantidade().subtract(r.quantidade()));
        } else {
            m.setQuantidade(m.getQuantidade().add(r.quantidade()));
            BigDecimal valor = r.valorCompra() != null && r.valorCompra().signum() > 0
                    ? r.valorCompra() : m.getCustoUnitario().multiply(r.quantidade());
            if (r.registrarDespesa() && valor.signum() > 0) {
                Lancamento l = new Lancamento();
                l.setTipo(TipoLancamento.SAIDA);
                l.setCategoria(CategoriaLancamento.MATERIAL);
                l.setDescricao("Compra — " + m.getNome() + " ("
                        + r.quantidade().stripTrailingZeros().toPlainString() + " " + m.getUnidade() + ")");
                l.setValor(valor);
                l.setData(LocalDate.now());
                l.setFormaPagamento(FormaPagamento.PIX);
                lancamentos.save(l);
            }
        }
        return paraResp(repo.save(m));
    }

    @Transactional
    public void excluir(Long id) {
        repo.delete(buscar(id));
    }

    private Material buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new NaoEncontradoException("Material"));
    }

    private void preencher(Material m, MaterialReq r) {
        m.setNome(r.nome().trim());
        m.setUnidade(r.unidade().trim());
        m.setQuantidade(r.quantidade());
        m.setEstoqueMinimo(r.estoqueMinimo());
        m.setCustoUnitario(r.custoUnitario());
    }

    public static MaterialResp paraResp(Material m) {
        return new MaterialResp(m.getId(), m.getNome(), m.getUnidade(), m.getQuantidade(), m.getEstoqueMinimo(),
                m.getCustoUnitario(), m.isAbaixoDoMinimo());
    }
}
