import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, Modal, ModalForm, Selo, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { STATUS_INSCRICAO, STATUS_OFICINA, dataHora, linkWhats, moeda, telefone } from '../util.js';
import { Topo } from './Layout.jsx';

export default function Oficinas() {
  const [params] = useSearchParams();
  const [form, setForm] = useState(null);
  const [aberta, setAberta] = useState(null);
  const toast = useToast();
  const { dados, erro, carregando, recarregar } = useCarregar(() => api.get('/api/oficinas'));
  const pix = useCarregar(() => api.get('/api/inscricoes/pix-a-conferir'));

  const agora = Date.now();
  const proximas = (dados || []).filter((o) => new Date(o.inicio).getTime() >= agora).reverse();
  const passadas = (dados || []).filter((o) => new Date(o.inicio).getTime() < agora);

  function atualizar() {
    recarregar();
    pix.recarregar();
  }

  return (
    <>
      <Topo titulo="Oficinas">
        <button type="button" className="btn btn-terra" onClick={() => setForm({})}><Icone nome="mais" tamanho={18} largura={3} />Nova oficina</button>
      </Topo>

      {(pix.dados?.length > 0 || params.get('pix')) && (
        <section className="cartao" style={{ background: 'var(--terra-claro)' }} aria-labelledby="t-pix">
          <div className="cartao-titulo"><h2 id="t-pix">Pix a conferir</h2></div>
          {pix.dados?.length === 0 ? <Vazio>Nenhum Pix pendente. Tudo conferido!</Vazio> : (
            <ul className="lista-linhas">
              {pix.dados?.map((i) => <LinhaInscricao key={i.id} i={i} mostrarOficina aoMudar={atualizar} />)}
            </ul>
          )}
        </section>
      )}

      <Estado carregando={carregando} erro={erro}>
        <section aria-labelledby="t-prox">
          <h2 id="t-prox" style={{ fontSize: 22, marginBottom: 14 }}>Próximas turmas</h2>
          {proximas.length === 0 ? <div className="cartao"><Vazio>Nenhuma oficina marcada. Crie uma e ela aparece no site.</Vazio></div> : (
            <div className="grade-2">{proximas.map((o) => <CartaoOficina key={o.id} o={o} aoAbrir={() => setAberta(o.id)} />)}</div>
          )}
        </section>
        {passadas.length > 0 && (
          <section className="cartao" aria-labelledby="t-pass">
            <div className="cartao-titulo"><h2 id="t-pass">Oficinas anteriores</h2></div>
            <ul className="lista-linhas">
              {passadas.map((o) => (
                <li key={o.id}>
                  <div className="info"><p>{o.titulo}</p><p>{dataHora(o.inicio)} · {o.confirmadas} {o.confirmadas === 1 ? 'aluna' : 'alunas'} · {moeda(o.confirmadas * o.valor)}</p></div>
                  <button type="button" className="btn btn-claro btn-p" onClick={() => setAberta(o.id)}>Abrir</button>
                </li>
              ))}
            </ul>
          </section>
        )}
      </Estado>

      {aberta && <DetalheOficina id={aberta} aoFechar={() => { setAberta(null); atualizar(); }}
        aoEditar={(o) => { setAberta(null); setForm(o); }} />}

      {form && (
        <ModalForm titulo={form.id ? 'Editar oficina' : 'Nova oficina'} aoFechar={() => setForm(null)} aoSalvar={async (fd) => {
          const d = lerForm(fd, ['vagas', 'valor']);
          d.publicada = fd.get('publicada') === 'on';
          if (form.id) await api.put(`/api/oficinas/${form.id}`, d);
          else await api.post('/api/oficinas', d);
          toast('Oficina salva.');
          setForm(null);
          atualizar();
        }}>
          <Campo rotulo="Título" nome="titulo" defaultValue={form.titulo} required maxLength={140} inteiro placeholder="Mandala para iniciantes" />
          <Campo rotulo="Início" nome="inicio" tipo="datetime-local" defaultValue={form.inicio?.slice(0, 16)} required />
          <Campo rotulo="Término" nome="fim" tipo="datetime-local" defaultValue={form.fim?.slice(0, 16)} />
          <Campo rotulo="Vagas" nome="vagas" tipo="number" min="1" defaultValue={form.vagas ?? 10} required />
          <Campo rotulo="Valor da inscrição (R$)" nome="valor" tipo="number" min="0" step="0.01" defaultValue={form.valor} required />
          <Campo rotulo="Local" nome="local" defaultValue={form.local} maxLength={160} inteiro placeholder="No ateliê" />
          <Campo rotulo="Descrição para o site" nome="descricao" linhas={3} defaultValue={form.descricao} inteiro
            placeholder="O que a aluna vai produzir, se precisa levar algo, se tem café…" />
          <Campo rotulo="Situação" nome="status" opcoes={Object.fromEntries(Object.entries(STATUS_OFICINA).map(([k, [r]]) => [k, r]))} defaultValue={form.status || 'ABERTA'} />
          <label className="checkbox" style={{ alignSelf: 'end', minHeight: 48 }}>
            <input type="checkbox" name="publicada" defaultChecked={form.publicada ?? true} /> Mostrar no site
          </label>
        </ModalForm>
      )}
    </>
  );
}

function CartaoOficina({ o, aoAbrir }) {
  const ocupadas = o.vagas - o.vagasRestantes;
  return (
    <article className="cartao" style={{ background: 'var(--oliva-menu)', display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10, flexWrap: 'wrap' }}>
        <Selo mapa={STATUS_OFICINA} valor={o.status} />
        {!o.publicada && <span className="selo selo-areia">Fora do site</span>}
      </div>
      <div>
        <h3 style={{ fontSize: 22 }}>{o.titulo}</h3>
        <p style={{ fontWeight: 700, color: 'var(--oliva-texto)' }}>{dataHora(o.inicio)} · {moeda(o.valor)}</p>
      </div>
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 14, fontWeight: 800 }}><span>Vagas</span><span>{ocupadas} de {o.vagas}</span></div>
        <div className="progresso" style={{ marginTop: 8 }}><div style={{ width: `${(ocupadas / o.vagas) * 100}%` }} /></div>
      </div>
      <p style={{ fontSize: 14, fontWeight: 700 }}>
        {o.confirmadas} confirmadas · {o.aguardandoPix + o.aConferir} aguardando Pix · {o.listaEspera} na espera
      </p>
      <button type="button" className="btn btn-claro" style={{ alignSelf: 'flex-start' }} onClick={aoAbrir}>Ver inscrições</button>
    </article>
  );
}

function DetalheOficina({ id, aoFechar, aoEditar }) {
  const of = useCarregar(() => api.get(`/api/oficinas/${id}`));
  const ins = useCarregar(() => api.get(`/api/oficinas/${id}/inscricoes`));
  const [nova, setNova] = useState(false);
  const [excluir, setExcluir] = useState(false);
  const toast = useToast();
  const o = of.dados;

  function atualizar() { of.recarregar(); ins.recarregar(); }

  if (nova) {
    return (
      <ModalForm titulo="Inscrever aluna" textoSalvar="Inscrever" aoFechar={() => setNova(false)} aoSalvar={async (fd) => {
        await api.post(`/api/oficinas/${id}/inscricoes`, lerForm(fd));
        toast('Inscrição registrada.');
        setNova(false);
        atualizar();
      }}>
        <Campo rotulo="Nome" nome="nome" required maxLength={140} inteiro />
        <Campo rotulo="WhatsApp" nome="whatsapp" required inputMode="tel" placeholder="(98) 90000-0000" />
        <Campo rotulo="Situação" nome="status" defaultValue="AGUARDANDO_PIX"
          opcoes={{ AGUARDANDO_PIX: 'Aguardando Pix', CONFIRMADA: 'Já pagou (confirmada)', LISTA_ESPERA: 'Lista de espera' }} />
      </ModalForm>
    );
  }
  if (excluir) {
    return <Confirmar titulo="Excluir oficina" perigo textoBotao="Excluir" texto="Excluir esta oficina? Só é possível se não houver inscrições."
      aoFechar={() => setExcluir(false)} aoConfirmar={async () => { await api.del(`/api/oficinas/${id}`); toast('Oficina excluída.'); aoFechar(); }} />;
  }

  return (
    <Modal titulo={o ? o.titulo : 'Oficina'} aoFechar={aoFechar} largo rodape={o && <>
      {ins.dados?.length === 0 && <button type="button" className="btn btn-perigo" onClick={() => setExcluir(true)}>Excluir</button>}
      <button type="button" className="btn btn-claro" onClick={() => aoEditar(o)}><Icone nome="editar" tamanho={18} />Editar</button>
      <button type="button" className="btn btn-terra" onClick={() => setNova(true)}><Icone nome="mais" tamanho={18} largura={3} />Inscrever aluna</button>
    </>}>
      <Estado carregando={of.carregando || ins.carregando} erro={of.erro || ins.erro}>
        {o && (
          <>
            <p className="forte" style={{ color: 'var(--oliva-texto)', marginBottom: 16 }}>
              {dataHora(o.inicio)} · {moeda(o.valor)} · {o.vagas - o.vagasRestantes} de {o.vagas} vagas ocupadas
            </p>
            {ins.dados?.length === 0 ? <Vazio>Nenhuma inscrição ainda.</Vazio> : (
              <ul className="lista-linhas">
                {ins.dados?.map((i) => <LinhaInscricao key={i.id} i={i} aoMudar={atualizar} />)}
              </ul>
            )}
          </>
        )}
      </Estado>
    </Modal>
  );
}

function LinhaInscricao({ i, mostrarOficina, aoMudar }) {
  const [erro, setErro] = useState(null);
  const [ocupado, setOcupado] = useState(false);
  const toast = useToast();

  async function mudar(status, mensagem) {
    setErro(null);
    setOcupado(true);
    try {
      await api.patch(`/api/inscricoes/${i.id}/status`, { status, formaPagamento: 'PIX' });
      toast(mensagem);
      aoMudar();
    } catch (e) {
      setErro(e.message);
    } finally {
      setOcupado(false);
    }
  }

  const primeiro = i.nome.split(' ')[0];
  const msgConfirmada = `Oi, ${primeiro}! Recebi seu Pix e sua vaga na oficina "${i.oficina}" (${dataHora(i.inicio)}) está confirmada. Até lá!`;
  const msgVaga = `Oi, ${primeiro}! Abriu uma vaga na oficina "${i.oficina}" (${dataHora(i.inicio)}). Ainda tem interesse?`;

  return (
    <li style={{ flexWrap: 'wrap' }}>
      <div className="info" style={{ flex: '1 1 220px' }}>
        <p>{i.nome}</p>
        <p>{telefone(i.whatsapp)}{mostrarOficina ? ` · ${i.oficina} (${dataHora(i.inicio)})` : ''}{i.origem === 'SITE' ? ' · pelo site' : ''}</p>
        {erro && <p className="erro-texto" role="alert">{erro}</p>}
      </div>
      <Selo mapa={STATUS_INSCRICAO} valor={i.status} />
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
        {(i.status === 'AGUARDANDO_PIX' || i.status === 'A_CONFERIR') && (
          <button type="button" className="btn btn-oliva btn-p" disabled={ocupado} onClick={() => mudar('CONFIRMADA', 'Pix confirmado e lançado no financeiro.')}>
            <Icone nome="check" tamanho={16} largura={3} />Confirmar Pix
          </button>
        )}
        {i.status === 'LISTA_ESPERA' && (
          <button type="button" className="btn btn-claro btn-p" disabled={ocupado} onClick={() => mudar('AGUARDANDO_PIX', 'Movida para a turma, aguardando Pix.')}>Dar a vaga</button>
        )}
        {i.status === 'CONFIRMADA' && (
          <a className="btn btn-claro btn-p" href={linkWhats(i.whatsapp, msgConfirmada)} target="_blank" rel="noopener noreferrer"><Icone nome="whats" tamanho={16} />Avisar</a>
        )}
        {i.status === 'LISTA_ESPERA' && (
          <a className="btn btn-claro btn-p" href={linkWhats(i.whatsapp, msgVaga)} target="_blank" rel="noopener noreferrer"><Icone nome="whats" tamanho={16} />Chamar</a>
        )}
        {i.status !== 'CANCELADA' && (
          <button type="button" className="btn btn-fantasma btn-p" disabled={ocupado} onClick={() => mudar('CANCELADA', 'Inscrição cancelada.')}>Cancelar</button>
        )}
        {i.status === 'CANCELADA' && (
          <button type="button" className="btn btn-fantasma btn-p" disabled={ocupado} onClick={() => mudar('LISTA_ESPERA', 'Voltou para a lista de espera.')}>Reativar</button>
        )}
      </div>
    </li>
  );
}

