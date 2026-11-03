import { useEffect, useMemo, useState } from 'react';
import { api } from '../api.js';
import Icone from './Icone.jsx';
import { FATOR_PADRAO, NOS_COMUNS, calcularFio, fmtCm, metrosPorUnidade, unidadeEhMetro } from '../calcFio.js';
import { moeda, qtd } from '../util.js';

const fator = (n) => `${Number(n).toLocaleString('pt-BR', { maximumFractionDigits: 2 })}×`;

/** Lista e cadastro das amostras de nó de um material. */
export function AmostrasFio({ material, aoMudar, compacto = false }) {
  const [lista, setLista] = useState(null);
  const [erro, setErro] = useState(null);
  const [novo, setNovo] = useState({ nomeNo: NOS_COMUNS[0], fioCm: '100', rendeuCm: '', observacao: '' });
  const [enviando, setEnviando] = useState(false);

  const carregar = () => api.get(`/api/materiais/${material.id}/amostras`).then((l) => { setLista(l); aoMudar?.(l); }).catch((e) => setErro(e.message));
  useEffect(() => { carregar(); }, [material.id]); // eslint-disable-line react-hooks/exhaustive-deps

  async function salvar(e) {
    e.preventDefault();
    setErro(null);
    setEnviando(true);
    try {
      await api.post(`/api/materiais/${material.id}/amostras`, {
        nomeNo: novo.nomeNo, fioCm: Number(novo.fioCm), rendeuCm: Number(novo.rendeuCm), observacao: novo.observacao || null,
      });
      setNovo((n) => ({ ...n, rendeuCm: '', observacao: '' }));
      await carregar();
    } catch (err) {
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  async function excluir(a) {
    await api.del(`/api/materiais/amostras/${a.id}`);
    carregar();
  }

  const previa = Number(novo.fioCm) > 0 && Number(novo.rendeuCm) > 0 ? Number(novo.fioCm) / Number(novo.rendeuCm) : null;

  return (
    <div className="amostras">
      {!compacto && (
        <p className="mudo" style={{ fontSize: 14 }}>
          Corte um fio de tamanho conhecido (100 cm é fácil), faça o nó do jeito de sempre e meça quanto de trabalho ele rendeu.
          O sistema guarda o seu fator e usa nas contas de fio das fichas.
        </p>
      )}
      {lista && lista.length > 0 && (
        <ul className="amostras-lista">
          {lista.map((a) => (
            <li key={a.id}>
              <span className="amostra-fator">{fator(a.fator)}</span>
              <span className="info"><strong>{a.nomeNo}</strong><span className="mudo">{fmtCm(a.fioCm)} de fio renderam {fmtCm(a.rendeuCm)}{a.observacao ? ` · ${a.observacao}` : ''}</span></span>
              <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Excluir amostra ${a.nomeNo}`} onClick={() => excluir(a)}><Icone nome="lixo" tamanho={16} /></button>
            </li>
          ))}
        </ul>
      )}
      {lista && lista.length === 0 && <p className="mudo forte" style={{ fontSize: 14 }}>Nenhuma amostra ainda.</p>}
      <form className={`amostra-form ${compacto ? 'amostra-form-compacto' : ''}`} onSubmit={salvar}>
        <div className="campo">
          <label htmlFor={`am-no-${material.id}`}>Nó</label>
          <input id={`am-no-${material.id}`} className="entrada" list={`nos-${material.id}`} value={novo.nomeNo} maxLength={80} required
            onChange={(e) => setNovo({ ...novo, nomeNo: e.target.value })} />
          <datalist id={`nos-${material.id}`}>{NOS_COMUNS.map((n) => <option key={n} value={n} />)}</datalist>
        </div>
        <div className="campo">
          <label htmlFor={`am-fio-${material.id}`}>Fio usado (cm)</label>
          <input id={`am-fio-${material.id}`} className="entrada" type="number" min="1" step="0.5" required value={novo.fioCm}
            onChange={(e) => setNovo({ ...novo, fioCm: e.target.value })} />
        </div>
        <div className="campo">
          <label htmlFor={`am-rend-${material.id}`}>Rendeu (cm)</label>
          <input id={`am-rend-${material.id}`} className="entrada" type="number" min="0.5" step="0.5" required value={novo.rendeuCm} placeholder="20"
            onChange={(e) => setNovo({ ...novo, rendeuCm: e.target.value })} />
        </div>
        <button type="submit" className="btn btn-oliva btn-p" disabled={enviando}>{enviando ? 'Salvando…' : 'Guardar amostra'}</button>
        {previa && <p className="mudo forte amostra-previa">Fator {fator(previa)}: cada 1 cm de trabalho gasta {qtd(previa)} cm de fio.</p>}
      </form>
      {erro && <p className="erro-texto" role="alert">{erro}</p>}
    </div>
  );
}

/** Calculadora de fio de uma linha da ficha técnica. */
export default function CalculadoraFio({ material, aoUsar, aoFechar }) {
  const [amostras, setAmostras] = useState([]);
  const [verAmostras, setVerAmostras] = useState(false);
  const [e, setE] = useState({
    modo: 'largura', larguraCm: '', fiosPorCm: material.fiosPorCm ?? '', qtdCordoes: '', dobrado: true,
    franjaCm: '', folgaCm: '20', perdaPct: '10',
  });
  const [trechos, setTrechos] = useState([{ cm: '', escolha: '' }]);

  useEffect(() => {
    api.get(`/api/materiais/${material.id}/amostras`).then(setAmostras).catch(() => {});
  }, [material.id]);

  // Primeiro trecho começa com a primeira amostra (ou a estimativa)
  const padrao = amostras[0] ? `a${amostras[0].id}` : 'padrao';
  const resolver = (t) => {
    const esc = t.escolha || padrao;
    if (esc === 'sem') return { fator: 1 };
    if (esc === 'padrao') return { fator: FATOR_PADRAO, estimativa: true };
    if (esc === 'outro') return { fator: Number(t.fatorOutro) || 1 };
    const a = amostras.find((x) => `a${x.id}` === esc);
    return a ? { fator: Number(a.fator) } : { fator: FATOR_PADRAO, estimativa: true };
  };
  const r = useMemo(() => calcularFio({ ...e, trechos: trechos.map((t) => ({ cm: t.cm, ...resolver(t) })) }, material),
    [e, trechos, amostras, material]); // eslint-disable-line react-hooks/exhaustive-deps

  const set = (k) => (ev) => setE((x) => ({ ...x, [k]: ev.target.type === 'checkbox' ? ev.target.checked : ev.target.value }));
  const mudarTrecho = (i, k, v) => setTrechos((l) => l.map((t, j) => (j === i ? { ...t, [k]: v } : t)));
  const metro = unidadeEhMetro(material.unidade);
  const id = `cf-${material.id}`;

  return (
    <div className="calc-fio" role="group" aria-label={`Calculadora de fio: ${material.nome}`}>
      <div className="calc-fio-topo">
        <p className="forte"><Icone nome="material" tamanho={18} /> Calcular fio · {material.nome}</p>
        <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label="Fechar calculadora" onClick={aoFechar}><Icone nome="fechar" tamanho={16} /></button>
      </div>

      <div className="calc-fio-grade">
        <div className="campo inteiro">
          <span className="rotulo-campo">Quantos cordões cortar</span>
          <div className="chips">
            <button type="button" className="chip" aria-pressed={e.modo === 'largura'} onClick={() => setE({ ...e, modo: 'largura' })}>Pela largura da peça</button>
            <button type="button" className="chip" aria-pressed={e.modo === 'quantidade'} onClick={() => setE({ ...e, modo: 'quantidade' })}>Eu sei quantos são</button>
          </div>
        </div>
        {e.modo === 'largura' ? <>
          <div className="campo">
            <label htmlFor={`${id}-larg`}>Largura da peça (cm)</label>
            <input id={`${id}-larg`} className="entrada" type="number" min="0" step="0.5" value={e.larguraCm} onChange={set('larguraCm')} placeholder="40" />
          </div>
          <div className="campo">
            <label htmlFor={`${id}-fpc`}>Fios lado a lado em 1 cm</label>
            <input id={`${id}-fpc`} className="entrada" type="number" min="0" step="0.1" value={e.fiosPorCm} onChange={set('fiosPorCm')} placeholder="2" />
            <span className="dica">Encoste os fios e conte quantos cabem em 1 cm.</span>
          </div>
        </> : (
          <div className="campo">
            <label htmlFor={`${id}-qtd`}>Cordões cortados</label>
            <input id={`${id}-qtd`} className="entrada" type="number" min="0" step="1" value={e.qtdCordoes} onChange={set('qtdCordoes')} placeholder="12" />
          </div>
        )}
        <label className="checkbox inteiro"><input type="checkbox" checked={e.dobrado} onChange={set('dobrado')} /> Cordão preso dobrado ao meio no suporte (2 pontas por cordão)</label>
      </div>

      <fieldset className="calc-fio-trechos">
        <legend>Trechos da peça, de cima para baixo</legend>
        {trechos.map((t, i) => (
          <div key={i} className="trecho-linha">
            <label className="sr" htmlFor={`${id}-t${i}`}>Trecho {i + 1}: nó</label>
            <select id={`${id}-t${i}`} className="entrada" value={t.escolha || padrao} onChange={(ev) => mudarTrecho(i, 'escolha', ev.target.value)}>
              {amostras.map((a) => <option key={a.id} value={`a${a.id}`}>{a.nomeNo} · {fator(a.fator)}</option>)}
              <option value="padrao">Com nós, sem amostra · {fator(FATOR_PADRAO)}</option>
              <option value="sem">Sem nó (1×)</option>
              <option value="outro">Outro fator…</option>
            </select>
            <span>{(t.escolha || padrao) === 'outro' && (
              <input className="entrada trecho-fator" type="number" min="1" step="0.1" aria-label={`Fator do trecho ${i + 1}`} placeholder="5"
                value={t.fatorOutro || ''} onChange={(ev) => mudarTrecho(i, 'fatorOutro', ev.target.value)} />
            )}</span>
            <div className="receita-qtd">
              <input className="entrada" type="number" min="0" step="0.5" aria-label={`Comprimento do trecho ${i + 1} em cm`} placeholder="30"
                value={t.cm} onChange={(ev) => mudarTrecho(i, 'cm', ev.target.value)} />
              <span className="mudo">cm</span>
            </div>
            <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Tirar trecho ${i + 1}`} disabled={trechos.length === 1}
              onClick={() => setTrechos((l) => l.filter((_, j) => j !== i))}><Icone nome="fechar" tamanho={16} /></button>
          </div>
        ))}
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <button type="button" className="btn btn-claro btn-p" onClick={() => setTrechos((l) => [...l, { cm: '', escolha: '' }])}>
            <Icone nome="mais" tamanho={16} largura={3} />Adicionar trecho
          </button>
          <button type="button" className="btn btn-fantasma btn-p" aria-expanded={verAmostras} onClick={() => setVerAmostras((v) => !v)}>
            {verAmostras ? 'Esconder amostras' : amostras.length ? `Minhas amostras (${amostras.length})` : 'Registrar uma amostra'}
          </button>
        </div>
        {verAmostras && <AmostrasFio material={material} compacto aoMudar={setAmostras} />}
      </fieldset>

      <div className="calc-fio-grade calc-fio-3">
        <div className="campo">
          <label htmlFor={`${id}-franja`}>Franja (cm)</label>
          <input id={`${id}-franja`} className="entrada" type="number" min="0" step="0.5" value={e.franjaCm} onChange={set('franjaCm')} placeholder="15" />
        </div>
        <div className="campo">
          <label htmlFor={`${id}-folga`}>Folga por cordão (cm)</label>
          <input id={`${id}-folga`} className="entrada" type="number" min="0" step="1" value={e.folgaCm} onChange={set('folgaCm')} />
        </div>
        <div className="campo">
          <label htmlFor={`${id}-perda`}>Perda (%)</label>
          <input id={`${id}-perda`} className="entrada" type="number" min="0" step="1" value={e.perdaPct} onChange={set('perdaPct')} />
        </div>
      </div>

      {r.valido ? (
        <div className="calc-fio-resultado" aria-live="polite">
          <p className="calc-fio-corte">Corte <strong>{r.cordoes} {r.cordoes === 1 ? 'cordão' : 'cordões'}</strong> de <strong>{fmtCm(r.corteCm)}</strong></p>
          <p className="mudo forte" style={{ fontSize: 13 }}>
            Cada ponta trabalha {fmtCm(r.pontaCm)}{e.dobrado ? ', dobrada ao meio' : ''}{Number(e.folgaCm) > 0 ? `, mais ${fmtCm(e.folgaCm)} de folga` : ''}. Peça com {fmtCm(r.alturaCm)} de altura.
          </p>
          <div className="calc-fio-numeros">
            <div><span>Total de fio</span><strong>{qtd(Math.round(r.totalM * 10) / 10)} m</strong>{Number(e.perdaPct) > 0 && <small>com {e.perdaPct}% de perda</small>}</div>
            {r.gramas != null && <div><span>Peso</span><strong>{r.gramas >= 1000 ? `${qtd(Math.round(r.gramas / 10) / 100)} kg` : `${qtd(Math.round(r.gramas))} g`}</strong></div>}
            {!metro && r.qtdFicha != null && <div><span>Em {material.unidade}</span><strong>{qtd(Math.round(r.qtdFicha * 100) / 100)}</strong></div>}
            {r.custo != null && <div><span>Custo do fio</span><strong>{moeda(r.custo)}</strong></div>}
          </div>
          {r.usaEstimativa && <p className="aviso-fio">Usando a estimativa de {FATOR_PADRAO}× em algum trecho. Registre uma amostra do seu nó para a conta ficar do seu jeito.</p>}
          {r.gramas == null && <p className="mudo" style={{ fontSize: 13 }}>Para ver o peso, informe em Materiais quanto pesa 1 metro deste fio.</p>}
          {r.qtdFicha == null
            ? <p className="aviso-fio">Para levar à ficha, informe em Materiais quantos metros tem 1 {material.unidade}.</p>
            : (
              <button type="button" className="btn btn-terra btn-p" style={{ alignSelf: 'flex-start' }}
                onClick={() => aoUsar(String(Math.round(r.qtdFicha * 1000) / 1000))}>
                <Icone nome="check" tamanho={16} />Usar na ficha: {qtd(Math.round(r.qtdFicha * 1000) / 1000)} {material.unidade}
              </button>
            )}
        </div>
      ) : (
        <p className="mudo forte calc-fio-resultado">Preencha {e.modo === 'largura' ? 'a largura, os fios por cm' : 'a quantidade de cordões'} e pelo menos um trecho para ver a conta.</p>
      )}
    </div>
  );
}

/** O material parece ser fio/cordão (merece a calculadora)? */
export const ehFio = (m) => unidadeEhMetro(m?.unidade) || m?.fiosPorCm != null || m?.metrosPorUnidade != null || (m?.amostras || 0) > 0
  || /(^|\s)(cord|fio|fios|barbante|linha|corda|novelo|lã)/i.test(m?.nome || '');
