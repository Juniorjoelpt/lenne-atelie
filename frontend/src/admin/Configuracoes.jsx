import { useState } from 'react';
import { api } from '../api.js';
import { Campo, Estado, lerForm, useCarregar, useToast } from '../componentes/ui.jsx';
import { moeda, telefone } from '../util.js';
import { Topo } from './Layout.jsx';

export default function Configuracoes() {
  const { dados: c, erro, carregando } = useCarregar(() => api.get('/api/configuracoes'));
  return (
    <>
      <Topo titulo="Configurações" />
      <Precificacao />
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

function Precificacao() {
  const { dados: p, erro: erroCarga, carregando } = useCarregar(() => api.get('/api/configuracoes/precificacao'));
  return (
    <Estado carregando={carregando} erro={erroCarga}>
      {p && <FormPrecificacao p={p} />}
    </Estado>
  );
}

function FormPrecificacao({ p }) {
  const [f, setF] = useState({
    salarioDesejado: String(p.salarioDesejado), horasMes: String(p.horasMes),
    custosFixosMensais: String(p.custosFixosMensais), margemPadrao: String(Number(p.margemPadrao)),
  });
  const [erro, setErro] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const toast = useToast();
  const n = (v) => Number(String(v).replace(',', '.')) || 0;
  const horas = Math.max(1, n(f.horasMes));
  const valorHora = n(f.salarioDesejado) / horas;
  const fixoHora = n(f.custosFixosMensais) / horas;
  const set = (k) => (e) => setF((x) => ({ ...x, [k]: e.target.value }));

  async function salvar(e) {
    e.preventDefault();
    setErro(null);
    setSalvando(true);
    try {
      await api.put('/api/configuracoes/precificacao', {
        salarioDesejado: n(f.salarioDesejado), horasMes: Math.round(n(f.horasMes)),
        custosFixosMensais: n(f.custosFixosMensais), margemPadrao: n(f.margemPadrao),
      });
      toast('Precificação salva. As fichas já usam os novos valores.');
    } catch (err) {
      setErro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <form className="cartao" onSubmit={salvar} noValidate>
      <div className="cartao-titulo"><h2>Precificação</h2><span className="selo selo-areia">só aparece aqui no sistema</span></div>
      <p className="mudo forte" style={{ marginBottom: 16, fontSize: 15 }}>
        Com esses valores o sistema calcula quanto vale sua hora e reparte os custos fixos em cada peça.
      </p>
      {erro && <div className="aviso aviso-erro" role="alert" style={{ marginBottom: 16 }}>{erro}</div>}
      <div className="form-grade">
        <div className="campo">
          <label htmlFor="pr-sal">Quanto quer ganhar por mês (R$)</label>
          <input id="pr-sal" className="entrada" type="number" min="0" step="50" value={f.salarioDesejado} onChange={set('salarioDesejado')} />
          <span className="dica">O seu "salário" com o ateliê.</span>
        </div>
        <div className="campo">
          <label htmlFor="pr-horas">Horas de produção por mês</label>
          <input id="pr-horas" className="entrada" type="number" min="1" max="400" step="1" value={f.horasMes} onChange={set('horasMes')} />
          <span className="dica">Ex.: 5 dias × 6 h × 4 semanas = 120 h.</span>
        </div>
        <div className="campo">
          <label htmlFor="pr-fixos">Custos fixos do mês (R$)</label>
          <input id="pr-fixos" className="entrada" type="number" min="0" step="10" value={f.custosFixosMensais} onChange={set('custosFixosMensais')} />
          <span className="dica">Aluguel, energia, internet, MEI, aplicativos…</span>
        </div>
        <div className="campo">
          <label htmlFor="pr-margem">Margem de lucro padrão (%)</label>
          <input id="pr-margem" className="entrada" type="number" min="0" max="500" step="1" value={f.margemPadrao} onChange={set('margemPadrao')} />
          <span className="dica">Aplicada sobre o custo total. Cada ficha pode ter a sua.</span>
        </div>
      </div>
      <div className="custos-grade" style={{ marginTop: 18 }}>
        <div style={{ background: 'var(--oliva-claro)' }}><p>Sua hora vale</p><p>{moeda(valorHora)}</p></div>
        <div style={{ background: 'var(--ocre-claro)' }}><p>Custo fixo por hora</p><p>{moeda(fixoHora)}</p></div>
        <div style={{ background: 'var(--terra-claro)' }}><p>Uma peça de 3 h custa de trabalho</p><p>{moeda((valorHora + fixoHora) * 3)}</p></div>
      </div>
      <div className="modal-rodape"><button type="submit" className="btn btn-terra" disabled={salvando}>{salvando ? 'Salvando…' : 'Salvar precificação'}</button></div>
    </form>
  );
}
