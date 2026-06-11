# Guia de Contribuicao

Obrigado pelo interesse em contribuir com o VFNews. Este documento contem as
diretrizes para contribuir com o projeto.

## Codigo de Conduta

Este projeto e seus participantes sao regidos pelo [Codigo de Conduta do
Contribuidor](CODE_OF_CONDUCT.md). Ao participar, voce espera-se que mantenha
este codigo.

## Como Contribuir

### Reportando Problemas

Antes de abrir uma issue, verifique se o problema ja nao foi reportado. Ao
abrir uma issue, inclua:

- Um titulo claro e descritivo.
- Uma descricao detalhada do problema.
- Passos para reproduzir (se aplicavel).
- Comportamento esperado vs. comportamento observado.
- Ambiente (sistema operacional, versao do Java, Node.js, etc.).
- Logs ou capturas de tela, se relevante.

### Sugerindo Melhorias

Sugestoes de melhoria sao bem-vindas. Ao abrir uma issue de melhoria:

- Explique o problema que a melhoria resolve.
- Descreva a solucao proposta detalhadamente.
- Mencione alternativas consideradas, se houver.

### Enviando Pull Requests

1. **Fork** o repositorio e crie um branch a partir da `main`.
2. Se aplicavel, adicione testes que cubram a nova funcionalidade ou correcao.
3. Garanta que o codigo compila sem erros:
   ```bash
   cd backend && ./mvnw compile
   cd frontend/vfnews && npm run build
   ```
4. Garanta que os testes existentes continuam passando:
   ```bash
   cd backend && ./mvnw test
   ```
5. Atualize a documentacao se necessario (READMEs, Javadoc, comentarios).
6. Faca o commit com uma mensagem clara e concisa (veja o padrao abaixo).
7. Abra o Pull Request para o branch `main`.

### Padrao de Mensagens de Commit

Utilizamos commits semanticos para facilitar a geracao de changelogs e a
navegacao pelo historico:

- `feat:` para novas funcionalidades.
- `fix:` para correcoes de bugs.
- `docs:` para alteracoes na documentacao.
- `refactor:` para refatoracoes que nao alteram comportamento.
- `test:` para adicao ou correcao de testes.
- `chore:` para tarefas de manutencao (build, dependencias, etc.).
- `style:` para alteracoes de formatacao (espacos, ponto-e-virgula, etc.).

Exemplos:

```
feat: importa dataset VFNews no startup do servidor
fix: corrige NPE quando publisher e nulo no retorno da API
docs: atualiza README com variaveis de ambiente
```

## Ambiente de Desenvolvimento

Consulte o [README.md](README.md) para instrucoes de configuracao do ambiente
de desenvolvimento.

### Backend

- JDK 21.
- Maven wrapper (`./mvnw`) incluso.
- Configure `GOOGLE_FACTCHECK_API_KEY` se desejar testar a integracao com a
  API externa.

### Frontend

- Node.js 22.
- npm para gerenciamento de dependencias.
- Execute `npm run dev` para o servidor de desenvolvimento.

## Estrutura de Branches

- `main` -- branch principal e de release.
- `feat/nome-da-feature` -- para novas funcionalidades.
- `fix/nome-do-bug` -- para correcoes.
- `docs/nome-da-doc` -- para documentacao.

## Licenca

Ao contribuir, voce concorda que suas contribuicoes serao licenciadas sob a
mesma licenca do projeto (GNU AGPL v3). Consulte o arquivo [LICENSE](LICENSE)
para mais detalhes.
