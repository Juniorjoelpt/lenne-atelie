import { useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, Modal, ModalForm, Selo, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { FORMAS, FRENTES, STATUS_ENCOMENDA, data, hojeIso, linkWhats, moeda, moedaUnit, qtd, qtdUn, telefone } from '../util.js';
import CamposFicha from '../componentes/CamposFicha.jsx';
import { Topo } from './Layout.jsx';
import { Info } from './Orcamentos.jsx';

const FILTROS = [['', 'Todas'], ['AGUARDANDO_SINAL', 'Aguardando sinal'], ['EM_PRODUCAO', 'Em produção'], ['PRONTA', 'Prontas'], ['ENTREGUE', 'Entregues'], ['CANCELADA', 'Canceladas']];
const PROXIMO = { AGUARDANDO_SINAL: 'EM_PRODUCAO', EM_PRODUCAO: 'PRONTA', PRONTA: 'ENTREGUE' };

export default function Encomendas() {
  const [filtro, setFiltro] = useState('');
  const [aberta, setAberta] = useState(null);
  const [form, setForm] = useState(null);
  const toast = useToast();
  const { dados, erro, carregando, recarregar } = useCarregar(
    () => api.get(`/api/encomendas${filtro ? `?status=${filtro}` : ''}`), [filtro]);

  return (
    <>
      <Topo titulo="Encomendas">
        <button type="button" className="btn btn-terra" onClick={() => setForm({})}><Icone nome="mais" tamanho={18} largura={3} />Nova encomenda</button>
      </Topo>
      <div className="chips" role="group" aria-label="Filtrar por status" style={{ alignSelf: 'flex-start' }}>
        {FILTROS.map(([v, r]) => <button key={v} type="button" className="chip" aria-pressed={filtro === v} onClick={() => setFiltro(v)}>{r}</button>)}
      </div>
      <section className="cartao">
        <Estado carregando={carregando} erro={erro}>
          {dados?.length === 0 ? <Vazio>Nenhuma encomenda {filtro ? 'neste status' : 'ainda'}. Aprove um orçamento ou crie uma nova.</Vazio> : (
            <div className="tabela-caixa">
              <table className="tabela">
                <thead><tr><th>Encomenda</th><th>Cliente</th><th>Entrega</th><th className="num">Total</th><th className="num">Falta receber</th><th>Status</th></tr></thead>
                <tbody>
                  {dados?.map((e) => (
                    <tr key={e.id} className="clicavel" tabIndex={0} onClick={() => setAberta(e)} onKeyDown={(k) => k.key === 'Enter' && setAberta(e)}>
                      <td><strong>{e.descricao}</strong><br /><span className="mudo" style={{ fontSize: 14 }}>{FRENTES[e.frente]}{e.produtoNome ? ` · ${e.quantidade}× ${e.produtoNome}` : ''}</span></td>
                      <td>{e.clienteNome}</td>
                      <td>{data(e.prazoEntrega)}</td>
                      <td className="num">{moeda(e.valorTotal)}</td>
                      <td className="num" style={{ fontWeight: 800, color: Number(e.saldo) > 0 ? 'var(--terra-texto)' : 'var(--oliva-texto)' }}>{Number(e.saldo) > 0 ? moeda(e.saldo) : 'Quitada'}</td>
                      <td><Selo mapa={STATUS_ENCOMENDA} valor={e.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Estado>
      </section>

      {aberta && <Detalhe id={aberta.id} aoFechar={() => { setAberta(null); recarregar(); }}
        aoEditar={(e) => { setAberta(null); setForm(e); }} />}

      {form && <FormEncomenda encomenda={form} aoFechar={() => setForm(null)} aoSalvar={() => { setForm(null); toast('Encomenda salva.'); recarregar(); }} />}
    </>
  );
}

function FormEncomenda({ encomenda: e, aoFechar, aoSalvar }) {
  const { dados: clientes } = useCarregar(() => api.get('/api/clientes'));
  if (!clientes) return null;
  if (clientes.length === 0) {
    return (
      <Modal titulo="Nova encomenda" aoFechar={aoFechar}>
        <p>Cadastre a cliente primeiro em <strong>Clientes</strong>, ou crie a encomenda aprovando um orçamento.</p>
      </Modal>
    );
  }
  return (
    <ModalForm titulo={e.id ? 'Editar encomenda' : 'Nova encomenda'} aoFechar={aoFechar} aoSalvar={async (fd) => {
      const d = lerForm(fd, ['valorTotal', 'valorSinal', 'clienteId', 'produtoId', 'quantidade', 'horasTrabalhadas']);
      if (e.id) await api.put(`/api/encomendas/${e.id}`, d);
      else await api.post('/api/encomendas', d);
      aoSalvar();
    }}>
      <Campo rotulo="Cliente" nome="clienteId" defaultValue={e.clienteId} inteiro
        opcoes={Object.fromEntries(clientes.map((c) => [c.id, `${c.nome}${c.whatsapp ? ` · ${telefone(c.whatsapp)}` : ''}`]))} />
      <Campo rotulo="Descrição" nome="descricao" defaultValue={e.descricao} required maxLength={200} inteiro />
      <Campo rotulo="Frente" nome="frente" opcoes={FRENTES} defaultValue={e.frente || 'ENCOMENDA'} />
      <Campo rotulo="Status" nome="status" opcoes={Object.fromEntries(Object.entries(STATUS_ENCOMENDA).map(([k, [r]]) => [k, r]))} defaultValue={e.status || 'AGUARDANDO_SINAL'} />
      <CamposFicha produtoId={e.produtoId} quantidade={e.quantidade} valorTotal={e.valorTotal} travado={e.materiaisBaixados && !!e.produtoId} />
      <Campo rotulo="Sinal (R$)" nome="valorSinal" tipo="number" min="0" step="0.01" defaultValue={e.valorSinal ?? 0} />
      <Campo rotulo="Horas trabalhadas" nome="horasTrabalhadas" tipo="number" min="0" step="0.25" defaultValue={e.horasTrabalhadas}
        dica="Deixe vazio para usar as horas da ficha." />
      <Campo rotulo="Prazo de entrega" nome="prazoEntrega" tipo="date" defaultValue={e.prazoEntrega} />
      <Campo rotulo="Observações" nome="observacoes" linhas={3} defaultValue={e.observacoes} inteiro />
    </ModalForm>
  );
}

function Detalhe({ id, aoFechar, aoEditar }) {
  const { dados: e, erro, carregando, recarregar } = useCarregar(() => api.get(`/api/encomendas/${id}`));
  const { dados: pagamentos, recarregar: recarregarPag } = useCarregar(() => api.get(`/api/encomendas/${id}/pagamentos`));
  const [pagar, setPagar] = useState(false);
  const [confirmar, setConfirmar] = useState(null);
  const [falha, setFalha] = useState(null);
  const toast = useToast();

  async function mudar(status) {
    try {
      await api.patch(`/api/encomendas/${id}/status`, { status });
      toast(`Status: ${STATUS_ENCOMENDA[status][0]}.`);
      setConfirmar(null);
      recarregar();
    } catch (err) { setFalha(err.message); }
  }

  if (pagar && e) {
    return (
      <ModalForm titulo="Registrar pagamento" textoSalvar="Registrar" aoFechar={() => setPagar(false)} aoSalvar={async (fd) => {
        await api.post(`/api/encomendas/${id}/pagamentos`, lerForm(fd, ['valor']));
        toast('Pagamento registrado.');
        setPagar(false);
        recarregar();
        recarregarPag();
      }}>
        <div className="inteiro aviso aviso-info">Falta receber {moeda(e.saldo)} de {e.clienteNome}.</div>
        <Campo rotulo="Valor (R$)" nome="valor" tipo="number" min="0.01" step="0.01" required
          defaultValue={Number(e.valorPago) === 0 && Number(e.valorSinal) > 0 ? e.valorSinal : e.saldo} />
        <Campo rotulo="Data" nome="data" tipo="date" defaultValue={hojeIso()} />
        <Campo rotulo="Forma" nome="formaPagamento" opcoes={FORMAS} defaultValue="PIX" />
        <Campo rotulo="Descrição (opcional)" nome="descricao" maxLength={200} placeholder="Sinal, saldo final…" />
      </ModalForm>
    );
  }
  if (confirmar) {
    return <Confirmar titulo={confirmar === 'EXCLUIR' ? 'Excluir encomenda' : 'Cancelar encomenda'} perigo
      textoBotao={confirmar === 'EXCLUIR' ? 'Excluir' : 'Cancelar encomenda'}
      texto={confirmar === 'EXCLUIR' ? 'Excluir esta encomenda? Isso não pode ser desfeito.' : 'A encomenda sai das entregas e do "a receber". Os pagamentos já registrados continuam no financeiro.'}
      aoFechar={() => setConfirmar(null)}
      aoConfirmar={async () => {
        if (confirmar === 'EXCLUIR') { await api.del(`/api/encomendas/${id}`); toast('Encomenda excluída.'); aoFechar(); }
        else await mudar('CANCELADA');
      }} />;
  }

  const proximo = e && PROXIMO[e.status];
  return (
    <Modal titulo={e ? e.descricao : 'Encomenda'} aoFechar={aoFechar} largo rodape={e && <>
      {e.status !== 'CANCELADA' && e.status !== 'ENTREGUE' && <button type="button" className="btn btn-perigo" onClick={() => setConfirmar(Number(e.valorPago) > 0 ? 'CANCELAR' : 'EXCLUIR')}>{Number(e.valorPago) > 0 ? 'Cancelar' : 'Excluir'}</button>}
      <button type="button" className="btn btn-claro" onClick={() => aoEditar(e)}><Icone nome="editar" tamanho={18} />Editar</button>
      {Number(e.saldo) > 0 && <button type="button" className="btn btn-claro" onClick={() => setPagar(true)}><Icone nome="dinheiro" tamanho={18} />Registrar pagamento</button>}
      {proximo && <button type="button" className="btn btn-terra" onClick={() => mudar(proximo)}>Marcar como {STATUS_ENCOMENDA[proximo][0].toLowerCase()}</button>}
    </>}>
      <Estado carregando={carregando} erro={erro}>
        {e && (
          <>
            {falha && <div className="aviso aviso-erro" role="alert" style={{ marginBottom: 12 }}>{falha}</div>}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center', marginBottom: 18 }}>
              <Selo mapa={STATUS_ENCOMENDA} valor={e.status} /><span className="selo selo-areia">{FRENTES[e.frente]}</span>
            </div>
            <div className="grade-resumo" style={{ marginBottom: 20 }}>
              {[['Total', e.valorTotal, 'var(--areia)'], ['Recebido', e.valorPago, 'var(--oliva-claro)'], ['Falta receber', e.saldo, 'var(--terra-claro)']].map(([r, v, f]) => (
                <div key={r} className="clay-sm" style={{ background: f, borderRadius: 22, padding: 16 }}>
                  <p className="forte" style={{ fontSize: 14 }}>{r}</p><p style={{ fontSize: 24, fontWeight: 900 }}>{moeda(v)}</p>
                </div>
              ))}
            </div>
            <div className="form-grade">
              <Info rotulo="Cliente" valor={e.clienteNome} />
              <Info rotulo="WhatsApp" valor={e.clienteWhatsapp ? <a href={linkWhats(e.clienteWhatsapp, `Olá, ${e.clienteNome.split(' ')[0]}! Aqui é a Lenne, do ateliê.`)} target="_blank" rel="noopener noreferrer">{telefone(e.clienteWhatsapp)}</a> : '—'} />
              <Info rotulo="Prazo de entrega" valor={data(e.prazoEntrega)} />
              <Info rotulo="Sinal combinado" valor={moeda(e.valorSinal)} />
              {e.observacoes && <div className="inteiro"><Info rotulo="Observações" valor={e.observacoes} /></div>}
            </div>
            <Custos e={e} />
            <MateriaisUsados e={e} aoMudar={() => { recarregar(); }} />
            <h3 style={{ fontSize: 18, margin: '24px 0 8px' }}>Pagamentos</h3>
            {!pagamentos?.length ? <p className="mudo forte">Nenhum pagamento registrado.</p> : (
              <ul className="lista-linhas">
                {pagamentos.map((p) => (
                  <li key={p.id}><div className="info"><p>{moeda(p.valor)} · {FORMAS[p.formaPagamento]}</p><p>{data(p.data)} · {p.descricao}</p></div></li>
                ))}
              </ul>
            )}
          </>
        )}
      </Estado>
    </Modal>
  );
}

function Custos({ e }) {
  const c = e.custos;
  const meta = Number(c.valorHoraDesejado);
  const porHora = c.ganhoPorHora != null ? Number(c.ganhoPorHora) : null;
  const bateu = porHora != null && meta > 0 && porHora >= meta;
  return (
    <section aria-labelledby={`custos-${e.id}`} style={{ marginTop: 24 }}>
      <h3 id={`custos-${e.id}`} style={{ fontSize: 18, marginBottom: 10 }}>
        Custos e lucro {c.estimado && <span className="selo selo-ocre" style={{ marginLeft: 6 }}>materiais estimados pela ficha</span>}
      </h3>
      <div className="custos-grade">
        <div><p>Valor cobrado</p><p>{moeda(e.valorTotal)}</p></div>
        <div><p>Materiais</p><p>{moeda(c.custoMateriais)}</p></div>
        <div><p>Custos fixos ({qtd(c.horas)} h)</p><p>{moeda(c.custoFixo)}</p></div>
        {Number(c.outrosCustos) > 0 && <div><p>Embalagem e outros</p><p>{moeda(c.outrosCustos)}</p></div>}
        <div style={{ background: 'var(--oliva-claro)' }}><p>Seu ganho</p><p>{moeda(c.ganho)}</p></div>
        <div style={{ background: porHora == null ? 'var(--cru)' : bateu ? 'var(--oliva-claro)' : 'var(--terra-claro)' }}>
          <p>Por hora trabalhada</p>
          <p>{porHora == null ? '—' : moeda(porHora)}</p>
        </div>
      </div>
      <p className="mudo forte" style={{ fontSize: 14, marginTop: 8 }}>
        {porHora == null ? 'Informe as horas trabalhadas (ou use uma ficha) para ver quanto rendeu sua hora.'
          : meta > 0 ? (bateu
            ? `Acima da sua meta de ${moeda(meta)}/h: sobra um lucro de ${moeda(c.lucro)} além do seu pagamento.`
            : `Abaixo da sua meta de ${moeda(meta)}/h: faltaram ${moeda(-c.lucro)} para pagar todas as suas horas.`)
          : 'Configure sua meta de valor da hora em Configurações.'}
      </p>
    </section>
  );
}

function MateriaisUsados({ e, aoMudar }) {
  const lista = useCarregar(() => api.get(`/api/encomendas/${e.id}/consumos`), [e.id, e.materiaisBaixados]);
  const materiais = useCarregar(() => api.get('/api/materiais'));
  const [novo, setNovo] = useState(null);
  const [erro, setErro] = useState(null);
  const toast = useToast();
  const cancelada = e.status === 'CANCELADA';

  async function acao(fn, msg) {
    setErro(null);
    try { await fn(); toast(msg); lista.recarregar(); aoMudar(); } catch (err) { setErro(err.message); }
  }

  return (
    <section aria-labelledby={`mat-${e.id}`} style={{ marginTop: 24 }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
        <h3 id={`mat-${e.id}`} style={{ fontSize: 18 }}>Materiais usados</h3>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {e.produtoId && !e.materiaisBaixados && !cancelada && (
            <button type="button" className="btn btn-oliva btn-p"
              onClick={() => acao(() => api.post(`/api/encomendas/${e.id}/baixar-ficha`), 'Materiais da ficha baixados do estoque.')}>
              Baixar materiais da ficha agora
            </button>
          )}
          {!cancelada && !novo && <button type="button" className="btn btn-claro btn-p" onClick={() => setNovo({ materialId: '', quantidade: '' })}><Icone nome="mais" tamanho={16} largura={3} />Material extra</button>}
        </div>
      </div>
      {e.produtoId && !e.materiaisBaixados && (
        <p className="mudo forte" style={{ fontSize: 14 }}>Os materiais da ficha saem do estoque sozinhos quando a encomenda entra em produção.</p>
      )}
      {erro && <div className="aviso aviso-erro" role="alert" style={{ margin: '8px 0' }}>{erro}</div>}
      {novo && (
        <form style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'flex-end', margin: '8px 0' }}
          onSubmit={(ev) => {
            ev.preventDefault();
            acao(async () => {
              await api.post(`/api/encomendas/${e.id}/consumos`, { materialId: Number(novo.materialId) || null, quantidade: Number(String(novo.quantidade).replace(',', '.')) || null });
              setNovo(null);
            }, 'Material registrado e baixado do estoque.');
          }}>
          <div className="campo" style={{ flex: '2 1 200px' }}>
            <label htmlFor={`cm-m-${e.id}`}>Material</label>
            <select id={`cm-m-${e.id}`} className="entrada" value={novo.materialId} onChange={(ev) => setNovo({ ...novo, materialId: ev.target.value })} required>
              <option value="">Escolha…</option>
              {(materiais.dados || []).map((m) => <option key={m.id} value={m.id}>{m.nome} ({qtdUn(m.quantidade, m.unidade)} em estoque)</option>)}
            </select>
          </div>
          <div className="campo" style={{ flex: '1 1 100px' }}>
            <label htmlFor={`cm-q-${e.id}`}>Quantidade</label>
            <input id={`cm-q-${e.id}`} className="entrada" type="number" min="0" step="0.001" value={novo.quantidade} onChange={(ev) => setNovo({ ...novo, quantidade: ev.target.value })} required />
          </div>
          <button type="submit" className="btn btn-oliva">Registrar</button>
          <button type="button" className="btn btn-fantasma" onClick={() => setNovo(null)}>Cancelar</button>
        </form>
      )}
      {!lista.dados?.length ? <p className="mudo forte">{lista.carregando ? 'Carregando…' : 'Nenhum material baixado ainda.'}</p> : (
        <ul className="lista-linhas">
          {lista.dados.map((c) => (
            <li key={c.id}>
              <div className="info">
                <p>{c.materialNome}: {qtdUn(c.quantidade, c.unidade)}</p>
                <p>{moedaUnit(c.custoUnitario)}/{c.unidade} · {c.origem === 'FICHA_ENCOMENDA' ? 'pela ficha' : 'extra'} · {data(c.data)}</p>
              </div>
              <strong className="num">{moeda(c.custoTotal)}</strong>
              {!cancelada && (
                <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Devolver ${c.materialNome} ao estoque`}
                  onClick={() => acao(() => api.del(`/api/consumos/${c.id}`), 'Material devolvido ao estoque.')}><Icone nome="fechar" tamanho={16} /></button>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
