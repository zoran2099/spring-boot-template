---
name: commit-message-generation
description: "Use when: generating a conventional commit message from the current staged changes, summarizing the main change clearly and matching the repository's commit conventions."
---

# Geração de mensagem de commit padronizada

## Objetivo

Produzir uma mensagem de commit em Conventional Commits em português, priorizando as alterações já adicionadas ao stage, mas sem depender obrigatoriamente delas quando o diff atual do workspace fornece contexto suficiente para inferir a intenção da mudança.

## Fluxo de trabalho

1. Revisar a base de contexto
   - Priorize `git diff --cached --stat` e `git diff --cached` para identificar arquivos alterados, removidos, renomeados e o impacto real da mudança.
   - Se o stage estiver vazio ou incompleto, use `git diff --stat` e `git diff` para avaliar o diff atual do workspace.
   - A prioridade é o stage, mas ele não é obrigatório: quando o diff atual for claro, a mensagem pode ser gerada a partir dele sem depender do stage.
   - Ignore alterações não relacionadas ao objetivo principal da mudança e não invente detalhes que não apareçam no contexto observado.

2. Classificar a mudança principal
   - `feat`: nova funcionalidade ou capacidade adicionada.
   - `fix`: correção de bug ou ajuste de comportamento.
   - `refactor`: reestruturação sem alteração funcional visível.
   - `docs`: documentação.
   - `test`: testes.
   - `chore`: tarefas de manutenção, configuração ou infraestrutura.
   - `perf`: melhoria de performance.
   - `build` ou `ci`: mudanças de build, pipeline, dependências ou integração contínua.
   - `revert`: reverter uma alteração anterior.

3. Definir escopo quando fizer sentido
   - Prefira usar um escopo curto e específico, por exemplo `auth`, `api`, `product`, `db`, `config`.
   - Se a mudança afetar múltiplos domínios ou não houver escopo claro, omita o escopo em vez de inventar um genérico.
   - Evite escopos muito longos ou vagos.

4. Escrever a linha de assunto
   - Formato obrigatório: `tipo(escopo): descrição curta` ou `tipo: descrição curta` quando não houver escopo claro.
   - Escreva a mensagem inteira em português.
   - Use verbo no imperativo, curto e direto.
   - Sem ponto final.
   - A descrição deve indicar a mudança principal e ser compreensível em uma leitura rápida.
   - Exemplo: `feat(auth): validar token bearer` ou `fix(api): tratar resposta vazia do catálogo`.
   - Quando usar ícones, prefira o prefixo visual no início da linha, mantendo o padrão convencional: `✨ feat(auth): validar token bearer`.
   - Recomendação prática e segura para terminal, VS Code e GitHub: `✨ feat`, `🐛 fix`, `♻️ refactor`, `📝 docs`, `🧪 test`, `🔧 chore`, `⚡ perf`, `🏗️ build`, `↩️ revert`.
   - Use somente um símbolo por mensagem, sem misturar estilos, e mantenha a mensagem legível em terminal sem quebrar o formato do commit.

5. Incluir contexto técnico quando necessário
   - Se a mudança for complexa, adicionar corpo opcional com 1 a 3 parágrafos curtos explicando o motivo, o impacto ou a abordagem.
   - O corpo deve complementar a mensagem, não repetir a linha de assunto.
   - Escreva o corpo também em português.

6. Referenciar a tarefa ou issue
   - Quando houver referência, finalizar a mensagem com `#12345`.
   - Se a tarefa não tiver número, omitir a referência.
   - A referência deve aparecer no final da mensagem, normalmente após o corpo ou na última linha da mensagem.

7. Validar qualidade antes de concluir
   - Confirmar que a mensagem representa a intenção principal da mudança observada, priorizando o stage quando existir.
   - Se o stage estiver vazio, validar o diff atual do workspace e inferir a intenção com base no que foi modificado.
   - Confirmar que o tipo é coerente com a mudança real.
   - Confirmar que a descrição está no imperativo, curta e objetiva.
   - Confirmar que a mensagem está em português.
   - Confirmar ausência de ponto final e alinhamento ao Conventional Commits.
   - Se houver ícone, ele deve ser o mesmo padrão da categoria da mudança e não deve substituir o tipo do commit.

## Critérios de conclusão

A mensagem está pronta quando:

- Prioriza o stage quando ele estiver presente, mas aceita o diff atual como fallback válido;
- Usa padrão `tipo(escopo): descrição` ou `tipo: descrição` quando não houver escopo claro;
- A descrição é curta, clara e no imperativo;
- O texto está em português;
- O corpo, quando existir, traz contexto técnico útil e também está em português;
- A referência de tarefa, quando disponível, termina a mensagem com `#12345`;
- Se houver ícone, ele é consistente com a categoria da mudança e não substitui o tipo do commit.

## Exemplos

- `✨ feat(auth): validar token bearer #12345`
- `🐛 fix(produto): tratar preço nulo na resposta do catálogo`
- `♻️ refactor(repositorio): simplificar busca de produtos`
- `📝 docs(api): atualizar exemplos de endpoints`
- `🧪 test(auth): cobrir erros de validação do token`

## Regra importante

Use o stage como fonte preferencial, mas não o trate como requisito absoluto. Se o stage estiver vazio, use o diff atual do workspace como fallback. Se o contexto ainda for insuficiente, peça ao usuário mais detalhes antes de gerar a mensagem. Nunca invente uma mudança que não esteja presente no contexto observado.
