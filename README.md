# Lenne Carvalho Ateliê — site e sistema de gestão

Site do ateliê de macramê e sistema de gestão, num único aplicativo.
Desenvolvido pela MS Soluções Inteligentes.

- **Site público** (`/`): serviços, portfólio, agenda de oficinas com inscrição e Pix manual, formulário de orçamento.
- **Sistema** (`/admin`): painel, orçamentos, encomendas, oficinas e inscrições, clientes, financeiro, materiais, portfólio e configurações.

**Tecnologias:** Spring Boot 3.4 (Java 21), MySQL 8 com Flyway, React 18 + Vite, login com JWT.

## Como o fluxo funciona

1. **Orçamento pelo site.** O pedido é salvo como "Novo" em Orçamentos, e a cliente continua a conversa no WhatsApp.
2. **Aprovar e criar encomenda.** Cadastra a cliente (ou reaproveita pelo WhatsApp) e cria a encomenda com valor total e sinal.
3. **Registrar pagamento.** O pagamento entra no financeiro. Quando o sinal é pago, a encomenda passa sozinha para "Em produção".
4. **Encomenda até a entrega.** Os status são: Em produção → Pronta → Entregue. O saldo em aberto aparece em "A receber".
5. **Oficina pelo site.** A aluna se inscreve e vê a chave Pix na tela.
   - Ao clicar em "Enviar comprovante", a inscrição vira "Pix a conferir".
   - Quando a turma lota, as novas inscrições vão para a lista de espera.
6. **Confirmar Pix.** A inscrição fica confirmada, a aluna entra em Clientes e o valor entra no financeiro. Desfazer a confirmação remove o lançamento.
7. **Materiais.** Em "Comprei", o estoque soma e a compra pode ir para as despesas. Em "Usei", o estoque desconta. Abaixo do mínimo, aparece um aviso no painel.

### Custos, preço e pronta-entrega

1. **Configurações › Precificação.** Ela informa quanto quer ganhar por mês, as horas de produção, os custos fixos e a margem padrão.
   - O sistema calcula o valor da hora e o custo fixo por hora.
   - Esses dados não aparecem no site.
2. **Produtos (fichas técnicas).** Cada peça tem materiais por unidade, horas de trabalho e embalagem. O preço é montado assim:
   ```
   custo = materiais + horas × valor da hora + horas × custo fixo por hora + embalagem
   preço sugerido = custo × (1 + margem), arredondado para cima no real
   ```
   A ficha mostra o lucro por peça e quanto rende a hora com o preço que ela cobra.
3. **Encomenda com ficha.** Os materiais da ficha × quantidade saem do estoque sozinhos quando a encomenda entra em produção, uma única vez.
   - Material extra pode ser registrado na encomenda.
   - Um material pode ser devolvido ao estoque.
   - A encomenda mostra os custos reais, o ganho, o valor por hora frente à meta e o lucro.
4. **Comprei material com valor.** O custo por unidade passa a ser o da última compra, e as fichas se atualizam.
5. **Pronta-entrega.**
   - "Produzi" soma peças ao estoque e baixa os materiais.
   - Em **Vendas**, o caixa rápido (feito para o celular, na feira) tira as peças do estoque e lança a entrada no financeiro.
   - Cancelar a venda devolve as peças e remove a entrada.

O estoque de material pode ficar negativo; nesse caso aparece como "registre a compra". Assim a produção nunca fica travada por um cadastro desatualizado.

### Relatórios, documentos, agenda, lembretes e fotos

- **Relatórios:** período livre ou atalhos (este mês, mês passado, 3 meses, ano).
  - Mostra entradas, saídas e resultado, comparados com o mesmo trecho do período anterior.
  - Traz um gráfico de 12 meses, as entradas por frente, quanto rendeu cada peça entregue (ganho e valor por hora frente à meta), a pronta-entrega, os mais vendidos e os materiais mais usados.
  - Exporta em **Excel** (uma aba por assunto, mais todos os lançamentos) e em **PDF**.
- **Proposta em PDF:** gerada no orçamento, com a marca do ateliê, valor, sinal, condições, validade e chave Pix.
- **Contrato em PDF:** gerado na encomenda, com as partes, objeto, valor e pagamentos, prazo, regras de cancelamento e assinaturas.
  - Os textos são editáveis em Configurações › Proposta e contrato.
  - O CPF/CNPJ e o endereço da cliente ficam no cadastro dela. Se faltarem, o contrato sai com espaço para preencher à mão.
- **Agenda:** calendário do mês com entregas, oficinas, compromissos (visitas técnicas, provas, dias de produção, compras) e aniversários de clientes. No celular, vira pontinhos coloridos e a lista do dia.
- **Lembretes de WhatsApp:** o sistema escreve a mensagem e ela só confere e envia (link wa.me), depois marca "Já enviei". Os tipos:
  - oficina hoje ou amanhã, para as confirmadas;
  - Pix pendente de oficina;
  - peça pronta;
  - entrega amanhã;
  - saldo em aberto (volta depois de 7 dias);
  - aniversário.
- **Fotos da encomenda:** referência da cliente e peça pronta.
  - Ficam numa pasta **privada**, fora do site, e só abrem com login.
  - A foto da peça pronta vai para o portfólio com um toque: o sistema faz uma cópia pública. A referência nunca vai, porque costuma ser imagem de outra pessoa.

### Compras, contas fixas, certificados, equipe e depoimentos

- **Materiais › Compras e fornecedores:** cadastro de fornecedores e compra com vários materiais de uma vez.
  - Cada compra soma no estoque, atualiza o custo de cada material (as fichas acompanham) e vira despesa no financeiro.
  - O frete é dividido entre os itens pelo valor de cada um, para o custo por unidade ser o real.
  - Na hora de lançar, o sistema mostra se o material saiu mais caro ou mais barato que o custo atual.
  - O botão de histórico em cada material mostra todos os preços pagos e o último preço em cada fornecedor, com o mais barato destacado.
  - Cancelar uma compra tira os materiais do estoque, apaga a despesa e volta o custo ao da compra anterior.
- **Financeiro › Contas fixas:** aluguel, energia, internet, MEI…
  - Cada conta tem valor previsto e dia de vencimento (dia 31 vence no último dia dos meses curtos).
  - "Paguei" lança a despesa em "Contas fixas", ligada ao mês; o mesmo mês não é pago duas vezes.
  - O painel avisa as contas vencidas e as que vencem em até 5 dias, e a agenda mostra os vencimentos.
  - Em Configurações › Precificação, "Usar as contas fixas" faz o custo fixo por hora acompanhar sozinho a soma das contas marcadas.
- **Certificado das oficinas:** PDF em A4 deitado, com as fontes do site (Allura e Nunito, incluídas no projeto sob a licença OFL).
  - Sai por aluna ou da turma toda (uma página por aluna confirmada), com carga horária, local, conteúdo e código de verificação.
  - A carga horária pode ser informada na oficina; em branco, usa a duração entre início e término.
  - A aluna recebe um link (`/certificado/<código>`) que confirma o certificado e baixa o PDF depois que a oficina começa.
  - Um dia depois da oficina, os lembretes trazem a mensagem de agradecimento com o link.
- **Equipe (Configurações › Equipe):** acesso de ajudante para quem ajuda na produção.
  - A ajudante vê encomendas (sem valores), registra material usado, marca como pronta ou entregue, envia fotos, vê agenda e oficinas, baixa certificados, lança "Produzi" e vende no caixa.
  - Ela não vê valor cobrado, custos, saldos, financeiro, relatórios, compras, contas, orçamentos, clientes nem configurações. O servidor bloqueia essas rotas e tira os valores das respostas, não só da tela.
  - Desativar o acesso bloqueia na hora, mesmo com o login aberto. Sempre fica pelo menos uma administradora ativa.
- **Portfólio › Depoimentos:** texto, quem é, estrelas, foto opcional, ordem e publicar ou esconder.
  - Os publicados aparecem no site depois do portfólio (no celular, em carrossel).
  - Alguns dias depois de uma entrega quitada, os lembretes sugerem pedir um depoimento à cliente.
  - Os depoimentos "Exemplo — …" do banco de testes são só para ver o visual: apague antes de publicar o site.

### Cursos online e catálogo

- **Cursos online (menu Cursos online):** curso gravado com aulas em vídeo.
  - Os vídeos ficam no YouTube (como "não listado"), no Vimeo ou no Google Drive. A Lenne só cola o link, e o sistema transforma em player. Links de outros sites são recusados.
  - Cada curso tem capa, resumo, descrição, nível, lista de materiais, ordem das aulas e aulas abertas como amostra.
  - No site, a aluna se inscreve e vê a chave Pix. Depois de "Enviar comprovante", a inscrição fica como "Pix a conferir" (contador no menu).
  - "Confirmar Pix e liberar" cadastra a cliente, lança a entrada em "Cursos online" no financeiro e libera o acesso. O lembrete do dia traz a mensagem com o link, e também há o botão "Enviar acesso".
  - A aluna assiste pelo link `/meu-curso/<código>`, sem senha, e marca as aulas concluídas. A Lenne vê o progresso de cada uma.
  - Os links dos vídeos só saem do servidor para quem tem o curso liberado (ou nas aulas abertas). Um vídeo não listado ainda pode ser repassado por quem tem o link; para conteúdo mais protegido, use o Vimeo com restrição de domínio.
  - Curso com valor zero é liberado na hora. Cancelar a inscrição desativa o link; voltar a liberação tira a entrada do financeiro.
- **Catálogo (Produtos › botão Catálogo):** a peça da ficha técnica aparece no site em `/catalogo` e numa seção da página inicial.
  - Tem texto para a cliente, até 8 fotos (a primeira é a capa), prazo de produção, ordem, e preço ou "sob consulta".
  - O preço é o que ela cobra: peça de pronta-entrega mostra o valor e as unidades disponíveis; peça sob encomenda mostra "a partir de".
  - O botão abre o WhatsApp já com o nome da peça. Custos e materiais nunca vão para o site.

### Identidade visual

- **Logos:** as versões do manual (empilhada, horizontal, circular e símbolo, cada uma também na versão clara para fundo escuro) estão em vetor em `frontend/public/marca/`. Foram extraídas direto do PDF do manual, com fundo transparente.
- **Onde aparecem:** topo do site e das páginas internas, menu do sistema, login, rodapé, painel, favicon e imagem de compartilhamento (`compartilhar.jpg`).
- **PDFs:** proposta, contrato, relatório e certificado usam a logo (PNG em `backend/src/main/resources/marca/`).
- **Cores:** marrom `#8D553A`, bege `#E3C4A8`, off-white `#F9F6EF` e verde `#00674E`, aplicados nos tokens do estilo clay em `estilo.css`. O manual escreve o verde como `#0067AE`, mas o RGB indicado (0, 103, 78) é `#00674E`.
- **Gráficos:** usam uma variação das cores, validada para daltonismo, porque o marrom e o verde da marca se confundem.
- **Fonte:** Montserrat no site, no sistema e nos PDFs (licença OFL, arquivos em `fontes/`). A Pinkerston é comercial e aparece só dentro das logos.

## Rodar no computador (Windows + NetBeans)

**Pré-requisitos:**
- JDK 21
- MySQL local (usuário `root`, senha `root` por padrão)
- Node.js 20 ou mais novo

**Passos:**

1. Monte o site uma vez. Isso gera o React dentro de `backend/src/main/resources/static`:
   ```
   cd frontend
   npm install
   npm run build
   ```
2. No NetBeans, abra a pasta `backend` (projeto Maven) e rode `AtelieApplication`.
   - O banco `lenne_atelie` é criado sozinho, e o Flyway cria as tabelas.
   - Se sua senha do MySQL não for `root`, defina a variável `DB_PASSWORD`.
   - **Configuração só do seu computador** (senha do MySQL, porta…): não altere o `application.yml` do projeto. Crie `backend/config/application.yml` só com o que muda, por exemplo:
     ```yaml
     spring:
       datasource:
         password: minha-senha
     ```
     O Spring lê esse arquivo por cima do padrão, e o Git ignora a pasta. Assim o `git pull` nunca entra em conflito.
3. Acesse:
   - Site: http://localhost:8080
   - Sistema: http://localhost:8080/admin
   - Login inicial: `lenne@atelie.local` / `trocar123`. Troque a senha em Configurações.

**Para mexer no visual com recarga automática:** deixe o backend rodando e, em `frontend`, rode `npm run dev`. Depois abra http://localhost:5173.

## Publicar na VPS (Docker)

1. Copie a pasta para a VPS, por exemplo `/opt/lenne-atelie`.
2. Crie o `.env` a partir do `.env.exemplo` e preencha senhas fortes, `JWT_SECRET`, o e-mail da Lenne e `URL_PUBLICA` (endereço do site, usado nos links de certificado).
3. Suba os serviços:
   ```
   docker compose up -d --build
   ```
   O sistema fica em `127.0.0.1:8085` (ou na `APP_PORTA` definida).
4. No Nginx, aponte o domínio para essa porta e ative HTTPS com o Certbot:
   ```nginx
   server {
       server_name lennecarvalhoatelie.com.br www.lennecarvalhoatelie.com.br;
       client_max_body_size 10M;
       location / {
           proxy_pass http://127.0.0.1:8085;
           proxy_set_header Host $host;
           proxy_set_header X-Forwarded-For $remote_addr;
           proxy_set_header X-Forwarded-Proto $scheme;
       }
   }
   ```
   Depois rode: `certbot --nginx -d lennecarvalhoatelie.com.br -d www.lennecarvalhoatelie.com.br`

**Backups.** O banco fica no volume `banco`, as fotos do site no volume `uploads` e as fotos privadas das encomendas no volume `privado`. Para um backup do banco:
```
docker compose exec banco sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" lenne_atelie' > backup.sql
```

## Testes

```
cd backend
mvn test
```

Os testes cobrem:
- login
- orçamento → encomenda → pagamento
- inscrição com Pix e lista de espera
- materiais
- envio de fotos
- cálculo da ficha técnica e do preço sugerido
- baixa automática de materiais e lucro da encomenda
- venda de pronta-entrega e cancelamento
- relatório com números conferidos à mão, Excel e PDF
- proposta e contrato em PDF
- agenda e lembretes
- fotos privadas e publicação no portfólio
- compra com frete rateado, histórico de preços e cancelamento
- contas fixas, pagamento no financeiro e precificação automática
- certificados (aluna, turma e link público)
- acesso de ajudante sem valores e com rotas bloqueadas
- depoimentos com foto no site e lembrete de pedir depoimento
- links de vídeo aceitos e recusados
- curso vendido pelo site, liberação, progresso, lembrete de acesso e cancelamento
- catálogo com fotos, ordem, preço ou sob consulta, sem custos

## Atualizar a VPS

```
git pull
docker compose up -d --build
```

O Flyway aplica sozinho as mudanças no banco, como a V5 desta etapa, sem apagar dados.

## Segurança

- Senhas guardadas com BCrypt.
- Login com limite de tentativas.
- Formulários públicos com limite por IP e campo anti-robô.
- Fotos validadas pelo conteúdo do arquivo (JPG, PNG ou WEBP, até 8 MB).
- Rotas `/api/**` exigem login, exceto `/api/publico/**`.
