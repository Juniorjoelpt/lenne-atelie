import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { linkWhats, moeda, primeiroNome } from '../util.js';
import BlocoPix, { duracaoTexto } from './BlocoPix.jsx';
import Cabecalho from './Cabecalho.jsx';

/** Área da aluna, aberta pelo link com o código: assiste às aulas e marca o que já fez. */
export default function MeuCurso() {
  const { codigo } = useParams();
  const [a, setA] = useState(null);
  const [erro, setErro] = useState(null);
  const [atual, setAtual] = useState(null);
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    document.title = 'Meu curso · Lenne Carvalho Ateliê';
    api.get(`/api/publico/matriculas/${encodeURIComponent(codigo)}`).then((x) => {
      setA(x);
      const aulas = x.curso.aulas;
      setAtual((aulas.find((au) => !au.concluida) || aulas[0])?.id ?? null);
    }).catch((e) => setErro(e.status === 404
      ? 'Não encontramos este acesso. Confira se o link está completo ou fale com o ateliê pelo WhatsApp.'
      : e.message));
  }, [codigo]);

  const aulas = a?.curso.aulas || [];
  const aula = useMemo(() => aulas.find((x) => x.id === atual), [aulas, atual]);
  const indice = aulas.findIndex((x) => x.id === atual);
  const pct = aulas.length ? Math.round((a.concluidas / aulas.length) * 100) : 0;
  const liberada = a?.status === 'LIBERADA';

  async function marcar(concluida) {
    setSalvando(true);
    try {
      const x = await api.put(`/api/publico/matriculas/${encodeURIComponent(codigo)}/aulas/${aula.id}?concluida=${concluida}`);
      setA(x);
      if (concluida && indice < aulas.length - 1) setAtual(aulas[indice + 1].id);
    } catch (e) { setErro(e.message); } finally { setSalvando(false); }
  }

  return (
    <div className="site">
      <Cabecalho />
      <main className="envolve" style={{ paddingTop: 24, paddingBottom: 56, display: 'flex', flexDirection: 'column', gap: 20 }}>
        {erro && <div className="aviso aviso-erro" role="alert">{erro}</div>}
        {!erro && !a && <p className="carregando">Carregando…</p>}
        {a && !liberada && (
          <section className="bloco" style={{ background: 'var(--cru)', maxWidth: 620, alignSelf: 'center', width: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
            <p className="sobretitulo">Curso online</p>
            <h1 style={{ fontSize: 28 }}>{a.curso.titulo}</h1>
            {a.status === 'A_CONFERIR' ? (
              <div className="aviso aviso-info">Oi, {primeiroNome(a.nome)}! Recebemos o aviso do seu comprovante. Assim que a Lenne conferir o Pix, as aulas abrem aqui neste mesmo link.</div>
            ) : (
              <>
                <p style={{ fontWeight: 600 }}>Oi, {primeiroNome(a.nome)}! Sua inscrição está guardada. Para liberar as aulas, faça o Pix de <strong>{moeda(a.valor)}</strong> e mande o comprovante.</p>
                <BlocoPix chave={a.chavePix} titular={a.titularPix} />
                <a className="btn btn-oliva" style={{ alignSelf: 'flex-start' }} target="_blank" rel="noopener noreferrer"
                  href={linkWhats(a.whatsappAtelie, `Olá, Lenne! Segue o comprovante do Pix do curso "${a.curso.titulo}".`)}
                  onClick={() => api.post(`/api/publico/matriculas/${encodeURIComponent(codigo)}/comprovante-enviado`).then(setA).catch(() => {})}>
                  <Icone nome="whats" tamanho={18} />Enviar comprovante
                </a>
              </>
            )}
            <p className="mudo" style={{ fontSize: 14 }}>Guarde este link: é por ele que você vai assistir às aulas.</p>
          </section>
        )}
        {a && liberada && (
          <>
            <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'flex-end', justifyContent: 'space-between', gap: 12 }}>
              <div style={{ minWidth: 0 }}>
                <p className="sobretitulo">Olá, {primeiroNome(a.nome)}!</p>
                <h1 style={{ fontSize: 30 }}>{a.curso.titulo}</h1>
              </div>
              <div style={{ minWidth: 220, flex: '0 1 320px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 800, fontSize: 14 }}><span>Seu progresso</span><span>{a.concluidas} de {aulas.length} aulas</span></div>
                <div className="progresso" style={{ marginTop: 6 }} role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100} aria-label="Progresso do curso"><div style={{ width: `${pct}%` }} /></div>
              </div>
            </div>
            <div className="sala-aula">
              <section className="bloco" style={{ background: 'var(--cru)', padding: 18 }} aria-label="Aula atual">
                {aula ? (
                  <>
                    {aula.videoEmbed
                      ? <div className="video"><iframe key={aula.id} src={aula.videoEmbed} title={aula.titulo} allow="encrypted-media; fullscreen; picture-in-picture" allowFullScreen /></div>
                      : <div className="aviso aviso-info">Esta aula ainda não tem vídeo. Volte mais tarde.</div>}
                    <div style={{ padding: '16px 6px 4px', display: 'flex', flexDirection: 'column', gap: 10 }}>
                      <p className="mudo forte" style={{ fontSize: 14 }}>Aula {indice + 1} de {aulas.length}{duracaoTexto(aula.duracaoMinutos) ? ` · ${duracaoTexto(aula.duracaoMinutos)}` : ''}</p>
                      <h2 style={{ fontSize: 24 }}>{aula.titulo}</h2>
                      {aula.descricao && <p style={{ whiteSpace: 'pre-line', fontWeight: 600, color: '#4A3A2C' }}>{aula.descricao}</p>}
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 6 }}>
                        {aula.concluida
                          ? <button type="button" className="btn btn-claro" disabled={salvando} onClick={() => marcar(false)}><Icone nome="check" tamanho={16} largura={3} cor="var(--oliva)" />Concluída (desmarcar)</button>
                          : <button type="button" className="btn btn-oliva" disabled={salvando} onClick={() => marcar(true)}><Icone nome="check" tamanho={16} largura={3} />Marcar como concluída</button>}
                        {indice < aulas.length - 1 && <button type="button" className="btn btn-fantasma" onClick={() => setAtual(aulas[indice + 1].id)}>Próxima aula<Icone nome="seta" tamanho={16} /></button>}
                      </div>
                    </div>
                  </>
                ) : <p>Este curso ainda não tem aulas.</p>}
              </section>
              <aside className="bloco" style={{ background: 'var(--cru)', padding: 18 }} aria-label="Aulas do curso">
                <ol className="lista-aulas lista-aulas-sala">
                  {aulas.map((x, i) => (
                    <li key={x.id}>
                      <button type="button" className={x.id === atual ? 'ativa' : ''} aria-current={x.id === atual ? 'true' : undefined} onClick={() => setAtual(x.id)}>
                        <span className={`num-aula ${x.concluida ? 'feita' : ''}`}>{x.concluida ? <Icone nome="check" tamanho={14} largura={3} /> : i + 1}</span>
                        <span style={{ flex: 1, minWidth: 0, textAlign: 'left' }}><span className="forte" style={{ display: 'block' }}>{x.titulo}</span>
                          {duracaoTexto(x.duracaoMinutos) && <span className="mudo" style={{ fontSize: 13 }}>{duracaoTexto(x.duracaoMinutos)}</span>}</span>
                      </button>
                    </li>
                  ))}
                </ol>
                {a.curso.materiais && (
                  <details style={{ marginTop: 14 }}>
                    <summary className="forte" style={{ cursor: 'pointer' }}>Materiais do curso</summary>
                    <p style={{ whiteSpace: 'pre-line', marginTop: 8, fontWeight: 600 }}>{a.curso.materiais}</p>
                  </details>
                )}
                <a className="btn btn-claro btn-p" style={{ marginTop: 14 }} target="_blank" rel="noopener noreferrer"
                  href={linkWhats(a.whatsappAtelie, `Olá, Lenne! Tenho uma dúvida no curso "${a.curso.titulo}"${aula ? `, aula "${aula.titulo}"` : ''}.`)}>
                  <Icone nome="whats" tamanho={16} />Tirar dúvida
                </a>
              </aside>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
