import { useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, Modal, ModalForm, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { data, linkWhats, moeda, primeiroNome, telefone } from '../util.js';
import { duracaoTexto } from '../site/BlocoPix.jsx';
import { Topo } from './Layout.jsx';

const STATUS_MAT = {
  AGUARDANDO_PIX: ['Aguardando Pix', 'ocre'],
  A_CONFERIR: ['Pix a conferir', 'terra'],
  LIBERADA: ['Liberada', 'oliva'],
  CANCELADA: ['Cancelada', 'perigo'],
};

const linkAcesso = (m) => `${window.location.origin}/meu-curso/${m.codigo}`;
const msgAcesso = (m) => `Oi, ${primeiroNome(m.nome)}! Seu acesso ao curso "${m.curso}" está liberado. É só abrir este link (guarde, ele é só seu): ${linkAcesso(m)} Qualquer dúvida nas aulas, me chama aqui. Bons nós!`;

/** Cursos online: aulas em vídeo, alunas e liberação do acesso depois do Pix. */
export default function Cursos() {
  const [form, setForm] = useState(null);
  const [aberto, setAberto] = useState(null);
  const toast = useToast();
  const lista = useCarregar(() => api.get('/api/cursos'));
  const pendentes = useCarregar(() => api.get('/api/matriculas/pendentes'));

  function atualizar() { lista.recarregar(); pendentes.recarregar(); }

  return (
    <>
      <Topo titulo="Cursos online">
        <button type="button" className="btn btn-terra" onClick={() => setForm({ publicado: false })}><Icone nome="mais" tamanho={18} largura={3} />Novo curso</button>
      </Topo>
      <p className="mudo forte" style={{ marginTop: -10 }}>
        Grave as aulas, suba no YouTube como "não listado" (ou no Vimeo ou Google Drive) e cole o link aqui.
        A aluna paga por Pix no site; depois que você conferir e liberar, ela assiste pelo link que recebe no WhatsApp.
      </p>

      {pendentes.dados?.length > 0 && (
        <section className="cartao" style={{ background: 'var(--terra-claro)' }} aria-labelledby="t-pixc">
          <div className="cartao-titulo"><h2 id="t-pixc">Pix a conferir</h2></div>
          <ul className="lista-linhas">
            {pendentes.dados.map((m) => <LinhaMatricula key={m.id} m={m} mostrarCurso aoMudar={atualizar} />)}
          </ul>
        </section>
      )}

      <Estado carregando={lista.carregando} erro={lista.erro}>
        {lista.dados?.length === 0 ? (
          <div className="cartao"><Vazio>Nenhum curso ainda. Comece pelo que mais pedem nas oficinas: a primeira mandala, o porta-vaso…</Vazio></div>
        ) : (
          <div className="cursos-grade">
            {lista.dados?.map((c) => (
              <article key={c.id} className="curso-card" style={{ cursor: 'default' }}>
                <span className="curso-capa">{c.capa ? <img src={c.capa} alt="" /> : <span className="cursiva" aria-hidden="true">macramê</span>}</span>
                <div style={{ padding: '14px 18px 18px', display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                    <span className={`selo ${c.publicado ? 'selo-oliva' : 'selo-areia'}`}>{c.publicado ? 'No site' : 'Rascunho'}</span>
                    {c.pendentes > 0 && <span className="selo selo-terra">{c.pendentes} aguardando Pix</span>}
                  </div>
                  <h2 style={{ fontSize: 20 }}>{c.titulo}</h2>
                  <p className="mudo forte" style={{ fontSize: 14 }}>
                    {moeda(c.valor)} · {c.aulas.length} {c.aulas.length === 1 ? 'aula' : 'aulas'}{duracaoTexto(c.duracaoTotal) ? ` · ${duracaoTexto(c.duracaoTotal)}` : ''}
                  </p>
                  <p className="forte" style={{ fontSize: 14, color: 'var(--oliva-texto)' }}>{c.liberadas} {c.liberadas === 1 ? 'aluna' : 'alunas'} · {moeda(c.receita)} recebidos</p>
                  <button type="button" className="btn btn-claro" style={{ alignSelf: 'flex-start', marginTop: 4 }} onClick={() => setAberto(c.id)}>Abrir curso</button>
                </div>
              </article>
            ))}
          </div>
        )}
      </Estado>

      {form && <FormCurso curso={form} aoFechar={() => setForm(null)} aoSalvar={(c) => { setForm(null); toast('Curso salvo.'); atualizar(); if (!form.id) setAberto(c.id); }} />}
      {aberto && <DetalheCurso id={aberto} aoFechar={() => { setAberto(null); atualizar(); }} aoEditar={(c) => { setAberto(null); setForm(c); }} />}
    </>
  );
}

function FormCurso({ curso: c, aoFechar, aoSalvar }) {
  return (
    <ModalForm titulo={c.id ? 'Editar curso' : 'Novo curso'} largo aoFechar={aoFechar} aoSalvar={async (fd) => {
      const d = lerForm(fd, ['valor', 'ordem']);
      d.publicado = fd.get('publicado') === 'on';
      aoSalvar(c.id ? await api.put(`/api/cursos/${c.id}`, d) : await api.post('/api/cursos', d));
    }}>
      <Campo rotulo="Título" nome="titulo" defaultValue={c.titulo} required maxLength={140} inteiro placeholder="Macramê do zero: sua primeira mandala" />
      <Campo rotulo="Resumo (aparece no cartão do site)" nome="resumo" defaultValue={c.resumo} maxLength={220} inteiro placeholder="Seis aulas curtas para fazer uma mandala de 30 cm em casa" />
      <Campo rotulo="Valor (R$)" nome="valor" tipo="number" min="0" step="0.01" defaultValue={c.valor ?? ''} required dica="0 = curso gratuito (libera na hora)." />
      <Campo rotulo="Nível" nome="nivel" defaultValue={c.nivel} maxLength={40} placeholder="Iniciante" />
      <Campo rotulo="Sobre o curso" nome="descricao" linhas={5} defaultValue={c.descricao} maxLength={8000} inteiro placeholder="Para quem é, o que vai aprender, o que vai levar pronto…" />
      <Campo rotulo="Materiais que a aluna precisa (um por linha)" nome="materiais" linhas={4} defaultValue={c.materiais} maxLength={4000} inteiro placeholder={'Cordão de algodão 5 mm (30 m)\nAro de madeira de 20 cm\nTesoura e fita métrica'} />
      <Campo rotulo="Ordem no site" nome="ordem" tipo="number" step="1" defaultValue={c.ordem ?? 0} />
      <label className="checkbox" style={{ alignSelf: 'end', minHeight: 48 }}><input type="checkbox" name="publicado" defaultChecked={c.publicado} /> Mostrar no site</label>
    </ModalForm>
  );
}

function DetalheCurso({ id, aoFechar, aoEditar }) {
  const curso = useCarregar(() => api.get(`/api/cursos/${id}`));
  const mats = useCarregar(() => api.get(`/api/cursos/${id}/matriculas`));
  const [aula, setAula] = useState(null);
  const [excluirAula, setExcluirAula] = useState(null);
  const [nova, setNova] = useState(false);
  const [excluir, setExcluir] = useState(false);
  const [erro, setErro] = useState(null);
  const toast = useToast();
  const c = curso.dados;

  async function acao(fn, msg) {
    setErro(null);
    try { await fn(); toast(msg); curso.recarregar(); mats.recarregar(); } catch (e) { setErro(e.message); }
  }

  async function mover(i, delta) {
    const ids = c.aulas.map((a) => a.id);
    [ids[i], ids[i + delta]] = [ids[i + delta], ids[i]];
    await acao(() => api.put(`/api/cursos/${id}/ordem-aulas`, { ids }), 'Ordem das aulas atualizada.');
  }

  async function capa(ev) {
    const f = ev.target.files[0];
    ev.target.value = '';
    if (!f) return;
    if (f.size > 8 * 1024 * 1024) { setErro('A foto deve ter no máximo 8 MB.'); return; }
    const fd = new FormData();
    fd.append('foto', f);
    await acao(() => api.enviar(`/api/cursos/${id}/capa`, fd), 'Capa atualizada.');
  }

  if (aula) {
    return (
      <ModalForm titulo={aula.id ? 'Editar aula' : 'Nova aula'} aoFechar={() => setAula(null)} aoSalvar={async (fd) => {
        const d = lerForm(fd, ['duracaoMinutos']);
        d.gratuita = fd.get('gratuita') === 'on';
        if (aula.id) await api.put(`/api/aulas/${aula.id}`, d);
        else await api.post(`/api/cursos/${id}/aulas`, d);
        toast('Aula salva.');
        setAula(null);
        curso.recarregar();
      }}>
        <Campo rotulo="Título da aula" nome="titulo" defaultValue={aula.titulo} required maxLength={160} inteiro placeholder="Nó quadrado" />
        <Campo rotulo="Link do vídeo" nome="videoUrl" tipo="url" defaultValue={aula.videoUrl} maxLength={500} inteiro placeholder="https://youtu.be/…"
          dica='YouTube (de preferência "não listado"), Vimeo ou Google Drive (compartilhado com "qualquer pessoa com o link").' />
        <Campo rotulo="Duração (minutos)" nome="duracaoMinutos" tipo="number" min="1" max="600" step="1" defaultValue={aula.duracaoMinutos} />
        <label className="checkbox" style={{ alignSelf: 'end', minHeight: 48 }}><input type="checkbox" name="gratuita" defaultChecked={aula.gratuita} /> Aula aberta (amostra no site)</label>
        <Campo rotulo="Texto da aula" nome="descricao" linhas={4} defaultValue={aula.descricao} maxLength={4000} inteiro placeholder="Dicas, medidas, o que preparar antes…" />
      </ModalForm>
    );
  }
  if (nova) {
    return (
      <ModalForm titulo="Inscrever aluna" textoSalvar="Inscrever" aoFechar={() => setNova(false)} aoSalvar={async (fd) => {
        await api.post(`/api/cursos/${id}/matriculas`, lerForm(fd));
        toast('Aluna inscrita.');
        setNova(false);
        mats.recarregar();
        curso.recarregar();
      }}>
        <Campo rotulo="Nome" nome="nome" required maxLength={140} inteiro />
        <Campo rotulo="WhatsApp" nome="whatsapp" required inputMode="tel" placeholder="(98) 90000-0000" />
        <Campo rotulo="E-mail (opcional)" nome="email" tipo="email" maxLength={160} />
        <Campo rotulo="Situação" nome="status" defaultValue="AGUARDANDO_PIX" inteiro
          opcoes={[['AGUARDANDO_PIX', 'Aguardando Pix'], ['LIBERADA', 'Já pagou: liberar o acesso agora']]} />
      </ModalForm>
    );
  }
  if (excluirAula) {
    return <Confirmar titulo="Excluir aula" perigo textoBotao="Excluir" texto={`Excluir a aula "${excluirAula.titulo}"? O progresso das alunas nesta aula também sai.`}
      aoFechar={() => setExcluirAula(null)} aoConfirmar={async () => { await api.del(`/api/aulas/${excluirAula.id}`); toast('Aula excluída.'); setExcluirAula(null); curso.recarregar(); }} />;
  }
  if (excluir) {
    return <Confirmar titulo="Excluir curso" perigo textoBotao="Excluir" texto="Excluir este curso? Só é possível se ninguém tiver se inscrito."
      aoFechar={() => setExcluir(false)} aoConfirmar={async () => { await api.del(`/api/cursos/${id}`); toast('Curso excluído.'); aoFechar(); }} />;
  }

  return (
    <Modal titulo={c ? c.titulo : 'Curso'} aoFechar={aoFechar} largo rodape={c && <>
      {mats.dados?.length === 0 && <button type="button" className="btn btn-perigo" onClick={() => setExcluir(true)}>Excluir</button>}
      {c.publicado && <a className="btn btn-claro" href={`/cursos/${c.id}`} target="_blank" rel="noopener noreferrer"><Icone nome="site" tamanho={18} />Ver no site</a>}
      <label className="btn btn-claro botao-upload"><Icone nome="foto" tamanho={18} />{c.capa ? 'Trocar capa' : 'Enviar capa'}
        <input type="file" accept="image/jpeg,image/png,image/webp" onChange={capa} aria-label="Foto de capa do curso" /></label>
      <button type="button" className="btn btn-terra" onClick={() => aoEditar(c)}><Icone nome="editar" tamanho={18} />Editar dados</button>
    </>}>
      <Estado carregando={curso.carregando} erro={curso.erro}>
        {c && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
            {erro && <div className="aviso aviso-erro" role="alert">{erro}</div>}
            <p className="forte" style={{ color: 'var(--oliva-texto)' }}>
              {moeda(c.valor)} · {c.publicado ? 'no site' : 'rascunho (fora do site)'} · {c.liberadas} {c.liberadas === 1 ? 'aluna' : 'alunas'} · {moeda(c.receita)} recebidos
            </p>
            <section aria-labelledby="t-aulas-adm">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8, flexWrap: 'wrap', marginBottom: 4 }}>
                <h3 id="t-aulas-adm" style={{ fontSize: 18 }}>Aulas{duracaoTexto(c.duracaoTotal) ? ` · ${duracaoTexto(c.duracaoTotal)}` : ''}</h3>
                <button type="button" className="btn btn-claro btn-p" onClick={() => setAula({})}><Icone nome="mais" tamanho={16} largura={3} />Nova aula</button>
              </div>
              {c.aulas.length === 0 ? <p className="mudo forte">Nenhuma aula ainda.</p> : (
                <div>
                  {c.aulas.map((a, i) => (
                    <div key={a.id} className="aula-adm">
                      <span className="num-aula">{i + 1}</span>
                      <div style={{ flex: '1 1 200px', minWidth: 0 }}>
                        <p className="forte">{a.titulo}</p>
                        <p className="mudo" style={{ fontSize: 13 }}>
                          {[duracaoTexto(a.duracaoMinutos), a.gratuita ? 'aberta no site' : null, a.videoEmbed ? null : 'sem vídeo'].filter(Boolean).join(' · ') || 'vídeo ok'}
                        </p>
                      </div>
                      <div style={{ display: 'flex', gap: 2 }}>
                        <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Subir ${a.titulo}`} disabled={i === 0} onClick={() => mover(i, -1)}><Icone nome="setaCima" tamanho={18} /></button>
                        <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Descer ${a.titulo}`} disabled={i === c.aulas.length - 1} onClick={() => mover(i, 1)}><Icone nome="setaBaixo" tamanho={18} /></button>
                        <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Editar ${a.titulo}`} onClick={() => setAula(a)}><Icone nome="editar" tamanho={18} /></button>
                        <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Excluir ${a.titulo}`} onClick={() => setExcluirAula(a)}><Icone nome="lixo" tamanho={18} /></button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>
            <section aria-labelledby="t-alunas">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8, flexWrap: 'wrap', marginBottom: 4 }}>
                <h3 id="t-alunas" style={{ fontSize: 18 }}>Alunas</h3>
                <button type="button" className="btn btn-claro btn-p" onClick={() => setNova(true)}><Icone nome="mais" tamanho={16} largura={3} />Inscrever aluna</button>
              </div>
              {mats.dados?.length === 0 ? <p className="mudo forte">Ninguém inscrito ainda.</p> : (
                <ul className="lista-linhas">
                  {mats.dados?.map((m) => <LinhaMatricula key={m.id} m={m} aoMudar={() => { mats.recarregar(); curso.recarregar(); }} />)}
                </ul>
              )}
            </section>
          </div>
        )}
      </Estado>
    </Modal>
  );
}

function LinhaMatricula({ m, mostrarCurso, aoMudar }) {
  const [erro, setErro] = useState(null);
  const [ocupado, setOcupado] = useState(false);
  const toast = useToast();
  const [rot, cor] = STATUS_MAT[m.status];

  async function mudar(status, msg) {
    setErro(null);
    setOcupado(true);
    try { await api.patch(`/api/matriculas/${m.id}/status`, { status, formaPagamento: 'PIX' }); toast(msg); aoMudar(); }
    catch (e) { setErro(e.message); } finally { setOcupado(false); }
  }

  async function copiar() {
    try { await navigator.clipboard.writeText(linkAcesso(m)); toast('Link de acesso copiado.'); } catch { toast('Não foi possível copiar.', 'erro'); }
  }

  return (
    <li style={{ flexWrap: 'wrap' }}>
      <div className="info" style={{ flex: '1 1 220px' }}>
        <p>{m.nome}</p>
        <p>{telefone(m.whatsapp)}{mostrarCurso ? ` · ${m.curso}` : ''} · {m.origem === 'SITE' ? 'pelo site' : 'manual'} em {data(m.criadoEm)}
          {m.status === 'LIBERADA' && m.totalAulas > 0 ? ` · ${m.aulasConcluidas} de ${m.totalAulas} aulas` : ''}</p>
        {erro && <p className="erro-texto" role="alert">{erro}</p>}
      </div>
      <span className={`selo selo-${cor}`}>{rot}</span>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
        {(m.status === 'AGUARDANDO_PIX' || m.status === 'A_CONFERIR') && (
          <button type="button" className="btn btn-oliva btn-p" disabled={ocupado} onClick={() => mudar('LIBERADA', 'Acesso liberado e Pix lançado no financeiro.')}>
            <Icone nome="check" tamanho={16} largura={3} />Confirmar Pix e liberar
          </button>
        )}
        {m.status === 'LIBERADA' && <>
          <a className="btn btn-oliva btn-p" href={linkWhats(m.whatsapp, msgAcesso(m))} target="_blank" rel="noopener noreferrer"><Icone nome="whats" tamanho={16} />Enviar acesso</a>
          <button type="button" className="btn btn-claro btn-p" onClick={copiar}><Icone nome="link" tamanho={16} />Copiar link</button>
        </>}
        {m.status !== 'CANCELADA' && <button type="button" className="btn btn-fantasma btn-p" disabled={ocupado} onClick={() => mudar('CANCELADA', 'Inscrição cancelada.')}>Cancelar</button>}
        {m.status === 'CANCELADA' && <button type="button" className="btn btn-fantasma btn-p" disabled={ocupado} onClick={() => mudar('AGUARDANDO_PIX', 'Inscrição reativada.')}>Reativar</button>}
      </div>
    </li>
  );
}

