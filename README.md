# VFNews (vfnews) -- Verificador de Fatos com Inteligencia Artificial

Plataforma full-stack de fact-checking que permite ao usuario colar uma noticia,
manchete ou alegacao (especialmente sobre politica e eleicoes brasileiras) e
receber uma analise de veracidade. O sistema combina fontes externas de
verificacao (Google Fact Check API) com um modelo de Machine Learning treinado
localmente via Naive Bayes Multinomial.

---

## Sumario

- [Arquitetura](#arquitetura)
- [Stack Tecnologico](#stack-tecnologico)
- [Estrutura do Repositorio](#estrutura-do-repositorio)
- [Pre-requisitos](#pre-requisitos)
- [Executando em Desenvolvimento](#executando-em-desenvolvimento)
- [Executando em Producao](#executando-em-producao)
- [Variaveis de Ambiente](#variaveis-de-ambiente)
- [Portas](#portas)
- [Integracao com Dataset FACTCK.BR](#integracao-com-dataset-factckbr)
- [API de Checagem](#api-de-checagem)
- [Contribuindo](#contribuindo)
- [Seguranca](#seguranca)
- [Licenca](#licenca)

---

## Arquitetura

O projeto segue uma arquitetura monorepo com backend em Java 21 (Spring Boot 4)
e frontend em React 19 (TypeScript + Vite).

```
Cliente (Browser)
       |
       | POST /api/check/ (claim em JSON)
       v
+-------------------------------+
|  Backend - Spring Boot 4      |
|  Porta 8080                   |
|                               |
|  +-------------------------+  |
|  | CheckClaimController    |  |
|  +------------+------------+  |
|               |               |
|  +------------v------------+  |
|  | FactCheckService        |  |
|  | 1. Google Fact Check    |  |
|  |    API                  |  |
|  | 2. ML (Naive Bayes)     |  |
|  | 3. Fallback amigavel    |  |
|  +------------+------------+  |
|               |               |
|  +------------v------------+  |
|  | Database (SQLite/       |  |
|  | PostgreSQL)             |  |
|  +-------------------------+  |
|                               |
|  +-------------------------+  |
|  | MLService               |  |
|  | Smile NLP 3.1           |  |
|  | Naive Bayes Multinomial |  |
|  +-------------------------+  |
+-------------------------------+
```

O sistema executa tres etapas de seeding quando o banco de dados esta vazio:

1. **FACTCK.BR dataset**: importa 1309 afirmacoes verificadas de tres agencias
   brasileiras (Aos Fatos, Lupa, Truco).
2. **Google Fact Check API**: consulta a API externa para obter checagens
   recentes sobre temas de politica brasileira.
3. **Fallback**: se o total de entradas no banco for inferior a 100, entradas
   adicionais sao inseridas.

Apos o seeding, o modelo de Machine Learning e treinado automaticamente.

---

## Stack Tecnologico

| Camada       | Tecnologia                            | Versao  |
|--------------|---------------------------------------|---------|
| Frontend     | React, TypeScript, Vite               | 19 / 6  |
| Frontend     | styled-components                     | 6       |
| Backend      | Spring Boot, Java                     | 4 / 21  |
| Backend (ML) | Smile NLP (Natural Language Processor)| 3.1.1   |
| Database     | SQLite (desenvolvimento)              | 3.45.2  |
| Database     | PostgreSQL (producao)                 | -       |
| ORM          | Hibernate + JPA                       | -       |
| Build        | Maven (backend), Vite (frontend)      | -       |
| Deploy       | Railpack                              | -       |

---

## Estrutura do Repositorio

```
vfnews/
+-- .github/
|   +-- workflows/               (CI/CD futuros)
+-- .gitignore
+-- LICENSE                       (GNU AGPL v3)
+-- README.md                    (este arquivo)
+-- CODE_OF_CONDUCT.md
+-- SECURITY.md
+-- CONTRIBUTING.md
+-- flake.nix                    (Nix dev shell)
+-- mise.toml                    (Runtime versions)
+-- railpack.json                (Railpack deploy)
+-- backend/
|   +-- pom.xml
|   +-- src/
|   |   +-- main/
|   |   |   +-- java/...        (codigo Java)
|   |   |   +-- resources/
|   |   |       +-- application.yml
|   |   |       +-- datasets/
|   |   |           +-- FACTCKBR.tsv
+-- frontend/
    +-- vfnews/
        +-- package.json
        +-- index.html
        +-- vite.config.ts
        +-- src/
            +-- App.tsx
            +-- main.tsx
            +-- style.ts
            +-- components/
            |   +-- ChatInput/
            |   +-- ChatMessage/
            |   +-- Logo/
            +-- services/
                +-- api.ts
```

---

## Pre-requisitos

- **Java Development Kit (JDK)** 21 ou superior
- **Node.js** 22 ou superior
- **npm** (acompanha o Node.js)
- **Maven** (opcional; o wrapper `./mvnw` incluso no backend pode ser usado)

---

## Executando em Desenvolvimento

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

O backend iniciara na porta `8080`. Durante o primeiro startup, o banco de dados
sera criado automaticamente (arquivo `factchecker.sqlite3` no diretorio raiz do
projeto) e o dataset sera populado com as entradas do FACTCK.BR, da Google Fact
Check API e do fallback embutido.

### Frontend

Em um segundo terminal:

```bash
cd frontend/vfnews
npm install
npm run dev
```

O servidor de desenvolvimento do Vite iniciara na porta `5173`. Ele fara proxy
das requisicoes `/api/*` para o backend na porta `8080`. Se precisar alterar a
URL da API, defina a variavel de ambiente `VITE_API_URL` (veja a secao de
variaveis de ambiente).

### Usando Nix (opcional)

Se voce utiliza Nix, um shell de desenvolvimento esta disponivel:

```bash
nix develop
```

Isso fornecera JDK 21, Maven e Node.js 22 no PATH.

---

## Executando em Producao

O deploy utiliza o Railpack (configurado em `railpack.json`). O processo de
build realiza os seguintes passos:

1. Instala as dependencias do frontend (`npm install --prefix frontend/vfnews`).
2. Compila o frontend (`npm run build --prefix frontend/vfnews`).
3. Copia os artefatos compilados do frontend para
   `backend/src/main/resources/static/` (o Spring Boot serve o frontend
   diretamente nesta configuracao).
4. Compila o backend com Maven, gerando um JAR executavel em
   `backend/target/factchecker-0.0.1-SNAPSHOT.jar`.
5. O comando de inicio executa o JAR com a JRE empacotada.

```bash
# Alternativamente, build manual
cd frontend/vfnews && npm run build
cp -r dist/* ../../backend/src/main/resources/static/
cd ../../backend && ./mvnw package -DskipTests
java -jar target/factchecker-0.0.1-SNAPSHOT.jar
```

Em producao, recomenda-se usar PostgreSQL:

```
DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
DATABASE_URL=jdbc:postgresql://<host>:<port>/<database>
DATABASE_USERNAME=<usuario>
DATABASE_PASSWORD=<senha>
```

---

## Variaveis de Ambiente

| Variavel                    | Obrigatoria | Padrao                                    | Descricao                                   |
|-----------------------------|-------------|-------------------------------------------|---------------------------------------------|
| `PORT`                      | Nao         | `8080`                                    | Porta do servidor HTTP do Spring Boot       |
| `GOOGLE_FACTCHECK_API_KEY`  | Sim         | (vazio)                                   | Chave de API do Google Fact Check Tools     |
| `DATABASE_PLATFORM`         | Nao         | `org.hibernate.community.dialect.SQLiteDialect` | Dialeto Hibernate do banco           |
| `DATABASE_URL`              | Nao         | `jdbc:sqlite:factchecker.sqlite3`         | URL de conexao JDBC                         |
| `DATABASE_USERNAME`         | Nao         | (vazio)                                   | Usuario do banco (apenas PostgreSQL)        |
| `DATABASE_PASSWORD`         | Nao         | (vazio)                                   | Senha do banco (apenas PostgreSQL)          |
| `VITE_API_URL`              | Nao         | `/api/check/`                             | URL completa da API para o frontend         |

---

## Portas

| Servico  | Ambiente de Desenvolvimento | Producao (padrao) |
|----------|-----------------------------|-------------------|
| Backend  | `8080`                      | `8080`            |
| Frontend | `5173`                      | Empacotado no JAR |
| Database | --                          | --                |

Em desenvolvimento, o frontend roda em `localhost:5173` e faz requisicoes para
`localhost:8080`. Em producao, o frontend e servido diretamente pelo Spring Boot
como conteudo estatico, utilizando a mesma porta do backend.

---

## Integracao com Dataset FACTCK.BR

O dataset [FACTCK.BR](https://github.com/jghm-f/FACTCK.BR) e um corpus de 1309
linhas contendo afirmacoes verificadas por agencias brasileiras de fact-checking.

### Colunas do Dataset

| Coluna            | Descricao                                    |
|-------------------|----------------------------------------------|
| URL               | URL do artigo de checagem                    |
| Author            | URL da agencia de verificacao                |
| datePublished     | Data de publicacao da checagem               |
| claimReviewed     | Texto da alegacao verificada                 |
| reviewBody        | Corpo do texto de verificacao                |
| Title             | Titulo do artigo                             |
| ratingValue       | Classificacao numerica (1-5)                 |
| bestRating        | Tamanho da escala de classificacao           |
| alternativeName   | Rotulo textual da classificacao              |

### Mapeamento para DatasetEntry

| DatasetEntry | Origem FACTCK.BR  | Descricao                                 |
|-------------|-------------------|-------------------------------------------|
| text        | claimReviewed     | Texto da alegacao (truncado em 500 chars) |
| label       | alternativeName   | Mapeado para "true", "false" ou "mixed"   |
| keywords    | texto completo    | Extraidas automaticamente do conteudo     |
| publisher   | Author (URL)      | Nome amigavel da agencia                  |

### Mapeamento de Rotulos

| Rotulo original FACTCK.BR               | Rotulo final DatasetEntry |
|-----------------------------------------|---------------------------|
| falso, false, fake, mentira, engano     | false                     |
| verdadeiro, true, real                  | true                      |
| distorcido, exagerado, insustentavel,   | mixed                     |
| impreciso, subestimado, discutivel,     |                           |
| impossivel provar, sem contexto, etc.   |                           |

### Referencia Academica

```
Nascimento et al., "FACTCK.BR: A Dataset for the Study of Fake News in
Portuguese", WebMedia '19, October 29 - November 1, 2019, Rio de Janeiro,
Brazil. DOI: 10.1145/3323503.3361698
```

---

## API de Checagem

### `POST /api/check/`

**Request body:**

```json
{
  "claim": "As urnas eletronicas foram fraudadas nas eleicoes de 2022."
}
```

**Response (exemplo):**

```json
{
  "id": 1,
  "claim": "As urnas eletronicas foram fraudadas nas eleicoes de 2022.",
  "result": "Esta alegacao foi verificada por uma fonte externa de fact-checking e classificada como falsa ou enganosa.",
  "source": "API",
  "rating": "falso",
  "publisher": "Aos Fatos",
  "url": "https://aosfatos.org/...",
  "matchedClaim": "E falso que urnas foram fraudadas nas eleicoes de 2022",
  "createdAt": "2026-05-28T12:00:00"
}
```

### `GET /api/dataset`

Lista paginada de entradas do dataset. Parametros: `page` (default 0), `size`
(default 50).

### `GET /api/dataset/stats`

Estatisticas do dataset: total, distribuicao por label, por keyword, por
publisher, e ultimas 10 entradas.

### `POST /api/dataset/train`

Retreina o modelo de Machine Learning com os dados atuais do dataset.

### `GET /api/ml/metrics`

Retorna as metricas do ultimo treinamento do modelo (acuracia, precisao, recall,
F1-score, matriz de confusao).

---

## Contribuindo

Consulte o arquivo [CONTRIBUTING.md](CONTRIBUTING.md) para obter orientacoes
detalhadas sobre como contribuir com o projeto.

## Seguranca

Para reportar vulnerabilidades, consulte o arquivo [SECURITY.md](SECURITY.md).

---

## Licenca

Copyright (C) 2026 Gustavo Muniz dos Anjos.

Este programa e software livre: voce pode redistribui-lo e/ou modifica-lo sob os
termos da GNU Affero General Public License (AGPL) versao 3, conforme publicada
pela Free Software Foundation.

Este programa e distribuido na esperanca de que seja util, mas SEM QUALQUER
GARANTIA; sem mesmo a garantia implicita de COMERCIALIZACAO ou ADEQUACAO A UM
DETERMINADO PROPOSITO. Veja a GNU Affero General Public License para mais
detalhes.

Uma copia da licenca esta incluida no arquivo [LICENSE](LICENSE) deste
repositorio.
