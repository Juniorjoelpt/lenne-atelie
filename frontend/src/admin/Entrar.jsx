import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api, sessao } from '../api.js';

export default function Entrar() {
  const [erro, setErro] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const [params] = useSearchParams();
  const ir = useNavigate();

  async function entrar(e) {
    e.preventDefault();
    const d = Object.fromEntries(new FormData(e.currentTarget).entries());
    setErro(null);
    setEnviando(true);
    try {
      const r = await api.post('/api/auth/login', { email: d.email, senha: d.senha });
      sessao.entrar(r.token, r.nome, r.perfil);
      ir('/admin', { replace: true });
    } catch (err) {
      setErro(err.message);
      setEnviando(false);
    }
  }

  return (
    <div className="login-pagina">
      <form className="login-caixa" onSubmit={entrar}>
        <div style={{ textAlign: 'center' }}>
          <p className="marca" style={{ justifyContent: 'center' }}><strong>Lenne Carvalho</strong><span>ateliê</span></p>
          <p className="mudo forte" style={{ marginTop: 4 }}>Sistema de gestão</p>
        </div>
        {params.get('expirou') && !erro && <div className="aviso aviso-info">Sua sessão expirou. Entre novamente.</div>}
        {erro && <div className="aviso aviso-erro" role="alert">{erro}</div>}
        <div className="campo">
          <label htmlFor="email">E-mail</label>
          <input id="email" name="email" type="email" className="entrada" required autoComplete="username" />
        </div>
        <div className="campo">
          <label htmlFor="senha">Senha</label>
          <input id="senha" name="senha" type="password" className="entrada" required autoComplete="current-password" />
        </div>
        <button type="submit" className="btn btn-terra" disabled={enviando} style={{ minHeight: 52 }}>{enviando ? 'Entrando…' : 'Entrar'}</button>
      </form>
    </div>
  );
}
