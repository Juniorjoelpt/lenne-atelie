import { useState } from 'react';
import Icone from '../componentes/Icone.jsx';

/** Chave Pix com botão de copiar (oficinas e cursos). */
export default function BlocoPix({ chave, titular }) {
  const [copiado, setCopiado] = useState(false);
  if (!chave) return <div className="aviso aviso-info">A Lenne vai te mandar a chave Pix pelo WhatsApp.</div>;
  async function copiar() {
    try {
      await navigator.clipboard.writeText(chave);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2500);
    } catch { setCopiado(false); }
  }
  return (
    <div className="press" style={{ background: 'var(--afundado)', borderRadius: 20, padding: 16, display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 12 }}>
      <Icone nome="pix" cor="var(--oliva)" />
      <div style={{ flex: '1 1 180px', minWidth: 0 }}>
        <p className="forte" style={{ wordBreak: 'break-all' }}>{chave}</p>
        {titular && <p className="mudo" style={{ fontSize: 14 }}>{titular}</p>}
      </div>
      <button type="button" className="btn btn-claro btn-p" onClick={copiar}>{copiado ? 'Copiada!' : 'Copiar chave'}</button>
    </div>
  );
}

export const duracaoTexto = (min) => {
  const m = Number(min) || 0;
  if (m <= 0) return null;
  if (m < 60) return `${m} min`;
  const h = Math.floor(m / 60);
  const r = m % 60;
  return r ? `${h} h ${r} min` : `${h} h`;
};
