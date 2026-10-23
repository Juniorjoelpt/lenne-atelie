import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Modal } from '../componentes/ui.jsx';
import { linkWhats, moeda } from '../util.js';
import BlocoPix, { duracaoTexto } from './BlocoPix.jsx';
import Cabecalho from './Cabecalho.jsx';

/** Página de venda do curso online: o que tem nas aulas, amostra grátis e compra com Pix. */
export default function CursoPagina() {
  const { id } = useParams();
  const [c, setC] = useState(null);
  const [erro, setErro] = useState(null);
  const [amostra, setAmostra] = useState(null);
  const [comprar, setComprar] = useState(false);

  useEffect(() => {
    api.get(`/api/publico/cursos/${id}`).then((x) => { setC(x); document.title = `${x.titulo} · Curso online · Lenne Carvalho Ateliê`; })
      .catch((e) => setErro(e.status === 404 ? 'Este curso não está disponível.' : e.message));
  }, [id]);

  return (
    <div className="site">
      <Cabecalho voltar="/#cursos" rotuloVoltar="Todos os cursos" />
      <main className="envolve" style={{ paddingTop: 28, paddingBottom: 56, display: 'flex', flexDirection: 'column', gap: 24 }}>
        {erro && <div className="aviso aviso-erro" role="alert">{erro} <Link to="/">Voltar ao site</Link></div>}
        {!erro && !c && <p className="carregando">Carregando…</p>}
        {c && (
          <>
            <section className="bloco curso-topo" style={{ background: 'var(--oliva-menu)' }}>
              <div className="curso-capa">
                {c.capa ? <img src={c.capa} alt="" /> : <span className="cursiva" aria-hidden="true">macramê</span>}
              </div>
              <div style={{ flex: '1 1 380px', minWidth: 0, display: 'flex', flexDirection: 'column', gap: 12 }}>
                <p className="sobretitulo" style={{ color: 'var(--oliva-texto)' }}>Curso online{c.nivel ? ` · ${c.nivel}` : ''}</p>
                <h1 className="titulo-secao" style={{ margin: 0 }}>{c.titulo}</h1>
                {c.resumo && <p style={{ fontSize: 18, fontWeight: 700, color: '#2E3518' }}>{c.resumo}</p>}
                <p className="forte" style={{ color: 'var(--oliva-texto)' }}>
                  {c.totalAulas} {c.totalAulas === 1 ? 'aula' : 'aulas'}{duracaoTexto(c.duracaoTotal) ? ` · ${duracaoTexto(c.duracaoTotal)} de vídeo` : ''} · assista quando e quantas vezes quiser
                </p>
                <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 14, marginTop: 4 }}>
                  <span style={{ fontSize: 34, fontWeight: 900 }}>{Number(c.valor) > 0 ? moeda(c.valor) : 'Grátis'}</span>
                  <button type="button" className="btn btn-terra" style={{ minHeight: 52 }} onClick={() => setComprar(true)}>
                    <Icone nome="play" tamanho={16} />{Number(c.valor) > 0 ? 'Quero fazer o curso' : 'Quero assistir'}
                  </button>
                </div>
              </div>
            </section>

            <div className="grade-2" style={{ alignItems: 'start' }}>
              <section className="bloco" style={{ background: 'var(--cru)', padding: 28 }} aria-labelledby="t-aulas">
                <h2 id="t-aulas" style={{ fontSize: 24, marginBottom: 12 }}>As aulas</h2>
                <ol className="lista-aulas">
                  {c.aulas.map((a, i) => (
                    <li key={a.id}>
                      <span className="num-aula">{i + 1}</span>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <p className="forte">{a.titulo}</p>
                        <p className="mudo" style={{ fontSize: 14 }}>{[duracaoTexto(a.duracaoMinutos), a.gratuita ? 'aula aberta' : null].filter(Boolean).join(' · ')}</p>
                      </div>
                      {a.videoEmbed
                        ? <button type="button" className="btn btn-claro btn-p" onClick={() => setAmostra(a)}><Icone nome="play" tamanho={14} />Assistir</button>
                        : <span className="mudo" aria-label="Aula para quem fizer o curso" title="Para quem fizer o curso"><Icone nome="cadeado" tamanho={18} /></span>}
                    </li>
                  ))}
                </ol>
              </section>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
                {c.descricao && (
                  <section className="bloco" style={{ background: 'var(--cru)', padding: 28 }} aria-labelledby="t-sobre">
                    <h2 id="t-sobre" style={{ fontSize: 24, marginBottom: 10 }}>Sobre o curso</h2>
                    <p style={{ whiteSpace: 'pre-line', fontWeight: 600, color: '#4A3A2C' }}>{c.descricao}</p>
                  </section>
                )}
                {c.materiais && (
                  <section className="bloco" style={{ background: 'var(--ocre-claro)', padding: 28 }} aria-labelledby="t-mat">
                    <h2 id="t-mat" style={{ fontSize: 24, marginBottom: 10 }}>Você vai precisar de</h2>
                    <ul className="lista-check">{c.materiais.split('\n').filter((l) => l.trim()).map((l) => <li key={l}><Icone nome="check" tamanho={16} largura={3} />{l.replace(/^[-•*]\s*/, '')}</li>)}</ul>
                  </section>
                )}
              </div>
            </div>
          </>
        )}
      </main>
      {amostra && (
        <Modal titulo={amostra.titulo} aoFechar={() => setAmostra(null)} largo>
          <div className="video"><iframe src={amostra.videoEmbed} title={amostra.titulo} allow="encrypted-media; fullscreen; picture-in-picture" allowFullScreen loading="lazy" /></div>
        </Modal>
      )}
      {comprar && c && <Compra curso={c} aoFechar={() => setComprar(false)} />}
    </div>
  );
}

function Compra({ curso, aoFechar }) {
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState(null);
  const [r, setR] = useState(null);

  async function enviar(e) {
    e.preventDefault();
    const d = Object.fromEntries(new FormData(e.currentTarget).entries());
    setErro(null);
    setEnviando(true);
    try {
      setR(await api.post(`/api/publico/cursos/${curso.id}/matriculas`, { nome: d.nome, whatsapp: d.whatsapp, email: d.email || null, site: d.site || null }));
    } catch (err) { setErro(err.message); } finally { setEnviando(false); }
  }

  const link = r && `${window.location.origin}/meu-curso/${r.codigo}`;
  const texto = r && `Olá, Lenne! Me inscrevi no curso online "${r.curso}" e segue o comprovante do Pix de ${moeda(r.valor)}.`;

  return (
    <Modal titulo={!r ? 'Inscrição no curso' : r.status === 'LIBERADA' ? 'Pronto, já pode assistir!' : 'Falta só o Pix!'} aoFechar={aoFechar}>
      {!r ? (
        <form onSubmit={enviar} className="form-grade">
          <div className="inteiro cartao" style={{ background: 'var(--oliva-claro)', boxShadow: 'none', padding: 18 }}>
            <p className="forte" style={{ fontSize: 19 }}>{curso.titulo}</p>
            <p style={{ fontWeight: 700, color: 'var(--oliva-texto)' }}>{Number(curso.valor) > 0 ? `${moeda(curso.valor)} · acesso pelo link que você recebe no WhatsApp` : 'Gratuito'}</p>
          </div>
          {erro && <div className="aviso aviso-erro inteiro" role="alert">{erro}</div>}
          <div className="campo inteiro"><label htmlFor="c-nome">Seu nome</label><input id="c-nome" name="nome" className="entrada" required maxLength={140} autoComplete="name" /></div>
          <div className="campo"><label htmlFor="c-whats">Seu WhatsApp</label><input id="c-whats" name="whatsapp" className="entrada" required maxLength={20} inputMode="tel" autoComplete="tel" placeholder="(98) 90000-0000" /></div>
          <div className="campo"><label htmlFor="c-email">E-mail (opcional)</label><input id="c-email" name="email" type="email" className="entrada" maxLength={160} autoComplete="email" /></div>
          <div aria-hidden="true" style={{ position: 'absolute', left: -9999, width: 1, height: 1, overflow: 'hidden' }}><label>Site <input name="site" tabIndex={-1} autoComplete="off" /></label></div>
          <div className="inteiro modal-rodape" style={{ marginTop: 4 }}>
            <button type="button" className="btn btn-fantasma" onClick={aoFechar}>Voltar</button>
            <button type="submit" className="btn btn-oliva" disabled={enviando}>{enviando ? 'Enviando…' : 'Continuar'}</button>
          </div>
        </form>
      ) : r.status === 'LIBERADA' ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <p style={{ fontWeight: 600 }}>Guarde este link: é por ele que você assiste às aulas.</p>
          <a className="btn btn-terra" href={link}><Icone nome="play" tamanho={16} />Abrir o curso</a>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <ol className="passos" style={{ color: 'var(--marrom)' }}>
            <li><span>1</span><span>Faça um Pix de <strong>{moeda(r.valor)}</strong> para a chave abaixo.</span></li>
            <li><span>2</span><span>Envie o comprovante pelo WhatsApp no botão verde.</span></li>
            <li><span>3</span><span>Assim que a Lenne conferir, o seu link de acesso chega no WhatsApp.</span></li>
          </ol>
          <BlocoPix chave={r.chavePix} titular={r.titularPix} />
          <p className="mudo" style={{ fontSize: 14 }}>Você também pode acompanhar pelo link <a href={link}>da sua inscrição</a>.</p>
          <div className="modal-rodape">
            <button type="button" className="btn btn-fantasma" onClick={aoFechar}>Fechar</button>
            <a className="btn btn-oliva" href={linkWhats(r.whatsappAtelie, texto)} target="_blank" rel="noopener noreferrer"
              onClick={() => api.post(`/api/publico/matriculas/${r.codigo}/comprovante-enviado`).catch(() => {})}><Icone nome="whats" tamanho={18} />Enviar comprovante</a>
          </div>
        </div>
      )}
    </Modal>
  );
}
