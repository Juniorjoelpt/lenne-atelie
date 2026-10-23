import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import Icone from '../componentes/Icone.jsx';
import Marca from '../componentes/Marca.jsx';

const EXTENSO = (iso) => new Date(`${iso}T12:00`).toLocaleDateString('pt-BR', { day: 'numeric', month: 'long', year: 'numeric' });

/** Página que a aluna abre pelo link do WhatsApp: confirma o certificado e baixa o PDF. */
export default function Certificado() {
  const { codigo } = useParams();
  const [c, setC] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    document.title = 'Certificado · Lenne Carvalho Ateliê';
    fetch(`/api/publico/certificados/${encodeURIComponent(codigo)}`)
      .then(async (r) => {
        if (r.status === 404) throw new Error('Não encontramos este certificado. Confira se o link está completo ou fale com o ateliê.');
        if (!r.ok) throw new Error('Não foi possível abrir agora. Tente de novo em alguns minutos.');
        setC(await r.json());
      })
      .catch((e) => setErro(e.message));
  }, [codigo]);

  const horas = c?.cargaHoraria != null ? Number(c.cargaHoraria).toLocaleString('pt-BR', { maximumFractionDigits: 1 }) : null;

  return (
    <main className="cert-pagina">
      <div className="cert-caixa">
        <a href="/" className="marca" style={{ justifyContent: 'center' }}><Marca variante="empilhada" style={{ width: 200, height: 'auto' }} /></a>
        {erro && <div className="aviso aviso-erro" role="alert">{erro}</div>}
        {!erro && !c && <p className="carregando">Carregando…</p>}
        {c && (
          <>
            <span className="selo selo-oliva"><Icone nome="check" tamanho={14} largura={3} />Certificado válido</span>
            <p className="mudo forte">Certificamos que</p>
            <p className="nome">{c.nome}</p>
            <p style={{ fontSize: 17, fontWeight: 600 }}>
              participou da oficina <strong>{c.oficina}</strong>, realizada em {EXTENSO(c.data)}
              {c.local ? `, em ${c.local}` : ''}{horas ? `, com carga horária de ${horas} ${horas === '1' ? 'hora' : 'horas'}` : ''}.
            </p>
            {c.disponivel ? (
              <a className="btn btn-terra" href={`/api/publico/certificados/${encodeURIComponent(codigo)}/pdf`} style={{ minHeight: 52, marginTop: 6 }}>
                <Icone nome="baixar" tamanho={18} />Baixar certificado (PDF)
              </a>
            ) : (
              <div className="aviso aviso-info">O certificado fica disponível para baixar depois que a oficina começar.</div>
            )}
            <p className="mudo" style={{ fontSize: 13 }}>Emitido por {c.atelie}. Este endereço serve como verificação do certificado.</p>
          </>
        )}
      </div>
    </main>
  );
}
