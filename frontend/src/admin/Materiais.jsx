import { useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, ModalForm, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { moeda } from '../util.js';
import { Topo } from './Layout.jsx';

const qtd = (n) => Number(n).toLocaleString('pt-BR', { maximumFractionDigits: 2 });

export default function Materiais() {
  const [form, setForm] = useState(null);
  const [ajuste, setAjuste] = useState(null);
  const [excluir, setExcluir] = useState(null);
  const toast = useToast();
  const { dados, erro, carregando, recarregar } = useCarregar(() => api.get('/api/materiais'));
  const valorEstoque = (dados || []).reduce((s, m) => s + Number(m.quantidade) * Number(m.custoUnitario), 0);

  return (
    <>
      <Topo titulo="Materiais">
        <button type="button" className="btn btn-terra" onClick={() => setForm({})}><Icone nome="mais" tamanho={18} largura={3} />Novo material</button>
      </Topo>
      <section className="cartao">
        <Estado carregando={carregando} erro={erro}>
          {dados?.length === 0 ? <Vazio>Cadastre os fios e cordões que você usa para receber aviso quando estiverem acabando.</Vazio> : (
            <>
              <p className="mudo forte" style={{ marginBottom: 12 }}>Valor em estoque: {moeda(valorEstoque)}</p>
              <div className="tabela-caixa">
                <table className="tabela">
                  <thead><tr><th>Material</th><th className="num">Em estoque</th><th className="num">Mínimo</th><th className="num">Custo</th><th><span className="sr">Ações</span></th></tr></thead>
                  <tbody>
                    {dados?.map((m) => (
                      <tr key={m.id}>
                        <td><strong>{m.nome}</strong>{m.abaixoDoMinimo && <span className="selo selo-perigo" style={{ marginLeft: 8 }}>acabando</span>}</td>
                        <td className="num" style={{ fontWeight: 800 }}>{qtd(m.quantidade)} {m.unidade}</td>
                        <td className="num">{qtd(m.estoqueMinimo)}</td>
                        <td className="num">{moeda(m.custoUnitario)}/{m.unidade}</td>
                        <td style={{ whiteSpace: 'nowrap', textAlign: 'right' }}>
                          <button type="button" className="btn btn-claro btn-p" onClick={() => setAjuste({ m, tipo: 'SAIDA' })}>Usei</button>{' '}
                          <button type="button" className="btn btn-oliva btn-p" onClick={() => setAjuste({ m, tipo: 'ENTRADA' })}>Comprei</button>{' '}
                          <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Editar ${m.nome}`} onClick={() => setForm(m)}><Icone nome="editar" tamanho={18} /></button>
                          <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Excluir ${m.nome}`} onClick={() => setExcluir(m)}><Icone nome="lixo" tamanho={18} /></button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </Estado>
      </section>

      {form && (
        <ModalForm titulo={form.id ? 'Editar material' : 'Novo material'} aoFechar={() => setForm(null)} aoSalvar={async (fd) => {
          const d = lerForm(fd, ['quantidade', 'estoqueMinimo', 'custoUnitario']);
          if (form.id) await api.put(`/api/materiais/${form.id}`, d);
          else await api.post('/api/materiais', d);
          toast('Material salvo.');
          setForm(null);
          recarregar();
        }}>
          <Campo rotulo="Nome" nome="nome" defaultValue={form.nome} required maxLength={140} inteiro placeholder="Cordão cru 5 mm" />
          <Campo rotulo="Unidade" nome="unidade" defaultValue={form.unidade || 'rolo'} required maxLength={20} dica="rolo, metro, kg, unidade…" />
          <Campo rotulo="Quantidade em estoque" nome="quantidade" tipo="number" min="0" step="0.01" defaultValue={form.quantidade ?? 0} required />
          <Campo rotulo="Avisar quando chegar a" nome="estoqueMinimo" tipo="number" min="0" step="0.01" defaultValue={form.estoqueMinimo ?? 0} required />
          <Campo rotulo="Custo por unidade (R$)" nome="custoUnitario" tipo="number" min="0" step="0.01" defaultValue={form.custoUnitario ?? 0} required />
        </ModalForm>
      )}

      {ajuste && (
        <ModalForm titulo={ajuste.tipo === 'ENTRADA' ? `Comprei ${ajuste.m.nome}` : `Usei ${ajuste.m.nome}`} textoSalvar="Confirmar"
          aoFechar={() => setAjuste(null)} aoSalvar={async (fd) => {
            const d = lerForm(fd, ['quantidade', 'valorCompra']);
            d.tipo = ajuste.tipo;
            d.registrarDespesa = fd.get('registrarDespesa') === 'on';
            await api.post(`/api/materiais/${ajuste.m.id}/ajuste`, d);
            toast(ajuste.tipo === 'ENTRADA' ? 'Estoque atualizado.' : 'Uso registrado.');
            setAjuste(null);
            recarregar();
          }}>
          <Campo rotulo={`Quantidade (${ajuste.m.unidade})`} nome="quantidade" tipo="number" min="0.01" step="0.01" required inteiro={ajuste.tipo === 'SAIDA'} />
          {ajuste.tipo === 'ENTRADA' && <>
            <Campo rotulo="Valor pago (R$)" nome="valorCompra" tipo="number" min="0" step="0.01" dica="Se ficar em branco, usa o custo cadastrado." />
            <label className="checkbox inteiro"><input type="checkbox" name="registrarDespesa" defaultChecked /> Lançar a compra como despesa no financeiro</label>
          </>}
        </ModalForm>
      )}

      {excluir && <Confirmar titulo="Excluir material" perigo textoBotao="Excluir" texto={`Excluir ${excluir.nome}?`}
        aoFechar={() => setExcluir(null)} aoConfirmar={async () => { await api.del(`/api/materiais/${excluir.id}`); toast('Material excluído.'); setExcluir(null); recarregar(); }} />}
    </>
  );
}
