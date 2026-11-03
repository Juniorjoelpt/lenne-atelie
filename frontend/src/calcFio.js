// Calculadora de fio do macramê.
// Base: a amostra da artesã (um fio de X cm rendeu Y cm de trabalho → fator X ÷ Y).
// Sem amostra, usa a estimativa de mercado de 4× para trechos com nós.

export const FATOR_PADRAO = 4;

export const NOS_COMUNS = [
  'Nó quadrado', 'Nó quadrado alternado', 'Espiral (meio nó)', 'Nó cabeça de cotovia', 'Nó festonê (diagonal)',
  'Nó festonê (horizontal)', 'Nó de botão', 'Trançado',
];

const num = (v) => {
  const n = Number(String(v ?? '').replace(',', '.'));
  return Number.isFinite(n) ? n : 0;
};

/** A unidade do material é metro? (metro, metros, m, mt) */
export function unidadeEhMetro(unidade) {
  return /^(m|mt|mts|metro|metros)\.?$/i.test(String(unidade || '').trim());
}

/** Quantos metros tem 1 unidade do material (1 para metro; para rolo/novelo, o cadastrado; senão null). */
export function metrosPorUnidade(material) {
  if (!material) return null;
  if (unidadeEhMetro(material.unidade)) return 1;
  const m = num(material.metrosPorUnidade);
  return m > 0 ? m : null;
}

/**
 * Calcula o fio de uma peça.
 * @param {object} e
 *  modo: 'largura' | 'quantidade'
 *  larguraCm, fiosPorCm   — pelo modo largura
 *  qtdCordoes             — pelo modo quantidade
 *  dobrado                — cordão preso dobrado ao meio no suporte (2 pontas por cordão)
 *  trechos: [{ cm, fator }] — de cima para baixo; sem nó = fator 1
 *  franjaCm, folgaCm (por cordão cortado), perdaPct
 * @param {object} material — { unidade, custoUnitario, gramasPorMetro, metrosPorUnidade }
 */
export function calcularFio(e, material) {
  const pontas = e.dobrado ? 2 : 1;
  const trechos = (e.trechos || []).filter((t) => num(t.cm) > 0);
  const pontaCm = trechos.reduce((s, t) => s + num(t.cm) * (num(t.fator) || 1), 0) + num(e.franjaCm);
  const corteCm = pontaCm * pontas + num(e.folgaCm);

  let cordoes = 0;
  if (e.modo === 'quantidade') cordoes = Math.max(0, Math.round(num(e.qtdCordoes)));
  else if (num(e.larguraCm) > 0 && num(e.fiosPorCm) > 0) cordoes = Math.ceil((num(e.larguraCm) * num(e.fiosPorCm)) / pontas - 1e-9);

  const semPerdaM = (cordoes * corteCm) / 100;
  const totalM = semPerdaM * (1 + num(e.perdaPct) / 100);
  const mpu = metrosPorUnidade(material);
  const qtdFicha = mpu ? totalM / mpu : null;
  const gpm = num(material?.gramasPorMetro);
  const gramas = gpm > 0 ? totalM * gpm : null;
  const custo = qtdFicha != null ? qtdFicha * num(material?.custoUnitario) : null;
  const usaEstimativa = trechos.some((t) => t.estimativa);

  return {
    pontaCm, corteCm, cordoes, semPerdaM, totalM, qtdFicha, gramas, custo, usaEstimativa,
    alturaCm: trechos.reduce((s, t) => s + num(t.cm), 0) + num(e.franjaCm),
    valido: cordoes > 0 && corteCm > 0,
  };
}

/** Novo tamanho de uma peça: regra de 3 pelo comprimento (cresce num sentido) ou pela área (cresce nos dois). */
export function escalarFio(gastoAtual, medidaAtual, medidaNova, cresceNosDois) {
  const a = num(medidaAtual); const n = num(medidaNova);
  if (a <= 0 || n <= 0) return null;
  const k = n / a;
  return num(gastoAtual) * (cresceNosDois ? k * k : k);
}

export const fmtCm = (cm) => {
  const v = Number(cm || 0);
  return v >= 100 ? `${(v / 100).toLocaleString('pt-BR', { maximumFractionDigits: 2 })} m` : `${v.toLocaleString('pt-BR', { maximumFractionDigits: 1 })} cm`;
};
