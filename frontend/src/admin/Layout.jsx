import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { api, sessao } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { primeiroNome } from '../util.js';

const MENU = [
  ['/admin', 'painel', 'Painel', true],
  ['/admin/orcamentos', 'orcamento', 'Orçamentos'],
  ['/admin/encomendas', 'encomenda', 'Encomendas'],
  ['/admin/produtos', 'etiqueta', 'Produtos'],
  ['/admin/vendas', 'sacola', 'Vendas'],
  ['/admin/agenda', 'calendario', 'Agenda'],
  ['/admin/oficinas', 'oficina', 'Oficinas'],
  ['/admin/clientes', 'cliente', 'Clientes'],
  ['/admin/financeiro', 'financeiro', 'Financeiro'],
  ['/admin/relatorios', 'grafico', 'Relatórios'],
  ['/admin/materiais', 'material', 'Materiais'],
  ['/admin/portfolio', 'portfolio', 'Portfólio'],
  ['/admin/configuracoes', 'config', 'Configurações'],
];

export default function Layout() {
  const [aberto, setAberto] = useState(false);
  const [pix, setPix] = useState(0);
  const [lembretes, setLembretes] = useState(0);
  const local = useLocation();
  const ir = useNavigate();
  const nome = sessao.nome();

  useEffect(() => {
    setAberto(false);
    api.get('/api/inscricoes/pix-a-conferir').then((l) => setPix(l.length)).catch(() => {});
    api.get('/api/lembretes').then((l) => setLembretes(l.length)).catch(() => {});
  }, [local.pathname]);

  function sair() {
    sessao.sair();
    ir('/admin/entrar');
  }

  return (
    <div className="app">
      {aberto && <div className="fundo-menu" onClick={() => setAberto(false)} />}
      <aside className={`lateral ${aberto ? 'aberta' : ''}`} aria-label="Menu do sistema">
        <div className="lateral-perfil">
          <div className="avatar" aria-hidden="true">{(nome || 'L').charAt(0)}</div>
          <p style={{ fontSize: 22, fontWeight: 900 }}>Olá, {primeiroNome(nome) || 'Lenne'}!</p>
        </div>
        <nav>
          {MENU.map(([para, icone, rotulo, fim]) => (
            <NavLink key={para} to={para} end={fim}>
              <Icone nome={icone} />{rotulo}
              {para === '/admin/oficinas' && pix > 0 && <span className="contador" aria-label={`${pix} Pix a conferir`}>{pix}</span>}
            </NavLink>
          ))}
        </nav>
        {lembretes > 0 && (
          <Link to="/admin/lembretes" className="clay" style={{ background: 'var(--ocre-claro)', borderRadius: 26, padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 12, textDecoration: 'none', color: 'var(--marrom)' }}>
            <Icone nome="sino" tamanho={26} cor="var(--ocre-texto)" />
            <span style={{ flex: 1, fontWeight: 800, lineHeight: 1.25 }}>{lembretes} {lembretes === 1 ? 'lembrete' : 'lembretes'} para enviar hoje</span>
            <Icone nome="seta" tamanho={18} />
          </Link>
        )}
        {pix > 0 && (
          <div className="clay" style={{ background: 'var(--terra-claro)', borderRadius: 30, padding: 20, textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
            <Icone nome="pix" tamanho={34} cor="var(--terra)" />
            <p style={{ fontWeight: 900, fontSize: 18 }}>Pix a conferir</p>
            <p style={{ fontSize: 14, color: 'var(--marrom-2)' }}>{pix} {pix === 1 ? 'inscrição aguardando' : 'inscrições aguardando'} sua conferência</p>
            <Link to="/admin/oficinas?pix=1" className="btn btn-terra btn-p" style={{ marginTop: 6 }}>Conferir agora</Link>
          </div>
        )}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4, marginTop: 'auto' }}>
          <a href="/" target="_blank" rel="noopener noreferrer" className="btn btn-fantasma" style={{ justifyContent: 'flex-start' }}><Icone nome="site" />Ver o site</a>
          <button type="button" className="btn btn-fantasma" style={{ justifyContent: 'flex-start' }} onClick={sair}><Icone nome="sair" />Sair</button>
        </div>
      </aside>
      <main className="conteudo">
        <button type="button" className="btn btn-claro menu-movel" style={{ alignSelf: 'flex-start' }} onClick={() => setAberto(true)} aria-label="Abrir menu">
          <Icone nome="menu" />Menu
        </button>
        <Outlet />
      </main>
    </div>
  );
}

/** Título da página com ações à direita. */
export function Topo({ titulo, children }) {
  useEffect(() => { document.title = `${titulo} · Ateliê`; }, [titulo]);
  return (
    <div className="topo">
      <h1>{titulo}</h1>
      {children}
    </div>
  );
}
