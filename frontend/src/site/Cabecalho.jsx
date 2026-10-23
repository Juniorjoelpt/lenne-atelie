import { Link } from 'react-router-dom';
import Icone from '../componentes/Icone.jsx';

/** Topo das páginas internas do site (catálogo, curso, área da aluna). */
export default function Cabecalho({ voltar = '/', rotuloVoltar = 'Voltar ao site' }) {
  return (
    <header className="envolve">
      <div className="cabecalho">
        <Link to="/" className="marca"><strong>Lenne Carvalho</strong><span>ateliê</span></Link>
        <nav aria-label="Principal">
          <Link to={voltar}><Icone nome="seta" tamanho={16} style={{ transform: 'rotate(180deg)', verticalAlign: '-3px', marginRight: 4 }} />{rotuloVoltar}</Link>
        </nav>
      </div>
    </header>
  );
}
