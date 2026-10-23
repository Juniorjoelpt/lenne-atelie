import { useEffect, useMemo, useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { CATEGORIAS_PORTFOLIO, TIPOS_PECA, dataHora, hojeIso, linkWhats, moeda, telefone } from '../util.js';
import Inscricao from './Inscricao.jsx';

const SERVICOS = [
  { icone: 'encomenda', fundo: 'var(--oliva-claro)', caixa: 'var(--oliva)', texto: '#4A4030', titulo: 'Peças sob encomenda',
    desc: 'Painéis, cortinas, cabeceiras e peças decorativas nas medidas e cores do seu espaço.' },
  { icone: 'cliente', fundo: 'var(--terra-claro)', caixa: 'var(--terra-vivo)', texto: '#5A3A28', titulo: 'Casamentos e eventos',
    desc: 'Arcos, capas de cadeira e decoração em macramê para cerimônias ao ar livre ou no salão.' },
  { icone: 'portfolio', fundo: 'var(--ocre-claro)', caixa: 'var(--ocre)', texto: '#5A4520', titulo: 'Presentes e lembrancinhas',
    desc: 'Chaveiros, terços e kits personalizados com a tag do ateliê para datas especiais.' },
  { icone: 'oficina', fundo: 'var(--areia)', caixa: 'var(--cafe)', texto: '#4A3A2C', titulo: 'Oficinas presenciais',
    desc: 'Encontros em grupo para aprender macramê do zero, com material incluso e um café no ateliê.' },
];

export default function Site() {
  const [site, setSite] = useState(null);
  const [erro, setErro] = useState(null);
  const [oficinaEscolhida, setOficinaEscolhida] = useState(null);

  useEffect(() => {
    api.get('/api/publico/site').then(setSite).catch((e) => setErro(e.message));
  }, []);

  const atelie = site?.atelie || { nomeAtelie: 'Lenne Carvalho Ateliê', whatsapp: '5598988793891', instagram: 'lennecarvalhoatelie' };
  const fotos = site?.portfolio || [];
  const oficinas = site?.oficinas || [];

  return (
    <div className="site">
      <header className="envolve">
        <div className="cabecalho">
          <a href="#inicio" className="marca"><strong>Lenne Carvalho</strong><span>ateliê</span></a>
          <nav aria-label="Principal">
            <a href="#servicos">Serviços</a>
            <a href="#portfolio">Portfólio</a>
            <a href="#oficinas">Oficinas</a>
            <a href="#sobre">Quem sou</a>
            <a href="#orcamento" className="btn btn-terra">Pedir orçamento</a>
          </nav>
        </div>
      </header>

      <main>
        <section id="inicio" className="envolve" style={{ paddingTop: 28 }}>
          <div className="heroi">
            <div style={{ flex: '1 1 440px', minWidth: 0 }}>
              <p className="lema">Transformo fios em poesia</p>
              <h1>Macramê feito à mão, com significado, para espaços e pessoas.</h1>
              <p style={{ marginTop: 20, fontSize: 19, maxWidth: 520, color: '#5A3A28', fontWeight: 600 }}>
                Peças sob encomenda, decoração de casamentos, presentes personalizados e oficinas presenciais
                em {atelie.cidade || 'São Luís — MA'}.
              </p>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 14, marginTop: 30 }}>
                <a href="#orcamento" className="btn btn-terra"><Icone nome="orcamento" tamanho={18} />Fazer orçamento</a>
                <a href="#oficinas" className="btn btn-claro"><Icone nome="play" tamanho={16} cor="var(--oliva)" />Ver próximas oficinas</a>
              </div>
            </div>
            <div className="heroi-fotos" aria-hidden={fotos.length === 0}>
              {[0, 1, 2].map((i) => {
                const f = fotos[i];
                const fundos = ['#F6E2D3', 'var(--oliva-claro)', 'var(--ocre-claro)'];
                return f ? (
                  <div key={i}><img src={f.url} alt={f.titulo} loading={i ? 'lazy' : 'eager'} /></div>
                ) : (
                  <div key={i} style={{ background: fundos[i] }}><Decoracao indice={i} /></div>
                );
              })}
            </div>
          </div>
        </section>

        <section id="servicos" className="envolve secao">
          <p className="sobretitulo">O que eu faço</p>
          <h2 className="titulo-secao">Cada nó é pensado para quem vai receber</h2>
          <div className="servicos">
            {SERVICOS.map((s) => (
              <article key={s.titulo} className="servico" style={{ background: s.fundo }}>
                <div className="icone-caixa" style={{ background: s.caixa }}><Icone nome={s.icone} cor="#FFFDF8" tamanho={28} /></div>
                <h3>{s.titulo}</h3>
                <p style={{ color: s.texto }}>{s.desc}</p>
              </article>
            ))}
          </div>
        </section>

        <Portfolio fotos={fotos} instagram={atelie.instagram} />

        {site?.depoimentos?.length > 0 && <Depoimentos lista={site.depoimentos} />}

        <section id="oficinas" className="envolve secao">
          <div className="bloco" style={{ background: 'var(--oliva-menu)' }}>
            <p className="sobretitulo" style={{ color: 'var(--oliva-texto)' }}>Agenda</p>
            <h2 className="titulo-secao">Próximas oficinas</h2>
            <p style={{ marginTop: 8, maxWidth: 640, color: '#2E3518', fontWeight: 600 }}>
              Turmas pequenas, material incluso e você sai com a sua peça pronta. A vaga é confirmada após o Pix da inscrição.
            </p>
            <div className="oficinas-grade">
              {oficinas.map((o) => (
                <article key={o.id} className="oficina-card">
                  <span className="selo selo-terra clay-sm" style={{ alignSelf: 'flex-start' }}>{dataHora(o.inicio)}</span>
                  <h3 style={{ fontSize: 23, marginTop: 4 }}>{o.titulo}</h3>
                  {o.descricao && <p style={{ color: 'var(--marrom-2)', fontWeight: 600, whiteSpace: 'pre-line' }}>{o.descricao}</p>}
                  {o.local && <p style={{ fontSize: 15, fontWeight: 700 }}>{o.local}</p>}
                  <p style={{ fontSize: 15, fontWeight: 800, color: 'var(--oliva-texto)' }}>
                    {moeda(o.valor)} · {o.esgotada ? 'turma lotada' : `${o.vagasRestantes} ${o.vagasRestantes === 1 ? 'vaga' : 'vagas'}`}
                  </p>
                  <button type="button" className={`btn ${o.esgotada ? 'btn-claro' : 'btn-oliva'}`} style={{ alignSelf: 'flex-start', marginTop: 6 }}
                    onClick={() => setOficinaEscolhida(o)}>
                    {o.esgotada ? 'Entrar na lista de espera' : 'Quero minha vaga'}
                  </button>
                </article>
              ))}
              {site && oficinas.length === 0 && (
                <article className="oficina-card">
                  <h3 style={{ fontSize: 23 }}>Novas turmas em breve</h3>
                  <p style={{ color: 'var(--marrom-2)', fontWeight: 600 }}>Quer ser avisada quando abrir a próxima oficina?</p>
                  <a className="btn btn-oliva" style={{ alignSelf: 'flex-start' }} target="_blank" rel="noopener noreferrer"
                    href={linkWhats(atelie.whatsapp, 'Olá, Lenne! Quero ser avisada quando abrir a próxima oficina.')}>Me avise</a>
                </article>
              )}
              <article className="press" style={{ background: 'var(--oliva-claro)', borderRadius: 30, padding: 26, display: 'flex', flexDirection: 'column', gap: 14 }}>
                <h3 style={{ fontSize: 23 }}>Como garantir sua vaga</h3>
                <ol className="passos">
                  <li><span>1</span><span>Escolha a turma e faça sua inscrição aqui.</span></li>
                  <li><span>2</span><span>Faça o Pix da inscrição para a chave que aparece na tela.</span></li>
                  <li><span>3</span><span>Envie o comprovante pelo WhatsApp.</span></li>
                  <li><span>4</span><span>Pronto: você recebe a confirmação da vaga.</span></li>
                </ol>
              </article>
            </div>
          </div>
        </section>

        <Orcamento whatsapp={atelie.whatsapp} />

        <section id="sobre" className="envolve secao">
          <div className="bloco" style={{ background: 'var(--areia)', display: 'flex', flexWrap: 'wrap', gap: 40, alignItems: 'center' }}>
            <div className="clay-sm" style={{ flex: '0 1 280px', minWidth: 200, aspectRatio: '1', borderRadius: '50%', background: 'var(--creme)', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
              <span className="cursiva" style={{ fontSize: 96, lineHeight: 1 }} aria-hidden="true">Lenne</span>
            </div>
            <div style={{ flex: '1 1 440px', minWidth: 0 }}>
              <p className="sobretitulo">Quem sou</p>
              <h2 className="titulo-secao" style={{ marginBottom: 14 }}>Prazer, Lenne Carvalho!</h2>
              <p style={{ color: '#4A3A2C', fontWeight: 600, whiteSpace: 'pre-line' }}>{atelie.sobreTexto}</p>
            </div>
          </div>
        </section>

        <section className="envolve" style={{ paddingTop: 56 }} aria-labelledby="cursos-titulo">
          <div className="bloco" style={{ background: 'var(--oliva-medio)', display: 'flex', flexWrap: 'wrap', gap: 24, alignItems: 'center', padding: '30px 36px' }}>
            <div style={{ flex: '1 1 360px', minWidth: 0, color: '#2E3518' }}>
              <p className="sobretitulo" style={{ color: '#2E3518' }}>Em breve</p>
              <h2 id="cursos-titulo" style={{ fontSize: 30, margin: '2px 0 4px' }}>Cursos online de macramê</h2>
              <p style={{ fontWeight: 700 }}>Quer aprender de casa? Entre na lista e seja avisada primeiro.</p>
            </div>
            <a className="btn btn-oliva" target="_blank" rel="noopener noreferrer"
              href={linkWhats(atelie.whatsapp, 'Olá, Lenne! Quero ser avisada quando os cursos online abrirem.')}>Quero ser avisada</a>
          </div>
        </section>
      </main>

      <footer className="envolve rodape-site">
        <div className="bloco" style={{ background: 'var(--cru)', padding: '36px 40px 24px' }}>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 36, justifyContent: 'space-between' }}>
            <div style={{ flex: '1 1 260px' }}>
              <p className="marca"><strong>Lenne Carvalho</strong><span>ateliê</span></p>
              <p style={{ marginTop: 4, fontWeight: 600, color: 'var(--marrom-2)' }}>Macramê feito à mão · {atelie.cidade || 'São Luís — MA'}</p>
            </div>
            <div style={{ flex: '1 1 220px', display: 'flex', flexDirection: 'column', gap: 8 }}>
              <p className="forte">Contato</p>
              <a href={linkWhats(atelie.whatsapp)} target="_blank" rel="noopener noreferrer" className="forte">WhatsApp {telefone(atelie.whatsapp)}</a>
              {atelie.instagram && <a href={`https://www.instagram.com/${atelie.instagram}/`} target="_blank" rel="noopener noreferrer" className="forte">@{atelie.instagram}</a>}
            </div>
            {(atelie.endereco || atelie.horario) && (
              <div style={{ flex: '1 1 220px', display: 'flex', flexDirection: 'column', gap: 8, fontWeight: 600, color: 'var(--marrom-2)' }}>
                <p className="forte" style={{ color: 'var(--marrom)' }}>Ateliê</p>
                {atelie.endereco && <p>{atelie.endereco}</p>}
                {atelie.horario && <p style={{ whiteSpace: 'pre-line' }}>{atelie.horario}</p>}
              </div>
            )}
          </div>
          <p style={{ marginTop: 28, paddingTop: 18, borderTop: '2px dashed #E4D8C6', fontSize: 13, color: 'var(--marrom-3)', fontWeight: 700 }}>
            © {new Date().getFullYear()} Lenne Carvalho Ateliê · Site por MS Soluções Inteligentes
          </p>
        </div>
      </footer>

      {erro && <div className="toast toast-erro" role="alert">Não foi possível carregar as oficinas agora.</div>}
      {oficinaEscolhida && <Inscricao oficina={oficinaEscolhida} aoFechar={() => setOficinaEscolhida(null)} />}
    </div>
  );
}

function Decoracao({ indice }) {
  // Desenho de macramê em traço, usado enquanto não há fotos no portfólio.
  const cores = ['#C9845F', '#7C8A55', '#C99A3E'];
  return (
    <svg width="60%" viewBox="0 0 120 150" aria-hidden="true">
      <rect x="14" y="10" width="92" height="8" rx="4" fill="#B98A5E" />
      <g stroke="#FBF6EE" strokeWidth="5" strokeLinecap="round" fill="none">
        <path d="M28 18v34l16 18 16-18 16 18 16-18V18" />
        <path d="M44 18v26l16 18 16-18V18M60 18v28" />
        <path d="M44 70v40M60 76v50M76 70v40M28 52v40M92 52v40" />
      </g>
      <circle cx="60" cy="62" r="6" fill={cores[indice]} />
    </svg>
  );
}

function Depoimentos({ lista }) {
  return (
    <section id="depoimentos" className="envolve secao" aria-labelledby="depo-titulo">
      <p className="sobretitulo">Depoimentos</p>
      <h2 id="depo-titulo" className="titulo-secao" style={{ marginBottom: 24 }}>Quem já levou um pedacinho do ateliê</h2>
      <div className="depo-site">
        {lista.map((d) => (
          <figure key={d.id} className="depo-card">
            <span className="estrelas" role="img" aria-label={`${d.estrelas} de 5 estrelas`}>
              {[1, 2, 3, 4, 5].map((i) => <Icone key={i} nome="estrela" tamanho={18} className={i > d.estrelas ? 'apagada' : ''} style={{ fill: i <= d.estrelas ? 'currentColor' : 'none' }} />)}
            </span>
            <blockquote>{d.texto}</blockquote>
            <figcaption className="depo-autor">
              {d.foto ? <img className="foto" src={d.foto} alt="" loading="lazy" /> : <span className="foto" aria-hidden="true">{d.nome.charAt(0)}</span>}
              <div><p className="forte">{d.nome}</p>{d.contexto && <p>{d.contexto}</p>}</div>
            </figcaption>
          </figure>
        ))}
      </div>
    </section>
  );
}

function Portfolio({ fotos, instagram }) {
  const [categoria, setCategoria] = useState('TODOS');
  const categorias = useMemo(() => {
    const usadas = new Set(fotos.map((f) => f.categoria));
    return Object.entries(CATEGORIAS_PORTFOLIO).filter(([k]) => usadas.has(k));
  }, [fotos]);
  const lista = categoria === 'TODOS' ? fotos : fotos.filter((f) => f.categoria === categoria);

  return (
    <section id="portfolio" className="envolve secao">
      <div className="bloco" style={{ background: 'var(--cru)' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-end', gap: 20, marginBottom: 28 }}>
          <div>
            <p className="sobretitulo">Portfólio</p>
            <h2 className="titulo-secao">Trabalhos recentes</h2>
          </div>
          {categorias.length > 1 && (
            <div className="chips" role="group" aria-label="Filtrar portfólio">
              {[['TODOS', 'Todos'], ...categorias].map(([k, r]) => (
                <button key={k} type="button" className="chip" aria-pressed={categoria === k} onClick={() => setCategoria(k)}>{r}</button>
              ))}
            </div>
          )}
        </div>
        {fotos.length === 0 ? (
          <p className="mudo forte">
            As fotos dos trabalhos chegam aqui em breve. Enquanto isso, veja tudo no{' '}
            <a href={`https://www.instagram.com/${instagram || 'lennecarvalhoatelie'}/`} target="_blank" rel="noopener noreferrer">Instagram</a>.
          </p>
        ) : (
          <div className="galeria">
            {lista.map((f) => (
              <figure key={f.id}>
                <img src={f.url} alt={f.titulo} loading="lazy" />
                <figcaption>
                  <span>{f.titulo}</span>
                  <span className="selo selo-oliva">{CATEGORIAS_PORTFOLIO[f.categoria]}</span>
                </figcaption>
              </figure>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

function Orcamento({ whatsapp }) {
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState(null);
  const [enviado, setEnviado] = useState(null);

  async function enviar(e) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const d = Object.fromEntries(fd.entries());
    setErro(null);
    setEnviando(true);
    try {
      await api.post('/api/publico/orcamentos', {
        nome: d.nome, whatsapp: d.whatsapp, tipoPeca: d.tipoPeca, medidas: d.medidas || null,
        cores: d.cores || null, dataDesejada: d.dataDesejada || null, detalhes: d.detalhes || null, site: d.site || null,
      });
      const linhas = [
        'Olá, Lenne! Acabei de pedir um orçamento pelo site.',
        `Nome: ${d.nome}`,
        `Peça: ${d.tipoPeca}`,
        d.medidas && `Medidas/quantidade: ${d.medidas}`,
        d.dataDesejada && `Para: ${d.dataDesejada.split('-').reverse().join('/')}`,
        d.cores && `Cores: ${d.cores}`,
        d.detalhes && `Detalhes: ${d.detalhes}`,
      ].filter(Boolean);
      setEnviado(linkWhats(whatsapp, linhas.join('\n')));
      e.target.reset();
    } catch (err) {
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section id="orcamento" className="envolve secao" style={{ display: 'flex', flexWrap: 'wrap', gap: 28, alignItems: 'stretch' }}>
      <div className="bloco" style={{ flex: '1 1 340px', minWidth: 0, background: 'var(--terra-claro)', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', gap: 24 }}>
        <div>
          <p className="sobretitulo" style={{ color: 'var(--terra-texto)' }}>Orçamento</p>
          <h2 className="titulo-secao" style={{ marginBottom: 14 }}>Conte o que você imagina</h2>
          <p style={{ color: '#5A3A28', fontWeight: 600 }}>
            Preencha os detalhes e eu recebo seu pedido organizado. Depois é só continuar a conversa no WhatsApp,
            onde você pode mandar fotos de referência.
          </p>
        </div>
        <p className="cursiva" style={{ fontSize: 40, lineHeight: 1.1 }}>Artesanato que abraça</p>
      </div>

      <div className="bloco" style={{ flex: '1 1 520px', minWidth: 0, background: 'var(--cru)' }}>
        {enviado ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16, alignItems: 'flex-start' }} role="status">
            <div className="icone-caixa" style={{ background: 'var(--oliva)' }}><Icone nome="check" cor="#fff" tamanho={28} largura={3} /></div>
            <h3 style={{ fontSize: 26 }}>Pedido recebido!</h3>
            <p style={{ fontWeight: 600, color: 'var(--marrom-2)' }}>
              Já anotei tudo por aqui. Para agilizar, continue no WhatsApp e mande uma foto de referência, se tiver.
            </p>
            <a className="btn btn-oliva" href={enviado} target="_blank" rel="noopener noreferrer"><Icone nome="whats" tamanho={20} />Continuar no WhatsApp</a>
            <button type="button" className="btn btn-fantasma" onClick={() => setEnviado(null)}>Fazer outro pedido</button>
          </div>
        ) : (
          <form onSubmit={enviar} className="form-grade">
            {erro && <div className="aviso aviso-erro inteiro" role="alert">{erro}</div>}
            <CampoSite rotulo="Seu nome" nome="nome" required maxLength={140} autoComplete="name" inteiro />
            <CampoSite rotulo="Seu WhatsApp" nome="whatsapp" required maxLength={20} inputMode="tel" autoComplete="tel" placeholder="(98) 90000-0000" />
            <div className="campo">
              <label htmlFor="o-tipo">Tipo de peça</label>
              <select id="o-tipo" name="tipoPeca" className="entrada" required>
                {TIPOS_PECA.map((t) => <option key={t}>{t}</option>)}
              </select>
            </div>
            <CampoSite rotulo="Medidas ou quantidade" nome="medidas" maxLength={160} placeholder="ex.: 1,20 × 0,80 m" />
            <CampoSite rotulo="Para quando?" nome="dataDesejada" type="date" min={hojeIso()} />
            <CampoSite rotulo="Cores desejadas" nome="cores" maxLength={160} placeholder="ex.: cru com detalhes em terracota" inteiro />
            <div className="campo inteiro">
              <label htmlFor="o-det">Detalhes</label>
              <textarea id="o-det" name="detalhes" rows={3} maxLength={2000} className="entrada" placeholder="Onde a peça vai ficar, estilo, ocasião…" />
            </div>
            {/* Campo escondido contra robôs */}
            <div aria-hidden="true" style={{ position: 'absolute', left: -9999, width: 1, height: 1, overflow: 'hidden' }}>
              <label>Site <input name="site" tabIndex={-1} autoComplete="off" /></label>
            </div>
            <button type="submit" className="btn btn-oliva inteiro" disabled={enviando} style={{ minHeight: 54, fontSize: 18 }}>
              <Icone nome="whats" tamanho={20} />{enviando ? 'Enviando…' : 'Enviar pedido de orçamento'}
            </button>
          </form>
        )}
      </div>
    </section>
  );
}

function CampoSite({ rotulo, nome, inteiro, ...resto }) {
  const id = `o-${nome}`;
  return (
    <div className={`campo ${inteiro ? 'inteiro' : ''}`}>
      <label htmlFor={id}>{rotulo}</label>
      <input id={id} name={nome} className="entrada" {...resto} />
    </div>
  );
}
