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
3. Acesse:
   - Site: http://localhost:8080
   - Sistema: http://localhost:8080/admin
   - Login inicial: `lenne@atelie.local` / `trocar123`. Troque a senha em Configurações.

**Para mexer no visual com recarga automática:** deixe o backend rodando e, em `frontend`, rode `npm run dev`. Depois abra http://localhost:5173.

## Publicar na VPS (Docker)

1. Copie a pasta para a VPS, por exemplo `/opt/lenne-atelie`.
2. Crie o `.env` a partir do `.env.exemplo` e preencha senhas fortes, `JWT_SECRET` e o e-mail da Lenne.
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

**Backups.** O banco fica no volume `banco` e as fotos no volume `uploads`. Para um backup do banco:
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

## Segurança

- Senhas guardadas com BCrypt.
- Login com limite de tentativas.
- Formulários públicos com limite por IP e campo anti-robô.
- Fotos validadas pelo conteúdo do arquivo (JPG, PNG ou WEBP, até 8 MB).
- Rotas `/api/**` exigem login, exceto `/api/publico/**`.
