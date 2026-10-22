// Formatação e rótulos em português

const moedaFmt = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
export const moeda = (v) => moedaFmt.format(Number(v || 0));
export const moedaCurta = (v) => {
  const n = Number(v || 0);
  if (n >= 1000) return `R$ ${(n / 1000).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} mil`;
  return moedaFmt.format(n).replace(',00', '');
};

/** "2026-10-24" → "24/10/2026" sem problemas de fuso. */
export function data(iso) {
  if (!iso) return '—';
  const [a, m, d] = iso.slice(0, 10).split('-');
  return `${d}/${m}/${a}`;
}

export function dataCurta(iso) {
  if (!iso) return '—';
  const d = new Date(iso.length === 10 ? `${iso}T12:00:00` : iso);
  return d.toLocaleDateString('pt-BR', { weekday: 'short', day: '2-digit', month: '2-digit' }).replace('.', '');
}

export function dataHora(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString('pt-BR', { weekday: 'short', day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }).replace('.', '');
}

export function hora(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

export const hojeIso = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

/** "5598988793891" → "(98) 98879-3891" */
export function telefone(w) {
  if (!w) return '—';
  const d = w.replace(/\D/g, '').replace(/^55/, '');
  if (d.length === 11) return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
  if (d.length === 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return w;
}

export function linkWhats(numero, texto) {
  if (!numero) return null;
  const n = numero.replace(/\D/g, '');
  return `https://wa.me/${n}${texto ? `?text=${encodeURIComponent(texto)}` : ''}`;
}

export const primeiroNome = (nome) => (nome || '').trim().split(/\s+/)[0] || '';

// ---------- Rótulos e cores dos status ----------

export const STATUS_ORCAMENTO = {
  NOVO: ['Novo', 'terra'],
  RESPONDIDO: ['Respondido', 'ocre'],
  APROVADO: ['Aprovado', 'oliva'],
  RECUSADO: ['Recusado', 'areia'],
};

export const STATUS_ENCOMENDA = {
  AGUARDANDO_SINAL: ['Aguardando sinal', 'ocre'],
  EM_PRODUCAO: ['Em produção', 'terra'],
  PRONTA: ['Pronta', 'oliva'],
  ENTREGUE: ['Entregue', 'areia'],
  CANCELADA: ['Cancelada', 'perigo'],
};

export const STATUS_INSCRICAO = {
  AGUARDANDO_PIX: ['Aguardando Pix', 'ocre'],
  A_CONFERIR: ['Pix a conferir', 'terra'],
  CONFIRMADA: ['Confirmada', 'oliva'],
  LISTA_ESPERA: ['Lista de espera', 'areia'],
  CANCELADA: ['Cancelada', 'perigo'],
};

export const STATUS_OFICINA = {
  ABERTA: ['Inscrições abertas', 'oliva'],
  ENCERRADA: ['Encerrada', 'areia'],
  CANCELADA: ['Cancelada', 'perigo'],
};

export const FRENTES = { ENCOMENDA: 'Encomenda', CASAMENTO: 'Casamento', PRESENTE: 'Presente' };

export const TIPOS_CLIENTE = { CLIENTE: 'Cliente', NOIVA: 'Noiva', PARCEIRO: 'Parceiro(a)', ALUNA: 'Aluna' };

export const CATEGORIAS_LANC = {
  ENCOMENDA: 'Encomendas', CASAMENTO: 'Casamentos', PRESENTE: 'Presentes',
  OFICINA: 'Oficinas', MATERIAL: 'Materiais', OUTROS: 'Outros',
};

export const FORMAS = { PIX: 'Pix', DINHEIRO: 'Dinheiro', CARTAO: 'Cartão', OUTRO: 'Outro' };

export const CATEGORIAS_PORTFOLIO = {
  DECORACAO: 'Decoração', CASAMENTOS: 'Casamentos', PRESENTES: 'Presentes', OFICINAS: 'Oficinas',
};

export const TIPOS_PECA = [
  'Painel ou peça decorativa',
  'Cortina ou cabeceira',
  'Decoração de casamento ou evento',
  'Presentes ou lembrancinhas',
  'Outro',
];

/** Cores dos gráficos, na ordem da marca. */
export const CORES_GRAFICO = ['#C9693F', '#A9B48A', '#E2BE7A', '#8C6A4F', '#D99A7A', '#5E6B3F'];
