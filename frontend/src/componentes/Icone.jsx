// Ícones em traço, desenhados para o ateliê (sem dependências externas).
const CAMINHOS = {
  painel: <path d="M3 11 12 3l9 8M5 9.5V20h5v-6h4v6h5V9.5" />,
  orcamento: <><path d="M6 3h9l4 4v14H6z" /><path d="M14 3v5h5M9 13h7M9 17h5" /></>,
  encomenda: <path d="M5 4h14M8 4v9M12 4v13M16 4v9M8 13l4 6 4-6" />,
  oficina: <><circle cx="8" cy="8" r="3" /><circle cx="17" cy="9" r="2.5" /><path d="M2 20c0-3.5 2.7-6 6-6s6 2.5 6 6M15 20c0-2.6 1.3-5 2.5-5S21 17 21 20" /></>,
  cliente: <path d="M12 20.5s-8-5.2-8-11a4.5 4.5 0 0 1 8-2.8 4.5 4.5 0 0 1 8 2.8c0 5.8-8 11-8 11z" />,
  financeiro: <path d="M3 12h4l3-7 4 14 3-7h4" />,
  material: <><circle cx="12" cy="12" r="8" /><path d="M5 9c4 1 10 1 14 0M5 15c4-1 10-1 14 0" /></>,
  portfolio: <><rect x="3" y="5" width="18" height="14" rx="3" /><circle cx="9" cy="10" r="1.8" /><path d="m21 16-5-5-8 8" /></>,
  config: <><circle cx="12" cy="12" r="3" /><path d="M12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1" /></>,
  sair: <path d="M15 4h4v16h-4M10 8l-4 4 4 4M6 12h10" />,
  site: <><circle cx="12" cy="12" r="9" /><path d="M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18" /></>,
  mais: <path d="M12 5v14M5 12h14" />,
  busca: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
  fechar: <path d="M6 6l12 12M18 6 6 18" />,
  menu: <path d="M4 7h16M4 12h16M4 17h16" />,
  whats: <path d="M3.5 20.5 5 16a8.5 8.5 0 1 1 3.5 3.5zM9 8.5c0 3.6 2.9 6.5 6.5 6.5l1-1.5-2-1-1 1c-1.4-.5-2.5-1.6-3-3l1-1-1-2z" />,
  editar: <path d="M4 20h4L19 9l-4-4L4 16zM13.5 6.5l4 4" />,
  lixo: <path d="M4 7h16M10 7V4h4v3M6 7l1 13h10l1-13M10 11v6M14 11v6" />,
  check: <path d="m5 12 5 5 9-10" />,
  dinheiro: <><rect x="3" y="6" width="18" height="12" rx="3" /><circle cx="12" cy="12" r="2.5" /></>,
  calendario: <><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M3 10h18M8 3v4M16 3v4" /></>,
  pix: <path d="m12 3 4 4-4 4-4-4zM12 13l4 4-4 4-4-4zM3 12l4-4 4 4-4 4zM13 12l4-4 4 4-4 4z" />,
  alerta: <><path d="M12 3 2 20h20z" /><path d="M12 10v4M12 17v.5" /></>,
  seta: <path d="m9 5 7 7-7 7" />,
  play: <path d="M7 5v14l12-7z" />,
  etiqueta: <><path d="M3 12V4h8l10 10-8 8z" /><circle cx="7.5" cy="8.5" r="1.5" /></>,
  sacola: <><path d="M5 8h14l-1 13H6z" /><path d="M9 8V6a3 3 0 0 1 6 0v2" /></>,
  menos: <path d="M5 12h14" />,
  foto: <><rect x="3" y="6" width="18" height="14" rx="3" /><circle cx="12" cy="13" r="3.5" /><path d="M8 6l1.5-2h5L16 6" /></>,
};

export default function Icone({ nome, tamanho = 22, cor = 'currentColor', largura = 2, ...resto }) {
  return (
    <svg aria-hidden="true" width={tamanho} height={tamanho} viewBox="0 0 24 24" fill="none" stroke={cor}
      strokeWidth={largura} strokeLinecap="round" strokeLinejoin="round" {...resto}>
      {CAMINHOS[nome]}
    </svg>
  );
}
