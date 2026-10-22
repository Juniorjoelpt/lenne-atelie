import { useState } from 'react';
import { api } from '../api.js';
import { Campo, Estado, lerForm, useCarregar, useToast } from '../componentes/ui.jsx';
import { telefone } from '../util.js';
import { Topo } from './Layout.jsx';

export default function Configuracoes() {
  const { dados: c, erro, carregando } = useCarregar(() => api.get('/api/configuracoes'));
  return (
    <>
      <Topo titulo="Configurações" />
      <Estado carregando={carregando} erro={erro}>
        {c && <DadosAtelie c={c} />}
      </Estado>
      <TrocarSenha />
    </>
  );
}

function DadosAtelie({ c }) {
  const [erro, setErro] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const toast = useToast();

  async function salvar(e) {
    e.preventDefault();
    setErro(null);
    setSalvando(true);
    try {
      await api.put('/api/configuracoes', lerForm(new FormData(e.currentTarget)));
      toast('Dados do ateliê salvos. O site já foi atualizado.');
    } catch (err) {
      setErro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <form className="cartao" onSubmit={salvar} noValidate>
      <div className="cartao-titulo"><h2>Dados do ateliê no site</h2></div>
      {erro && <div className="aviso aviso-erro" role="alert" style={{ marginBottom: 16 }}>{erro}</div>}
      <div className="form-grade">
        <Campo rotulo="Nome do ateliê" nome="nomeAtelie" defaultValue={c.nomeAtelie} required />
        <Campo rotulo="WhatsApp do ateliê" nome="whatsapp" defaultValue={telefone(c.whatsapp)} required inputMode="tel" dica="Recebe os orçamentos e os comprovantes de Pix." />
        <Campo rotulo="Chave Pix" nome="chavePix" defaultValue={c.chavePix} dica="Aparece para a aluna depois da inscrição na oficina." />
        <Campo rotulo="Nome do titular do Pix" nome="titularPix" defaultValue={c.titularPix} />
        <Campo rotulo="Instagram" nome="instagram" defaultValue={c.instagram} placeholder="lennecarvalhoatelie" />
        <Campo rotulo="Cidade" nome="cidade" defaultValue={c.cidade} placeholder="São Luís — MA" />
        <Campo rotulo="Endereço do ateliê" nome="endereco" defaultValue={c.endereco} inteiro />
        <Campo rotulo="Horário de funcionamento" nome="horario" defaultValue={c.horario} inteiro placeholder="Seg. a sex., 9h às 18h" />
        <Campo rotulo='Texto do "Quem sou"' nome="sobreTexto" linhas={5} defaultValue={c.sobreTexto} inteiro />
      </div>
      <div className="modal-rodape"><button type="submit" className="btn btn-terra" disabled={salvando}>{salvando ? 'Salvando…' : 'Salvar'}</button></div>
    </form>
  );
}

function TrocarSenha() {
  const [erro, setErro] = useState(null);
  const toast = useToast();

  async function salvar(e) {
    e.preventDefault();
    const form = e.currentTarget;
    const d = lerForm(new FormData(form));
    setErro(null);
    if (d.novaSenha !== d.confirmar) { setErro('A confirmação não confere com a nova senha.'); return; }
    try {
      await api.post('/api/auth/trocar-senha', { senhaAtual: d.senhaAtual, novaSenha: d.novaSenha });
      toast('Senha alterada.');
      form.reset();
    } catch (err) {
      setErro(err.message);
    }
  }

  return (
    <form className="cartao" onSubmit={salvar} noValidate>
      <div className="cartao-titulo"><h2>Trocar senha</h2></div>
      {erro && <div className="aviso aviso-erro" role="alert" style={{ marginBottom: 16 }}>{erro}</div>}
      <div className="form-grade">
        <Campo rotulo="Senha atual" nome="senhaAtual" tipo="password" autoComplete="current-password" required inteiro />
        <Campo rotulo="Nova senha" nome="novaSenha" tipo="password" autoComplete="new-password" minLength={8} required dica="Mínimo de 8 caracteres." />
        <Campo rotulo="Confirmar nova senha" nome="confirmar" tipo="password" autoComplete="new-password" required />
      </div>
      <div className="modal-rodape"><button type="submit" className="btn btn-terra">Trocar senha</button></div>
    </form>
  );
}
