# VFNews Backend -- Spring Boot Fact Checker API

Backend da plataforma VFNews, responsavel por receber alegacoes do usuario,
consultar fontes externas e locais de fact-checking, e retornar uma analise de
veracidade. Construido com Spring Boot 4 e Java 21, o servico utiliza a
biblioteca Smile NLP para o modelo de Machine Learning e suporta SQLite
(desenvolvimento) e PostgreSQL (producao).

---

## Sumario

- [Stack Tecnologico](#stack-tecnologico)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Pre-requisitos](#pre-requisitos)
- [Executando](#executando)
- [Configuracao](#configuracao)
- [Endpoints da API](#endpoints-da-api)
- [Fluxo de Checagem](#fluxo-de-checagem)
- [Seeding do Dataset](#seeding-do-dataset)
- [Modelo de Machine Learning](#modelo-de-machine-learning)
- [Banco de Dados](#banco-de-dados)
- [Integracao com FACTCK.BR](#integracao-com-factckbr)
- [Empacotando o Frontend](#empacotando-o-frontend)
- [Testes](#testes)

---

## Stack Tecnologico

| Componente     | Tecnologia                        | Versao |
|----------------|-----------------------------------|--------|
| Linguagem      | Java                              | 21     |
| Framework      | Spring Boot                       | 4.0    |
| ORM            | Hibernate / JPA                   | -      |
| Database (dev) | SQLite (via xerial JDBC)          | 3.45.2 |
| Database (prod)| PostgreSQL                        | -      |
| ML             | Smile NLP (Natural Language Proc) | 3.1.1  |
| Build          | Maven                             | -      |
| Lombok         | Project Lombok                    | -      |

---

## Estrutura do Projeto

```
backend/
+-- pom.xml
+-- mvnw / mvnw.cmd
+-- Dockerfile
+-- src/
    +-- main/
    |   +-- java/com/vfnews/factchecker/
    |   |   +-- FactcheckerApplication.java
    |   |   +-- config/
    |   |   |   +-- DataSeeder.java                (Runner de inicializacao)
    |   |   |   +-- DataSourceConfig.java          (Config de datasource)
    |   |   |   +-- DatasetSeederService.java      (Seeding via Google API)
    |   |   |   +-- FACTCKBRImporterService.java   (Import do FACTCK.BR TSV)
    |   |   +-- domain/
    |   |   |   +-- DatasetEntry.java              (Entidade JPA do dataset)
    |   |   |   +-- FactCheck.java                 (Entidade JPA de checagem)
    |   |   +-- repository/
    |   |   |   +-- DatasetEntryRepository.java
    |   |   |   +-- FactCheckRepository.java
    |   |   +-- service/
    |   |   |   +-- FactCheckService.java          (Orquestrador de checagem)
    |   |   |   +-- MLService.java                 (Modelo Naive Bayes)
    |   |   |   +-- google/
    |   |   |       +-- GoogleFactCheckService.java
    |   |   |       +-- GoogleFactCheckResponse.java
    |   |   +-- web/
    |   |       +-- CheckClaimController.java      (POST /api/check/)
    |   |       +-- DatasetController.java         (CRUD /api/dataset)
    |   |       +-- MLMetricsController.java       (GET /api/ml/metrics)
    |   |       +-- dto/
    |   |           +-- CheckClaimRequest.java
    |   |           +-- DatasetEntryRequest.java
    |   |           +-- FactCheckResponse.java
    |   +-- resources/
    |       +-- application.yml
    |       +-- datasets/
    |           +-- FACTCKBR.tsv
    +-- test/
```

---

## Pre-requisitos

- JDK 21 ou superior
- Maven 3.9+ (ou utilizar o wrapper `./mvnw` incluso)

---

## Executando

### Desenvolvimento

```bash
cd backend
./mvnw spring-boot:run
```

O servidor iniciara em `http://localhost:8080`. Na primeira execucao:

1. O banco SQLite sera criado em `backend/../factchecker.sqlite3`.
2. O `DataSeeder` executara o seeding do dataset (FACTCK.BR + Google API +
   fallback).
3. O modelo ML sera treinado automaticamente.

### Package (JAR)

```bash
./mvnw package -DskipTests
java -jar target/factchecker-0.0.1-SNAPSHOT.jar
```

---

## Configuracao

Toda a configuracao esta em `src/main/resources/application.yml`. As
propriedades podem ser sobrescritas por variaveis de ambiente.

### application.yml

```yaml
server:
  port: ${PORT:8080}

spring:
  application:
    name: factchecker
  jpa:
    database-platform: ${DATABASE_PLATFORM:org.hibernate.community.dialect.SQLiteDialect}
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

google:
  factcheck:
    api:
      key: ${GOOGLE_FACTCHECK_API_KEY:}
    trusted-publishers:
      - "Aos Fatos"
      - "Lupa"
      - "Estadao Verifica"
      - ...
```

### Variaveis de Ambiente

| Variavel                   | Padrao                                           | Descricao                             |
|----------------------------|--------------------------------------------------|---------------------------------------|
| `PORT`                     | `8080`                                           | Porta do servidor HTTP                |
| `GOOGLE_FACTCHECK_API_KEY` | (vazio)                                          | Chave da API Google Fact Check Tools  |
| `DATABASE_PLATFORM`        | `org.hibernate.community.dialect.SQLiteDialect`  | Dialeto Hibernate do banco            |
| `DATABASE_URL`             | `jdbc:sqlite:factchecker.sqlite3`                | URL de conexao JDBC                   |
| `DATABASE_USERNAME`        | (vazio)                                          | Usuario do banco (apenas PostgreSQL)  |
| `DATABASE_PASSWORD`        | (vazio)                                          | Senha do banco (apenas PostgreSQL)    |

---

## Endpoints da API

### Checagem de Alegacao

**`POST /api/check/`** -- Endpoint principal. Recebe uma alegacao em texto e
retorna a analise de veracidade.

**Request body:**

```json
{
  "claim": "texto da alegacao a ser verificada"
}
```

**Response (200 OK):**

```json
{
  "id": 1,
  "claim": "texto original",
  "result": "descricao textual do resultado",
  "source": "API",
  "rating": "falso",
  "publisher": "Aos Fatos",
  "url": "https://...",
  "matchedClaim": "alegacao correspondente encontrada na API",
  "createdAt": "2026-05-28T12:00:00"
}
```

O campo `source` pode ser `"API"` (Google Fact Check API) ou `"ML"` (modelo
local). Quando o modelo local nao consegue classificar a alegacao, o rating
retornado e `"Inconclusivo"`.

### Dataset

**`GET /api/dataset?page=0&size=50`** -- Lista paginada de entradas do dataset
de treinamento.

**`GET /api/dataset/stats`** -- Estatisticas do dataset:

```json
{
  "total": 1309,
  "byLabel": {
    "false": 850,
    "true": 120,
    "mixed": 339
  },
  "byKeyword": {
    "bolsonaro": 423,
    "lula": 311,
    "eleicao": 245,
    ...
  },
  "byPublisher": {
    "Aos Fatos": 702,
    "Lupa": 420,
    "Agencia Publica (Truco)": 187
  },
  "latest": [ ... ]
}
```

**`POST /api/dataset`** -- Insere manualmente uma nova entrada no dataset.

**`DELETE /api/dataset/{id}`** -- Remove uma entrada do dataset.

**`POST /api/dataset/train`** -- Dispara o retreinamento do modelo ML com os
dados atuais do dataset.

### Metricas do Modelo

**`GET /api/ml/metrics`** -- Retorna as metricas do ultimo treinamento:

```json
{
  "datasetSize": 1309,
  "trainSize": 1047,
  "testSize": 262,
  "vocabularySize": 4712,
  "algorithm": "Multinomial Naive Bayes",
  "accuracy": 0.8923,
  "precision": {
    "false": 0.91,
    "true": 0.82,
    "mixed": 0.78,
    "macro": 0.84
  },
  "recall": { ... },
  "f1": { ... }
}
```

---

## Fluxo de Checagem

O `FactCheckService` implementa uma cadeia de fallback progressiva:

```
Recebe alegacao do usuario
         |
         v
[1] Google Fact Check API existe?
    |-- SIM --> Retorna resultado da API
    |-- NAO --> continua
         |
         v
[2] Modelo ML (Naive Bayes) consegue classificar?
    |-- SIM --> Retorna classificacao do modelo
    |-- NAO --> continua
         |
         v
[3] Fallback amigavel: "Nao encontrei informacoes suficientes"
```

Antes de retornar o resultado da API, o servico verifica se a alegacao
encontrada e relevante para a consulta do usuario. A verificacao de relevancia
utiliza sobreposicao de tokens (termos comuns) entre a consulta e o resultado,
garantindo que pelo menos 50% dos termos relevantes da consulta aparecam na
alegacao encontrada.

---

## Seeding do Dataset

Quando o banco esta vazio, o `DataSeeder` executa tres etapas em ordem:

### Etapa 1: FACTCK.BR (FACTCKBRImporterService)

Importa o arquivo TSV localizado em `src/main/resources/datasets/FACTCKBR.tsv`.
O arquivo contem 1309 linhas com afirmacoes verificadas. A importacao:

- Le o arquivo linha a linha.
- Para cada linha, extrai `claimReviewed` (texto da alegacao), `alternativeName`
  (rotulo), `Author` (URL da agencia) e demais campos.
- Mapeia o rotulo para "true", "false" ou "mixed".
- Extrai palavras-chave do texto.
- Insere em lotes de 100 entradas, evitando duplicatas verificando o campo
  `text`.

### Etapa 2: Google Fact Check API (DatasetSeederService)

Consulta a API externa para cada palavra-chave predefinida (eleicao, bolsonaro,
lula, pt, campanha, urna, voto, fraude).

### Etapa 3: Fallback (DataSeeder.buildFallbackDataset)

Se o total de entradas for inferior a 100, insere um conjunto fixo de
afirmacoes verdadeiras e falsas sobre politica brasileira para garantir
equilibrio entre as classes.

---

## Modelo de Machine Learning

O `MLService` utiliza a biblioteca **Smile NLP** para implementar um
classificador **Naive Bayes Multinomial**.

### Pipeline de Treinamento

1. **Divisao treino-teste**: 80% treino, 20% teste (embaralhado com semente 42).
2. **Tokenizacao**: utiliza `SimpleTokenizer` do Smile NLP.
3. **Limpeza**: conversao para minusculas e remocao de caracteres nao
   alfabeticos.
4. **Construcao do vocabulario**: conjunto de tokens unicos do corpus de
   treinamento.
5. **Vetorizacao**: Bag of Words (frequencia de termos).
6. **Treinamento**: algoritmo Multinomial Naive Bayes do Smile.
7. **Persistencia**: modelo e vocabulario serializados para arquivos
   (`model.ser`, `vocab.ser`).

### Predicao

1. Tokeniza e limpa o texto de entrada.
2. Vetoriza utilizando o vocabulario existente.
3. Executa `model.predict(features)`.
4. Retorna o rotulo previsto ou "Inconclusivo" se o modelo nao estiver
   disponivel.

### Metricas

Apos o treinamento, o modelo e avaliado no conjunto de teste:

- Acurracia geral.
- Precisao, recall e F1-score por classe e macro.
- Matriz de confusao (para depuracao interna).

---

## Banco de Dados

### Entidades JPA

**DatasetEntry** -- Armazena as afirmacoes usadas para treinar o modelo ML.

| Campo     | Tipo         | Descricao                                   |
|-----------|--------------|---------------------------------------------|
| id        | Long (PK)    | Identificador auto-incremento               |
| text      | TEXT         | Texto da alegacao                           |
| label     | String       | Classificacao: "true", "false", "mixed"     |
| keywords  | String       | Palavras-chave separadas por virgula        |
| publisher | String       | Nome da fonte da verificacao                |
| createdAt | LocalDateTime| Timestamp de criacao                        |

**FactCheck** -- Armazena o historico de checagens realizadas pelos usuarios.

| Campo     | Tipo         | Descricao                                   |
|-----------|--------------|---------------------------------------------|
| id        | Long (PK)    | Identificador auto-incremento               |
| claim     | TEXT         | Alegacao enviada pelo usuario               |
| result    | String       | Texto descritivo do resultado               |
| source    | Enum (API/ML)| Origem da checagem                          |
| rating    | String       | Classificacao textual                       |
| publisher | String       | Publicador da checagem (quando via API)     |
| url       | String       | URL do artigo original (quando via API)     |
| createdAt | LocalDateTime| Timestamp de criacao                        |

### Dialetos

- **SQLite**: `org.hibernate.community.dialect.SQLiteDialect`
- **PostgreSQL**: `org.hibernate.dialect.PostgreSQLDialect`

A propriedade `spring.jpa.hibernate.ddl-auto` esta configurada como `update`, o
que cria e atualiza as tabelas automaticamente com base nas entidades.

---

## Integracao com FACTCK.BR

O dataset FACTCK.BR esta incluido no classpath como um arquivo TSV em
`src/main/resources/datasets/FACTCKBR.tsv`.

### Servico de Importacao

`FACTCKBRImporterService` e um bean Spring anotado com `@Service` que:

1. Abre o arquivo do classpath.
2. Ignora a linha de cabecalho.
3. Para cada linha subsequente, parseia as colunas separadas por tabulacao.
4. Mapeia os campos conforme a tabela abaixo.
5. Verifica duplicatas em `DatasetEntryRepository.existsByText()`.
6. Insere em lotes de 100.

### Mapeamento de Rotulos

Os rotulos textuais do dataset (coluna `alternativeName`) sao simplificados para
o esquema de tres classes esperado pelo modelo:

| Rotulo FACTCK.BR                       | Mapeamento |
|----------------------------------------|------------|
| `falso`, `false`                       | `false`    |
| `verdadeiro`, `true`                   | `true`     |
| `distorcido`, `exagerado`,             | `mixed`    |
| `insustentavel`, `impreciso`,          |            |
| `subestimado`, `discutivel`,           |            |
| `impossivel provar`, `sem contexto`    |            |

---

## Empacotando o Frontend

Em producao, o frontend compilado e copiado para `src/main/resources/static/`,
fazendo com que o Spring Boot sirva tanto a API quanto a SPA na mesma porta.

```bash
cd frontend/vfnews
npm run build
cp -r dist/* ../../backend/src/main/resources/static/
cd ../../backend
./mvnw package -DskipTests
```

---

## Testes

O projeto inclui o starter `spring-boot-starter-test` como dependencia de
teste. Para executar:

```bash
./mvnw test
```

---

## Referencias

- Smile NLP: https://haifengl.github.io/smile/
- Google Fact Check Tools API: https://developers.google.com/fact-check/tools/api
- FACTCK.BR Dataset: https://github.com/jghm-f/FACTCK.BR
