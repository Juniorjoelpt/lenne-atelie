import { Link } from 'react-router-dom';
import { api, sessao } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Estado, Selo, useCarregar, Vazio } from '../componentes/ui.jsx';
import { CORES_GRAFICO, STATUS_ENCOMENDA, dataCurta, dataHora, moeda, moedaCurta, primeiroNome } from '../util.js';
import { Topo } from './Layout.jsx';

export default function Painel() {
  const { dados: p, erro, carregando } = useCarregar(() => api.get('/api/painel'));

  return (
    <>
      <Topo titulo="Painel">
        <Link to="/admin/orcamentos?novo=1" className="btn btn-terra"><Icone nome="mais" tamanho={18} largura={3} />Novo orçamento</Link>
      </Topo>
      <Estado carregando={carregando} erro={erro}>
        {p && <Conteudo p={p} />}
      </Estado>
    </>
  );
}

function Conteudo({ p }) {
  const frases = [];
  if (p.orcamentosNovos) frases.push(<strong key="o">{p.orcamentosNovos} {p.orcamentosNovos === 1 ? 'orçamento novo' : 'orçamentos novos'}</strong>);
  if (p.entregasSemana) frases.push(<strong key="e">{p.entregasSemana} {p.entregasSemana === 1 ? 'entrega' : 'entregas'} nos próximos 7 dias</strong>);
  const of = p.proximaOficina;
  const maxSemana = Math.max(1, ...p.recebidoPorSemana.map((b) => Number(b.valor)));
  const topo = Math.ceil(maxSemana / 500) * 500;

  let acumulado = 0;
  const fatias = p.receitaPorFrente.map((f, i) => {
    const de = acumulado;
    acumulado += f.percentual;
    return `${CORES_GRAFICO[i % CORES_GRAFICO.length]} ${de}% ${acumulado}%`;
  });

  return (
    <>
      <section className="boas-vindas" aria-label="Boas-vindas">
        <div style={{ flex: '1 1 300px', minWidth: 0 }}>
          <p style={{ fontSize: 30, fontWeight: 900, color: 'var(--terra-escuro)' }}>{p.saudacao}, {primeiroNome(sessao.nome()) || 'Lenne'}!</p>
          <p style={{ marginTop: 6, fontSize: 18, color: '#5A3A28' }}>
            {frases.length === 0 ? 'Tudo em dia por aqui. Bom trabalho!'
              : <>Você tem {frases.reduce((a, f, i) => (i === 0 ? [f] : [...a, ' e ', f]), [])}.</>}
          </p>
        </div>
        <svg width="110" height="140" viewBox="0 0 120 150" aria-hidden="true">
          <rect x="14" y="10" width="92" height="8" rx="4" fill="#B98A5E" />
          <g stroke="#FBF6EE" strokeWidth="4" strokeLinecap="round" fill="none">
            <path d="M28 18v34l16 18 16-18 16 18 16-18V18" /><path d="M44 18v26l16 18 16-18V18M60 18v28" />
            <path d="M44 70v40M60 76v50M76 70v40M28 52v40M92 52v40" />
          </g>
          <circle cx="60" cy="62" r="6" fill="#C9845F" />
        </svg>
      </section>

      <section className="grade-resumo" aria-label="Resumo">
        <Resumo para="/admin/orcamentos" fundo="var(--oliva-claro)" caixa="var(--oliva)" icone="orcamento" rotulo="Orçamentos abertos"
          valor={p.orcamentosAbertos} extra={`${p.orcamentosNovos} aguardando resposta`} corExtra="var(--oliva-texto)" />
        <Resumo para="/admin/encomendas" fundo="var(--terra-claro)" caixa="var(--terra-vivo)" icone="encomenda" rotulo="Em produção"
          valor={p.emProducao} extra={`${p.entregasSemana} entregas em 7 dias`} corExtra="var(--terra-texto)" />
        <Resumo para="/admin/financeiro" fundo="var(--ocre-claro)" caixa="var(--ocre)" icone="dinheiro" rotulo="A receber"
          valor={moeda(p.aReceber)} extra={`saldo de ${p.contasAReceber} ${p.contasAReceber === 1 ? 'encomenda' : 'encomendas'}`} corExtra="var(--ocre-texto)" />
        <Resumo para="/admin/oficinas" fundo="var(--areia)" caixa="var(--cafe)" icone="oficina" rotulo="Próxima oficina"
          valor={of ? `${of.vagas - of.vagasRestantes}/${of.vagas}` : '—'}
          extra={of ? `vagas ocupadas · ${dataCurta(of.inicio)}` : 'nenhuma turma aberta'} corExtra="var(--marrom-2)" />
      </section>

      <div className="grade-2">
        <section className="cartao" aria-labelledby="t-sem">
          <div className="cartao-titulo">
            <h2 id="t-sem">Recebido por semana</h2>
            <span className="selo selo-areia">{p.mesReferencia}</span>
          </div>
          <div className="barras" role="img" aria-label={`Recebido por semana em ${p.mesReferencia}: ${p.recebidoPorSemana.map((b) => `${b.rotulo} ${moeda(b.valor)}`).join(', ')}`}>
            <div className="barras-eixo"><span>{moedaCurta(topo)}</span><span>{moedaCurta(topo / 2)}</span><span>0</span></div>
            <div className="barras-area">
              {p.recebidoPorSemana.map((b, i) => (
                <div key={b.rotulo} className="barra-col" title={moeda(b.valor)}>
                  <div className="barra" style={{ height: `${(Number(b.valor) / topo) * 150}px`, background: CORES_GRAFICO[i] }} />
                  <span>{b.rotulo}</span>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="cartao" aria-labelledby="t-frente">
          <div className="cartao-titulo"><h2 id="t-frente">Receita por frente</h2><span className="selo selo-areia">{p.mesReferencia}</span></div>
          {p.receitaPorFrente.length === 0 ? <Vazio>Nenhuma entrada registrada neste mês.</Vazio> : (
            <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 28 }}>
              <div className="rosca" role="img" aria-label={p.receitaPorFrente.map((f) => `${f.nome} ${f.percentual}%`).join(', ')}
                style={{ background: `conic-gradient(${fatias.join(', ')})` }}>
                <div className="rosca-miolo" />
              </div>
              <ul className="legenda">
                {p.receitaPorFrente.map((f, i) => (
                  <li key={f.nome}><span className="bolinha" style={{ background: CORES_GRAFICO[i % CORES_GRAFICO.length] }} />
                    <span style={{ flex: 1 }}>{f.nome}</span><span>{f.percentual}%</span></li>
                ))}
              </ul>
            </div>
          )}
        </section>

        <section className="cartao" aria-labelledby="t-ent">
          <div className="cartao-titulo"><h2 id="t-ent">Próximas entregas</h2><Link to="/admin/encomendas" className="btn btn-claro btn-p">Ver todas</Link></div>
          {p.proximasEntregas.length === 0 ? <Vazio>Nenhuma entrega com prazo marcado.</Vazio> : (
            <ul className="lista-linhas">
              {p.proximasEntregas.map((e) => (
                <li key={e.id}>
                  <div className="icone-caixa" style={{ background: 'var(--terra-claro)', width: 46, height: 46 }}><Icone nome="encomenda" cor="var(--terra)" /></div>
                  <div className="info"><p>{e.descricao}</p><p>{e.clienteNome} · {dataCurta(e.prazoEntrega)}</p></div>
                  <Selo mapa={STATUS_ENCOMENDA} valor={e.status} />
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="cartao" aria-labelledby="t-of" style={{ background: 'var(--oliva-menu)' }}>
          <div className="cartao-titulo"><h2 id="t-of">Próxima oficina</h2><Link to="/admin/oficinas" className="btn btn-claro btn-p">Abrir</Link></div>
          {!of ? <Vazio>Nenhuma oficina aberta. Que tal marcar a próxima?</Vazio> : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div>
                <p style={{ fontSize: 22, fontWeight: 900 }}>{of.titulo}</p>
                <p style={{ fontWeight: 700, color: 'var(--oliva-texto)' }}>{dataHora(of.inicio)}{of.local ? ` · ${of.local}` : ''}</p>
              </div>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 14, fontWeight: 800 }}>
                  <span>Vagas</span><span>{of.vagas - of.vagasRestantes} de {of.vagas}</span>
                </div>
                <div className="progresso" style={{ marginTop: 8 }}><div style={{ width: `${((of.vagas - of.vagasRestantes) / of.vagas) * 100}%` }} /></div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 10 }}>
                {[[of.confirmadas, 'Confirmadas'], [of.aConferir + of.aguardandoPix, 'Aguardando Pix'], [of.listaEspera, 'Lista de espera']].map(([n, r]) => (
                  <div key={r} className="clay-sm" style={{ background: 'var(--creme)', borderRadius: 18, padding: 12, textAlign: 'center' }}>
                    <p style={{ fontSize: 22, fontWeight: 900 }}>{n}</p><p style={{ fontSize: 13, fontWeight: 700 }}>{r}</p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </section>
      </div>

      {p.materiaisBaixos.length > 0 && (
        <section className="clay" aria-label="Aviso de estoque" style={{ background: 'var(--oliva-medio)', borderRadius: 32, padding: '22px 30px', display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 20 }}>
          <div className="icone-caixa" style={{ background: 'var(--ocre-claro)' }}><Icone nome="alerta" cor="var(--ocre-texto)" tamanho={28} /></div>
          <div style={{ flex: '1 1 260px', minWidth: 0, color: '#2E3518' }}>
            <p style={{ fontSize: 20, fontWeight: 900 }}>
              {p.materiaisBaixos.length === 1 ? `${p.materiaisBaixos[0].nome} está acabando` : `${p.materiaisBaixos.length} materiais estão acabando`}
            </p>
            <p style={{ fontWeight: 700 }}>{p.materiaisBaixos.map((m) => `${m.nome}: restam ${Number(m.quantidade).toLocaleString('pt-BR')} (${m.unidade}), mínimo ${Number(m.estoqueMinimo).toLocaleString('pt-BR')}`).join(' · ')}</p>
          </div>
          <Link to="/admin/materiais" className="btn btn-oliva">Ver materiais</Link>
        </section>
      )}
    </>
  );
}

function Resumo({ para, fundo, caixa, icone, rotulo, valor, extra, corExtra }) {
  return (
    <Link to={para} className="resumo" style={{ background: fundo }}>
      <div className="icone-caixa" style={{ background: caixa }}><Icone nome={icone} cor="#FFFDF8" tamanho={26} /></div>
      <p className="rotulo">{rotulo}</p>
      <p className="valor">{valor}</p>
      <p className="extra" style={{ color: corExtra }}>{extra}</p>
    </Link>
  );
}
