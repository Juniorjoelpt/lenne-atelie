import { SemImagem } from '../componentes/Marca.jsx';
import { useEffect, useMemo, useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Modal } from '../componentes/ui.jsx';
import { FRENTES, linkWhats, moeda } from '../util.js';
import Cabecalho from './Cabecalho.jsx';

const FILTROS = [['TODAS', 'Todas'], ['PRONTA', 'Pronta-entrega'], ['ENCOMENDA', 'Decoração'], ['CASAMENTO', 'Casamentos'], ['PRESENTE', 'Presentes']];

export function precoTexto(i) {
  if (i.preco == null) return 'Sob consulta';
  return i.prontaEntrega ? moeda(i.preco) : `a partir de ${moeda(i.preco)}`;
}

export function mensagemPeca(i) {
  return i.prontaEntrega && i.disponiveis > 0
    ? `Olá, Lenne! Vi no catálogo a peça "${i.nome}" e quero comprar. Ainda tem disponível?`
    : `Olá, Lenne! Vi no catálogo a peça "${i.nome}" e quero fazer uma encomenda. Pode me passar os detalhes?`;
}

/** Cartão da peça (catálogo e página inicial). */
export function CartaoPeca({ i, aoAbrir }) {
  return (
    <button type="button" className="peca-card" onClick={aoAbrir} aria-label={`${i.nome}, ${precoTexto(i)}. Ver detalhes`}>
      <span className="peca-foto">
        {i.fotos[0] ? <img src={i.fotos[0]} alt="" loading="lazy" /> : <SemImagem />}
        {i.prontaEntrega && i.disponiveis > 0 && <span className="selo selo-oliva peca-selo">Pronta-entrega</span>}
      </span>
      <span className="peca-info">
        <span className="peca-nome">{i.nome}</span>
        <span className="peca-preco">{precoTexto(i)}</span>
      </span>
    </button>
  );
}

/** Detalhe da peça com galeria e botão para o WhatsApp. */
export function DetalhePeca({ i, whatsapp, aoFechar }) {
  const [foto, setFoto] = useState(0);
  return (
    <Modal titulo={i.nome} aoFechar={aoFechar} largo>
      <div className="peca-detalhe">
        <div style={{ minWidth: 0 }}>
          <div className="peca-foto grande">
            {i.fotos[foto] ? <img src={i.fotos[foto]} alt={`${i.nome}, foto ${foto + 1}`} /> : <SemImagem />}
          </div>
          {i.fotos.length > 1 && (
            <div className="miniaturas" role="group" aria-label="Fotos da peça">
              {i.fotos.map((f, n) => (
                <button key={f} type="button" aria-pressed={n === foto} aria-label={`Foto ${n + 1}`} onClick={() => setFoto(n)}><img src={f} alt="" /></button>
              ))}
            </div>
          )}
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, minWidth: 0 }}>
          <span className="selo selo-areia" style={{ alignSelf: 'flex-start' }}>{FRENTES[i.frente] === 'Encomenda' ? 'Decoração' : FRENTES[i.frente]}</span>
          <p style={{ fontSize: 28, fontWeight: 900 }}>{precoTexto(i)}</p>
          {i.texto && <p style={{ whiteSpace: 'pre-line', fontWeight: 600, color: '#4A3A2C' }}>{i.texto}</p>}
          <ul className="lista-check">
            {i.prontaEntrega && i.disponiveis > 0
              ? <li><Icone nome="check" tamanho={16} largura={3} />Pronta-entrega: {i.disponiveis} {i.disponiveis === 1 ? 'disponível' : 'disponíveis'}</li>
              : <li><Icone nome="check" tamanho={16} largura={3} />Feita sob encomenda, nas cores e medidas que você escolher</li>}
            {i.prazoProducao && !(i.prontaEntrega && i.disponiveis > 0) && <li><Icone nome="check" tamanho={16} largura={3} />Prazo de produção: {i.prazoProducao.charAt(0).toLowerCase() + i.prazoProducao.slice(1)}</li>}
            <li><Icone nome="check" tamanho={16} largura={3} />Feita à mão, peça a peça</li>
          </ul>
          <a className="btn btn-oliva" style={{ alignSelf: 'flex-start', minHeight: 52 }} target="_blank" rel="noopener noreferrer" href={linkWhats(whatsapp, mensagemPeca(i))}>
            <Icone nome="whats" tamanho={18} />{i.prontaEntrega && i.disponiveis > 0 ? 'Quero comprar' : 'Quero encomendar'}
          </a>
        </div>
      </div>
    </Modal>
  );
}

/** Página com todas as peças que o ateliê produz. */
export default function Catalogo() {
  const [dados, setDados] = useState(null);
  const [erro, setErro] = useState(null);
  const [filtro, setFiltro] = useState('TODAS');
  const [aberta, setAberta] = useState(null);

  useEffect(() => {
    document.title = 'Catálogo · Lenne Carvalho Ateliê';
    api.get('/api/publico/site').then(setDados).catch((e) => setErro(e.message));
  }, []);

  const itens = dados?.catalogo || [];
  const filtros = FILTROS.filter(([k]) => k === 'TODAS' || (k === 'PRONTA' ? itens.some((i) => i.prontaEntrega && i.disponiveis > 0) : itens.some((i) => i.frente === k)));
  const lista = useMemo(() => itens.filter((i) => filtro === 'TODAS' || (filtro === 'PRONTA' ? i.prontaEntrega && i.disponiveis > 0 : i.frente === filtro)), [itens, filtro]);
  const whatsapp = dados?.atelie?.whatsapp;

  return (
    <div className="site">
      <Cabecalho />
      <main className="envolve" style={{ paddingTop: 28, paddingBottom: 56, display: 'flex', flexDirection: 'column', gap: 22 }}>
        <div>
          <p className="sobretitulo">Catálogo</p>
          <h1 className="titulo-secao">Peças feitas no ateliê</h1>
          <p style={{ marginTop: 8, maxWidth: 680, fontWeight: 600, color: 'var(--marrom-2)' }}>
            Toque em uma peça para ver as fotos e falar com a Lenne. Quase tudo pode ser feito nas cores e medidas do seu espaço.
          </p>
        </div>
        {filtros.length > 2 && (
          <div className="chips" role="group" aria-label="Filtrar peças" style={{ alignSelf: 'flex-start' }}>
            {filtros.map(([k, r]) => <button key={k} type="button" className="chip" aria-pressed={filtro === k} onClick={() => setFiltro(k)}>{r}</button>)}
          </div>
        )}
        {erro && <div className="aviso aviso-erro" role="alert">Não foi possível carregar o catálogo agora.</div>}
        {!erro && !dados && <p className="carregando">Carregando…</p>}
        {dados && lista.length === 0 && <div className="aviso aviso-info">Em breve as peças aparecem aqui. Enquanto isso, chame a Lenne no WhatsApp.</div>}
        <div className="pecas-grade">
          {lista.map((i) => <CartaoPeca key={i.id} i={i} aoAbrir={() => setAberta(i)} />)}
        </div>
        {whatsapp && (
          <div className="bloco" style={{ background: 'var(--terra-claro)', display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 18, padding: '24px 30px' }}>
            <p style={{ flex: '1 1 300px', fontWeight: 800, fontSize: 18 }}>Não achou o que procurava? A Lenne cria peças exclusivas para você.</p>
            <a className="btn btn-terra" href="/#orcamento">Pedir orçamento</a>
          </div>
        )}
      </main>
      {aberta && <DetalhePeca i={aberta} whatsapp={whatsapp} aoFechar={() => setAberta(null)} />}
    </div>
  );
}
