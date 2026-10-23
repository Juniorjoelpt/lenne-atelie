import { Navigate, Route, Routes } from 'react-router-dom';
import { sessao } from './api.js';
import Site from './site/Site.jsx';
import Layout from './admin/Layout.jsx';
import Entrar from './admin/Entrar.jsx';
import Painel from './admin/Painel.jsx';
import Orcamentos from './admin/Orcamentos.jsx';
import Encomendas from './admin/Encomendas.jsx';
import Clientes from './admin/Clientes.jsx';
import Oficinas from './admin/Oficinas.jsx';
import Financeiro from './admin/Financeiro.jsx';
import Materiais from './admin/Materiais.jsx';
import Portfolio from './admin/Portfolio.jsx';
import Configuracoes from './admin/Configuracoes.jsx';
import Produtos from './admin/Produtos.jsx';
import Vendas from './admin/Vendas.jsx';

function Protegido({ children }) {
  return sessao.token() ? children : <Navigate to="/admin/entrar" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Site />} />
      <Route path="/admin/entrar" element={<Entrar />} />
      <Route path="/admin" element={<Protegido><Layout /></Protegido>}>
        <Route index element={<Painel />} />
        <Route path="orcamentos" element={<Orcamentos />} />
        <Route path="encomendas" element={<Encomendas />} />
        <Route path="produtos" element={<Produtos />} />
        <Route path="vendas" element={<Vendas />} />
        <Route path="clientes" element={<Clientes />} />
        <Route path="oficinas" element={<Oficinas />} />
        <Route path="financeiro" element={<Financeiro />} />
        <Route path="materiais" element={<Materiais />} />
        <Route path="portfolio" element={<Portfolio />} />
        <Route path="configuracoes" element={<Configuracoes />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
