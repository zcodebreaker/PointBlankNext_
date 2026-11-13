<div align="center">

# KRYONIX NETWORK

### PointBlankNext_

**Portal web de Point Blank — da conta do jogador à loja e ao suporte.**

![Next.js](https://img.shields.io/badge/Next.js-16.3.2-111827?style=for-the-badge&logo=nextdotjs&logoColor=white)
![React](https://img.shields.io/badge/React-19.2.8-149ECA?style=for-the-badge&logo=react&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-E76F00?style=for-the-badge)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)

[Recursos](#recursos) · [Estrutura](#estrutura) · [Como executar](#como-executar) · [Comandos](#comandos)

</div>

---

## Sobre o projeto

O **Kryonix Network** reúne os serviços web da comunidade de Point Blank em uma aplicação com frontend em Next.js e API em Spring Boot. O portal integra contas de jogadores, notícias, rankings, downloads, loja virtual e atendimento por tickets.

O frontend utiliza React e TypeScript. O backend acessa o PostgreSQL via JDBC, gerencia as migrações com Flyway e utiliza Spring Security com JWT para autenticação.

## Recursos

| Área | Funcionalidades |
| :--- | :--- |
| 👤 **Conta do jogador** | Cadastro, login, verificação de e-mail, alteração de senha e informações da conta. |
| 🎮 **Perfil no jogo** | Consulta de dados do jogador e informações relacionadas ao clã. |
| 🏆 **Rankings** | Classificação de jogadores e clãs. |
| 📰 **Notícias** | Listagem de publicações, páginas de artigos, capas e imagens de carrossel. |
| 🛍️ **Loja virtual** | Catálogo de itens, compras com cash do jogo e histórico de pedidos. |
| 💬 **Suporte** | Abertura e acompanhamento de tickets, anexos e respostas da equipe. |
| 📥 **Downloads** | Informações de versão, links do cliente, espelhos e patches. |
| 🛠️ **Administração na API** | Gestão de contas, perfis administrativos, notícias, loja, tickets e downloads. |

## Estrutura

```text
Kryonix_Ntw/
├── backend/
│   ├── src/main/java/          # API, regras de negócio e acesso aos dados
│   ├── src/main/resources/
│   │   ├── db/migration/      # Migrações Flyway
│   │   └── application.yml   # Configuração da aplicação
│   ├── src/test/java/         # Testes do backend
│   ├── .env.example           # Modelo de variáveis de ambiente
│   ├── run-dev.ps1            # Inicialização local no PowerShell
│   └── pom.xml                # Dependências e build Maven
├── frontend/
│   ├── app/                   # Páginas, layouts, ações e rotas do Next.js
│   ├── components/            # Componentes da interface
│   ├── lib/                   # Integrações e utilitários
│   ├── public/                # Imagens, estilos e arquivos estáticos
│   ├── scripts/               # Scripts de desenvolvimento
│   ├── .env.example           # Modelo de configuração do site
│   └── package.json           # Dependências e comandos npm
├── .gitignore
└── read.md
```

## Como executar

### 1. Prepare o ambiente

Tenha disponíveis:

- **Node.js 20.9 ou superior** e npm.
- **JDK 21**, com `JAVA_HOME` configurado.
- **PostgreSQL**, com uma base do jogo compatível com as consultas da API.
- **Servidor SMTP** para o fluxo de verificação de e-mail.

O Maven Wrapper está incluído em `backend/`; ele baixa a distribuição Maven configurada no primeiro uso.

> **Banco de dados:** as migrações complementam a estrutura do jogo. Elas dependem de tabelas existentes, como `public.accounts`, e não provisionam uma base completa de Point Blank a partir de um banco vazio. A conexão padrão aponta para `PointBlankNextGen`.

### 2. Configure as variáveis

Na raiz do projeto, execute no PowerShell:

```powershell
Copy-Item backend/.env.example backend/.env
Copy-Item frontend/.env.example frontend/.env.local
```

Preencha os arquivos criados com os valores do seu ambiente:

| Arquivo | Configurações principais |
| :--- | :--- |
| `backend/.env` | `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` para a conexão com o banco. |
| `backend/.env` | `FLYWAY_DB_USERNAME` e `FLYWAY_DB_PASSWORD` para executar as migrações. |
| `backend/.env` | `APP_JWT_SECRET` com um segredo aleatório de pelo menos 32 bytes. |
| `backend/.env` | `SMTP_*` e `APP_MAIL_FROM` para o envio de e-mails. |
| `backend/.env` | `APP_PUBLIC_URL` e `APP_CORS_ALLOWED_ORIGINS` com o endereço do frontend. |
| `backend/.env` | `TURNSTILE_SECRET_KEY` e `TURNSTILE_ALLOWED_HOSTNAMES` para validar o captcha. |
| `frontend/.env.local` | `POINTBLANK_API_URL` com o endereço da API e `TURNSTILE_SITE_KEY` com a chave pública do captcha. |

Para desenvolvimento local, os scripts já utilizam chaves de teste do Turnstile quando as variáveis correspondentes não estão definidas. Se você copiou os modelos, substitua os placeholders por este par de teste:

```dotenv
# frontend/.env.local
TURNSTILE_SITE_KEY=1x00000000000000000000AA

# backend/.env
TURNSTILE_SECRET_KEY=1x0000000000000000000000000000000AA
TURNSTILE_ALLOWED_HOSTNAMES=localhost,127.0.0.1
```

Use as chaves do seu próprio widget no ambiente publicado. Os arquivos `.env` locais são ignorados pelo Git; os modelos `.env.example` permanecem no repositório.

### 3. Inicie o backend

Em um terminal na raiz do projeto:

```powershell
cd backend
.\run-dev.ps1
```

O script carrega `backend/.env` e inicia o Spring Boot. Por padrão, a API fica em **http://localhost:8080** e executa as migrações Flyway durante a inicialização.

### 4. Inicie o frontend

Em outro terminal, também a partir da raiz:

```powershell
cd frontend
npm ci
npm run dev
```

Acesse o portal em **http://localhost:3000**. O comando do frontend inicia apenas o site; mantenha o backend em execução no outro terminal.

## Comandos

### Frontend · execute em `frontend/`

| Comando | Finalidade |
| :--- | :--- |
| `npm ci` | Instalar as dependências conforme o lockfile. |
| `npm run dev` | Iniciar o site com o script de desenvolvimento. |
| `npm run dev:site` | Executar o servidor de desenvolvimento do Next.js diretamente. |
| `npm run typecheck` | Verificar os tipos TypeScript. |
| `npm run build` | Gerar o build de produção. |
| `npm start` | Servir o build de produção previamente gerado. |

### Backend · execute em `backend/`, no PowerShell

| Comando | Finalidade |
| :--- | :--- |
| `.\run-dev.ps1` | Carregar o `.env` e iniciar a API. |
| `.\mvnw.cmd test` | Executar os testes habilitados do backend. |
| `.\mvnw.cmd package` | Executar o build e gerar o JAR em `target/`. |

Os testes de integração com banco são habilitados por `RUN_DB_TESTS=true`. Para executá-los, disponibilize o banco e as variáveis necessárias no ambiente do processo; os comandos Maven diretos não carregam o `.env` pelo script `run-dev.ps1`.

---

<div align="center">

**Kryonix Network** · PointBlankNext_

Site, serviços e comunidade em um só projeto.

</div>
