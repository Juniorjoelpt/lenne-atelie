import { useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, Modal, ModalForm, Selo, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { FORMAS, FRENTES, STATUS_ENCOMENDA, data, hojeIso, linkWhats, moeda, telefone } from '../util.js';
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
                      <td><strong>{e.descricao}</strong><br /><span className="mudo" style={{ fontSize: 14 }}>{FRENTES[e.frente]}</span></td>
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
      const d = lerForm(fd, ['valorTotal', 'valorSinal', 'clienteId']);
      if (e.id) await api.put(`/api/encomendas/${e.id}`, d);
      else await api.post('/api/encomendas', d);
      aoSalvar();
    }}>
      <Campo rotulo="Cliente" nome="clienteId" defaultValue={e.clienteId} inteiro
        opcoes={Object.fromEntries(clientes.map((c) => [c.id, `${c.nome}${c.whatsapp ? ` · ${telefone(c.whatsapp)}` : ''}`]))} />
      <Campo rotulo="Descrição" nome="descricao" defaultValue={e.descricao} required maxLength={200} inteiro />
      <Campo rotulo="Frente" nome="frente" opcoes={FRENTES} defaultValue={e.frente || 'ENCOMENDA'} />
      <Campo rotulo="Status" nome="status" opcoes={Object.fromEntries(Object.entries(STATUS_ENCOMENDA).map(([k, [r]]) => [k, r]))} defaultValue={e.status || 'AGUARDANDO_SINAL'} />
      <Campo rotulo="Valor total (R$)" nome="valorTotal" tipo="number" min="0" step="0.01" defaultValue={e.valorTotal} required />
      <Campo rotulo="Sinal (R$)" nome="valorSinal" tipo="number" min="0" step="0.01" defaultValue={e.valorSinal ?? 0} />
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
