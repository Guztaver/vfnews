# VFNews Frontend -- Interface de Verificacao de Fatos

Interface web da plataforma VFNews, construida com React 19, TypeScript e Vite.
Apresenta uma interface de chat onde o usuario pode colar noticias, manchetes ou
alegacoes e receber analises de veracidade em tempo real.

---

## Sumario

- [Stack Tecnologico](#stack-tecnologico)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Pre-requisitos](#pre-requisitos)
- [Executando em Desenvolvimento](#executando-em-desenvolvimento)
- [Build de Producao](#build-de-producao)
- [Configuracao da API](#configuracao-da-api)
- [Componentes](#componentes)
- [Fluxo de Mensagens](#fluxo-de-mensagens)
- [Temas e Estilos](#temas-e-estilos)
- [Empacotamento para Producao](#empacotamento-para-producao)

---

## Stack Tecnologico

| Componente       | Tecnologia              | Versao |
|------------------|-------------------------|--------|
| Framework        | React                   | 19     |
| Linguagem        | TypeScript              | 6      |
| Build Tool       | Vite                    | 5      |
| Estilizacao      | styled-components       | 6      |
| Icones           | lucide-react            | 1.8    |
| Lint             | ESLint                  | 9      |

---

## Estrutura do Projeto

```
frontend/vfnews/
+-- index.html                  (HTML entry point)
+-- package.json
+-- tsconfig.json               (configuracao base do TypeScript)
+-- tsconfig.app.json           (config para codigo da aplicacao)
+-- tsconfig.node.json          (config para codigo do Node/Vite)
+-- vite.config.ts              (configuracao do Vite)
+-- eslint.config.js            (configuracao do ESLint)
+-- src/
|   +-- main.tsx                (ponto de entrada React)
|   +-- App.tsx                 (componente principal)
|   +-- style.ts                (estilos globais com styled-components)
|   +-- components/
|   |   +-- ChatInput/
|   |   |   +-- index.tsx       (campo de texto e botao de envio)
|   |   +-- ChatMessage/
|   |   |   +-- index.tsx       (bolha de mensagem user/assistant)
|   |   +-- Logo/
|   |       +-- index.tsx       (logotipo do VFNews)
|   +-- services/
|   |   +-- api.ts              (cliente HTTP para o backend)
|   +-- assets/
|       +-- images/
|           +-- logo.png
```

---

## Pre-requisitos

- Node.js 22 ou superior
- npm (acompanha o Node.js)
- Backend VFNews rodando (para testar a integracao)

---

## Executando em Desenvolvimento

```bash
cd frontend/vfnews
npm install
npm run dev
```

O servidor de desenvolvimento do Vite iniciara em `http://localhost:5173`. O
frontend espera que o backend esteja rodando em `http://localhost:8080` e faz
requisicoes para `/api/check/`. Se precisar alterar a URL da API, utilize a
variavel de ambiente `VITE_API_URL`.

### Variavel de Ambiente

| Variavel       | Padrao         | Descricao                          |
|----------------|----------------|------------------------------------|
| `VITE_API_URL` | `/api/check/`  | URL completa do endpoint de checagem |

Para usar um backend em outra porta:

```bash
VITE_API_URL=http://localhost:8080/api/check/ npm run dev
```

Para usar um backend remoto:

```bash
VITE_API_URL=https://meu-servidor.com/api/check/ npm run dev
```

### Scripts Disponiveis

| Comando           | Descricao                                     |
|-------------------|-----------------------------------------------|
| `npm run dev`     | Inicia o servidor de desenvolvimento (Vite)   |
| `npm run build`   | Compila o TypeScript e gera o bundle em `dist/` |
| `npm run preview` | Servidor local para pre-visualizar o build    |
| `npm run lint`    | Executa o ESLint em todo o codigo-fonte       |

---

## Build de Producao

```bash
npm run build
```

O diretorio `dist/` sera gerado com os arquivos estaticos otimizados. Esses
arquivos devem ser copiados para o diretorio `static/` do Spring Boot para que
o backend sirva o frontend:

```bash
cp -r dist/* ../../backend/src/main/resources/static/
```

---

## Configuracao da API

O servico de API esta definido em `src/services/api.ts`:

```typescript
const API_URL = import.meta.env.VITE_API_URL || "/api/check/";
```

O endpoint espera um `POST` com um corpo JSON contendo o campo `claim`. A
resposta e um objeto `FactCheckResult` com a seguinte estrutura:

```typescript
interface FactCheckResult {
  id: number;
  claim: string;          // texto original enviado pelo usuario
  result: string;         // descricao textual do resultado
  source: "API" | "ML";   // origem da checagem
  rating: string | null;  // classificacao (ex.: "falso", "verdadeiro")
  publisher: string | null; // nome da fonte
  url: string | null;     // URL do artigo original
  matchedClaim: string | null; // alegacao correspondente (API)
  created_at: string;
}
```

---

## Componentes

### App (componente principal)

Orquestra o estado global da aplicacao:

- `messages`: array de mensagens (role user ou assistant).
- `loading`: booleano que indica se uma requisicao esta em andamento.
- Gerencia o auto-scroll da lista de mensagens.

O componente `App` e responsavel por:

1. Receber o texto do `ChatInput`.
2. Disparar a requisicao via `checkClaim()`.
3. Construir a resposta textual do assistente via `buildAssistantResponse()`.
4. Adicionar a mensagem do usuario e a resposta do assistente ao estado.

### ChatInput

Componente de entrada de texto com as seguintes caracteristicas:

- Textarea com auto-resize (altura maxima de 120px).
- Placeholder: "Cole uma noticia ou manchete para verificar..."
- Envio ao pressionar Enter (Shift+Enter para nova linha).
- Botao de envio com icone de Send (lucide-react).
- Estado de loading: desabilita o botao e exibe um spinner (Loader2).

### ChatMessage

Renderiza uma mensagem individual no chat:

- Bolha com avatar diferenciado (usuario vs. assistente).
- Animacao de entrada (fade in com translacao).
- Animacao de digitacao (tres pontos pulsantes) quando `typing` e `true`.
- Suporte a formatacao Markdown basica: texto em **negrito** usando `**`.
- Secao de metadados ao final (quando o resultado esta presente):
  - Selo da fonte ("Google Fact Check API" ou "Modelo Local").
  - Nome do publisher.
  - Link para a fonte original.

### Logo

Exibe o logo do VFNews no topo do layout.

---

## Fluxo de Mensagens

```
Usuario digita "As urnas foram fraudadas?"
         |
         v
App.handleSend("As urnas foram fraudadas?")
         |
         +--> Adiciona mensagem do usuario ao estado
         +--> setLoading(true)
         +--> Exibe animacao de digitacao
         |
         v
api.checkClaim("As urnas foram fraudadas?")
         |
         v
[Backend] POST /api/check/ { claim: "..." }
         |
         v
api retorna FactCheckResult
         |
         v
App.buildAssistantResponse(result)
  - Verifica source (API ou ML)
  - Verifica rating (falso, verdadeiro, inconclusivo)
  - Monta texto com explicacoes e recomendacoes
         |
         +--> Adiciona mensagem do assistente ao estado
         +--> setLoading(false)
         +--> Auto-scroll para o final
```

### Logica de Construcao da Resposta

A funcao `buildAssistantResponse()` em `App.tsx` decide o texto a ser exibido
com base nos campos `source`, `rating` e `result`:

1. **Fonte API com rating conhecido**: exibe "Esta alegacao foi verificada por
   uma fonte externa de fact-checking e classificada como X." Inclui a alegacao
   correspondente encontrada pela API quando disponivel.

2. **Fonte API com matchedClaim**: inclui o texto da alegacao correspondente
   dentro do resultado.

3. **Fonte ML com rating conclusivo**: "Nao encontrei esta alegacao nas bases
   de fact-checking externas. Consultei meu modelo de Machine Learning local...
   Resultado: esta alegacao e provavelmente X." Inclui disclaimer sobre a
   natureza probabilistica da classificacao.

4. **Rating inconclusivo**: exibe mensagem amigavel com sugestoes de como
   prosseguir (consultar TSE, sites de fact-checking, etc.).

5. **Erro de rede**: mensagem de erro informando problema de conexao.

---

## Temas e Estilos

O projeto utiliza **styled-components** com tema escuro (dark mode). Os estilos
globais sao definidos em `src/style.ts`:

```typescript
import { createGlobalStyle } from "styled-components";

export const Global = createGlobalStyle`
  * {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
  }

  body {
    background: #0f172a;
    color: #e2e8f0;
    font-family: "Inter", system-ui, sans-serif;
    -webkit-font-smoothing: antialiased;
  }
  ...
`;
```

### Paleta de Cores

| Token              | Cor       | Uso                                     |
|--------------------|-----------|-----------------------------------------|
| `#0f172a`          | Slate 900 | Fundo da pagina                         |
| `#1e293b`          | Slate 800 | Fundo de botoes e inputs                |
| `#334155`          | Slate 700 | Bordas                                  |
| `#64748b`          | Slate 500 | Texto secundario / placeholder          |
| `#94a3b8`          | Slate 400 | Texto terciario                         |
| `#e2e8f0`          | Slate 200 | Texto principal                         |
| `#3b82f6`          | Blue 500  | Botao de envio ativo / foco             |
| `#1d4ed8`          | Blue 700  | Bolha de mensagem do usuario            |
| `#facc15`          | Amber 400 | Destaque para texto em negrito          |
| `#0ea5e9`          | Sky 500   | Avatar do assistente (gradiente)        |

---

## Empacotamento para Producao

Em producao, o frontend compilado e servido diretamente pelo Spring Boot. O
arquivo `railpack.json` na raiz do projeto automatiza esse processo:

```json
{
  "steps": {
    "build": {
      "commands": [
        "npm install --prefix frontend/vfnews",
        "npm run build --prefix frontend/vfnews",
        "mkdir -p backend/src/main/resources/static",
        "cp -r frontend/vfnews/dist/* backend/src/main/resources/static/"
      ]
    }
  }
}
```

---

## Referencias

- React 19: https://react.dev/
- Vite: https://vite.dev/
- styled-components: https://styled-components.com/
- lucide-react: https://lucide.dev/
