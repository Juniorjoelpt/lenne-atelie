import { useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Modal } from '../componentes/ui.jsx';
import { dataHora, linkWhats, moeda } from '../util.js';

/** Inscrição na oficina pelo site, com as instruções do Pix manual. */
export default function Inscricao({ oficina, aoFechar }) {
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [copiado, setCopiado] = useState(false);

  async function enviar(e) {
    e.preventDefault();
    const d = Object.fromEntries(new FormData(e.currentTarget).entries());
    setErro(null);
    setEnviando(true);
    try {
      setResultado(await api.post(`/api/publico/oficinas/${oficina.id}/inscricoes`, {
        nome: d.nome, whatsapp: d.whatsapp, site: d.site || null,
      }));
    } catch (err) {
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  async function copiar() {
    try {
      await navigator.clipboard.writeText(resultado.chavePix);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2500);
    } catch {
      setCopiado(false);
    }
  }

  function avisarComprovante() {
    // Marca como "Pix a conferir" no sistema; o WhatsApp abre pelo próprio link.
    api.post(`/api/publico/inscricoes/${resultado.codigo}/comprovante-enviado`).catch(() => {});
  }

  const espera = resultado?.status === 'LISTA_ESPERA';
  const textoComprovante = resultado && `Olá, Lenne! Fiz a inscrição na oficina "${resultado.oficina}" (${dataHora(resultado.inicio)}) e segue o comprovante do Pix de ${moeda(resultado.valor)}.`;
  const textoEspera = resultado && `Olá, Lenne! Entrei na lista de espera da oficina "${resultado.oficina}" (${dataHora(resultado.inicio)}).`;

  return (
    <Modal titulo={resultado ? (espera ? 'Você está na lista de espera' : 'Falta só o Pix!') : 'Inscrição na oficina'} aoFechar={aoFechar}>
      {!resultado ? (
        <form onSubmit={enviar} className="form-grade">
          <div className="inteiro cartao" style={{ background: 'var(--oliva-claro)', boxShadow: 'none', padding: 18 }}>
            <p className="forte" style={{ fontSize: 19 }}>{oficina.titulo}</p>
            <p style={{ fontWeight: 700, color: 'var(--oliva-texto)' }}>{dataHora(oficina.inicio)} · {moeda(oficina.valor)}</p>
            {oficina.esgotada && <p style={{ marginTop: 6, fontWeight: 700 }}>A turma está lotada: sua inscrição entra na lista de espera e avisamos se abrir vaga.</p>}
          </div>
          {erro && <div className="aviso aviso-erro inteiro" role="alert">{erro}</div>}
          <div className="campo inteiro">
            <label htmlFor="i-nome">Seu nome</label>
            <input id="i-nome" name="nome" className="entrada" required maxLength={140} autoComplete="name" />
          </div>
          <div className="campo inteiro">
            <label htmlFor="i-whats">Seu WhatsApp</label>
            <input id="i-whats" name="whatsapp" className="entrada" required maxLength={20} inputMode="tel" autoComplete="tel" placeholder="(98) 90000-0000" />
          </div>
          <div aria-hidden="true" style={{ position: 'absolute', left: -9999, width: 1, height: 1, overflow: 'hidden' }}>
            <label>Site <input name="site" tabIndex={-1} autoComplete="off" /></label>
          </div>
          <div className="inteiro modal-rodape" style={{ marginTop: 4 }}>
            <button type="button" className="btn btn-fantasma" onClick={aoFechar}>Voltar</button>
            <button type="submit" className="btn btn-oliva" disabled={enviando}>{enviando ? 'Enviando…' : oficina.esgotada ? 'Entrar na lista de espera' : 'Fazer inscrição'}</button>
          </div>
        </form>
      ) : espera ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <p style={{ fontWeight: 600 }}>Se abrir uma vaga em <strong>{resultado.oficina}</strong>, a Lenne te chama no WhatsApp. Você ainda não precisa pagar nada.</p>
          <div className="modal-rodape">
            <a className="btn btn-claro" href={linkWhats(resultado.whatsappAtelie, textoEspera)} target="_blank" rel="noopener noreferrer"><Icone nome="whats" tamanho={18} />Falar com a Lenne</a>
            <button type="button" className="btn btn-oliva" onClick={aoFechar}>Entendi</button>
          </div>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <p style={{ fontWeight: 600 }}>Sua vaga em <strong>{resultado.oficina}</strong> fica reservada. Para confirmar:</p>
          <ol className="passos" style={{ color: 'var(--marrom)' }}>
            <li><span>1</span><span>Faça um Pix de <strong>{moeda(resultado.valor)}</strong> para a chave abaixo.</span></li>
            <li><span>2</span><span>Envie o comprovante pelo WhatsApp no botão verde.</span></li>
          </ol>
          {resultado.chavePix ? (
            <div className="press" style={{ background: 'var(--afundado)', borderRadius: 20, padding: 16, display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 12 }}>
              <Icone nome="pix" cor="var(--oliva)" />
              <div style={{ flex: '1 1 180px', minWidth: 0 }}>
                <p className="forte" style={{ wordBreak: 'break-all' }}>{resultado.chavePix}</p>
                {resultado.titularPix && <p className="mudo" style={{ fontSize: 14 }}>{resultado.titularPix}</p>}
              </div>
              <button type="button" className="btn btn-claro btn-p" onClick={copiar}>{copiado ? 'Copiada!' : 'Copiar chave'}</button>
            </div>
          ) : (
            <div className="aviso aviso-info">A Lenne vai te mandar a chave Pix pelo WhatsApp.</div>
          )}
          <div className="modal-rodape">
            <button type="button" className="btn btn-fantasma" onClick={aoFechar}>Fechar</button>
            <a className="btn btn-oliva" href={linkWhats(resultado.whatsappAtelie, textoComprovante)} target="_blank" rel="noopener noreferrer"
              onClick={avisarComprovante}><Icone nome="whats" tamanho={18} />Enviar comprovante</a>
          </div>
        </div>
      )}
    </Modal>
  );
}
