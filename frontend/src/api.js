// Acesso à API do Spring Boot. O token de login fica no navegador da Lenne.
const CHAVE_TOKEN = 'atelie.token';
const CHAVE_NOME = 'atelie.nome';

export const sessao = {
  token: () => localStorage.getItem(CHAVE_TOKEN),
  nome: () => localStorage.getItem(CHAVE_NOME) || '',
  entrar(token, nome) {
    localStorage.setItem(CHAVE_TOKEN, token);
    localStorage.setItem(CHAVE_NOME, nome);
  },
  sair() {
    localStorage.removeItem(CHAVE_TOKEN);
    localStorage.removeItem(CHAVE_NOME);
  },
};

export class ErroApi extends Error {
  constructor(mensagem, status) {
    super(mensagem);
    this.status = status;
  }
}

async function chamar(metodo, url, corpo, { formData = false } = {}) {
  const headers = {};
  const token = sessao.token();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (corpo !== undefined && !formData) headers['Content-Type'] = 'application/json';

  let resp;
  try {
    resp = await fetch(url, {
      method: metodo,
      headers,
      body: corpo === undefined ? undefined : formData ? corpo : JSON.stringify(corpo),
    });
  } catch {
    throw new ErroApi('Sem conexão com o servidor. Verifique a internet e tente de novo.', 0);
  }

  if (resp.status === 401 && url.startsWith('/api/') && !url.startsWith('/api/auth/login')) {
    sessao.sair();
    if (window.location.pathname.startsWith('/admin')) {
      window.location.href = '/admin/entrar?expirou=1';
    }
    throw new ErroApi('Sua sessão expirou. Entre novamente.', 401);
  }
  if (resp.status === 204) return null;

  const texto = await resp.text();
  const dados = texto ? JSON.parse(texto) : null;
  if (!resp.ok) {
    throw new ErroApi(dados?.erro || 'Não foi possível concluir. Tente novamente.', resp.status);
  }
  return dados;
}

export const api = {
  get: (url) => chamar('GET', url),
  post: (url, corpo) => chamar('POST', url, corpo ?? {}),
  put: (url, corpo) => chamar('PUT', url, corpo),
  patch: (url, corpo) => chamar('PATCH', url, corpo),
  del: (url) => chamar('DELETE', url),
  enviar: (url, formData) => chamar('POST', url, formData, { formData: true }),
};
