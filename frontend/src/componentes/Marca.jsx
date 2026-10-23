// Logos do ateliê (vetor extraído do manual de identidade visual).
const ARQUIVOS = {
  horizontal: [600, 122],
  empilhada: [404, 150],
  circular: [159, 135],
  simbolo: [100, 100],
};

/**
 * variante: horizontal | empilhada | circular | simbolo
 * clara: versão para fundo escuro (marrom ou verde)
 */
export default function Marca({ variante = 'horizontal', clara = false, decorativa = false, className = '', ...resto }) {
  const [w, h] = ARQUIVOS[variante] || ARQUIVOS.horizontal;
  return (
    <img src={`/marca/${variante}${clara ? '-clara' : ''}.svg`} width={w} height={h}
      alt={decorativa ? '' : 'Lenne Carvalho Ateliê'} aria-hidden={decorativa || undefined}
      className={`logo ${className}`} {...resto} />
  );
}

/** Lugar de foto que ainda não existe: o símbolo da marca, suave. */
export function SemImagem() {
  return <img src="/marca/simbolo.svg" alt="" aria-hidden="true" className="sem-imagem" />;
}
