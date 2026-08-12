# ADR 0005 — `.github/copilot-instructions.md` como fonte de verdade para sessões de IA

- **Status:** Accepted
- **Data:** 2026-08-12

## Contexto

Este template será usado como base para múltiplos projetos derivados.
Muito do trabalho será feito com apoio de assistentes de IA (GitHub Copilot no
VS Code, Copilot CLI, Copilot Cloud/Coding Agent). Sem contexto explícito,
essas ferramentas:

- Tendem a "corrigir" convenções propositais do Boot 4.x para o padrão 3.x
  (ex.: renomear `spring-boot-starter-webmvc` para `spring-boot-starter-web`).
- Sugerem `application-dev.properties` com senhas dummy porque é o que aparece
  no material de treinamento.
- Duplicam a configuração de Testcontainers em cada `@SpringBootTest`.
- Não sabem que Lombok está registrado em duas execuções do compiler plugin
  intencionalmente.

Cada uma dessas "correções bem-intencionadas" degrada o template.

## Decisão

Criar e manter `.github/copilot-instructions.md` como o **único** ponto de
entrada para qualquer sessão de IA. O arquivo deve:

1. Ser lido automaticamente pelo Copilot (VS Code, CLI, Coding Agent) — o
   caminho `.github/copilot-instructions.md` é a convenção oficial.
2. Documentar **apenas** o que é específico deste projeto: comandos,
   arquitetura macro, e convenções não-óbvias. Não repetir Spring genérico.
3. Explicitar as "armadilhas" onde a IA erra com frequência (starters Boot
   4.x, duplicação intencional do processor Lombok, minimalismo do
   `application.properties`, dev-services Testcontainers).
4. Listar plugins recomendados do marketplace `awesome-copilot` (ADR-0006)
   para que a sessão puxe skills adicionais automaticamente.

## Estrutura mandatória do arquivo

- **Build, test, run** — inclui como rodar um teste único (não só a suite),
  como subir a app com Testcontainers, e como buildar imagem OCI.
- **Architecture: Testcontainers as a dev-time service** — explica o padrão
  dos três arquivos (ADR-0002).
- **Spring Boot 4.x starter naming** — evita regressão para nomes 3.x.
- **Lombok** — alerta sobre duplicação em `default-compile` e `default-testCompile`.
- **Conventions** — pacote base, minimalismo do `application.properties`,
  escopo `runtime` do driver Postgres.
- **Reference** — aponta para `HELP.md` (auto-gerado com links da versão
  Boot correta).
- **Recommended Copilot CLI plugins** — comandos `copilot plugin install ...`.

## Alternativas consideradas

1. **`AGENTS.md` na raiz** (convenção OpenCode/Codex/Jules) — descartado como
   arquivo principal, mas pode ser adicionado como *link* que aponta para
   `.github/copilot-instructions.md` se um projeto derivado usar outras
   ferramentas de IA. Manter duas fontes de verdade é a receita para
   drift — sempre uma delas fica desatualizada.

2. **`CONTRIBUTING.md` genérico** — insuficiente porque não é auto-carregado
   pelo Copilot e mistura audiência humana/IA. Mantido como complementar,
   se o projeto derivado precisar.

3. **Comentários no `pom.xml` e nos arquivos-fonte** — parte da solução
   (ver ADRs 0002 e 0008), mas não substitui a visão macro. Comentários
   locais explicam "o que", não "por quê arquitetural".

## Consequências

### Positivas

- Sessões de IA convergem rapidamente para o padrão do template sem que o
  humano precise re-explicar as convenções.
- Novo membro do time também se beneficia: o arquivo é útil como onboarding.
- Alinhamento explícito com ferramentas oficiais (VS Code, Copilot CLI) que
  procuram exatamente esse caminho.

### Negativas / atenção

- É mais uma peça a manter em sincronia com o `pom.xml`, ADRs e código real.
  **Regra:** ao aceitar uma decisão que altera padrão do template, atualizar
  simultaneamente (a) ADR correspondente, (b) `copilot-instructions.md`,
  (c) `pom.xml`/código, e (d) o `session-log.md` se estiver em bootstrap.
- Se um projeto derivado *revoga* uma convenção (ex.: passa a usar
  `docker-compose` em vez de dev-services), **precisa** editar o
  `copilot-instructions.md` para não induzir sessões futuras a erro.
- O arquivo pode ficar longo. Manter foco: se algo é convenção genérica de
  Spring, remover.

## Manutenção

Alterações no `.github/copilot-instructions.md` devem ser feitas em PR,
com revisão específica pelo dono do template, tratando o arquivo como
código de infraestrutura.
