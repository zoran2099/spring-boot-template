# ADR 0006 — Baseline de plugins do marketplace `awesome-copilot`

- **Status:** Accepted
- **Data:** 2026-08-12

## Contexto

O Copilot CLI (e outras integrações) permite instalar **plugins** vindos de
marketplaces registrados. O marketplace `awesome-copilot` (`github/awesome-copilot`)
já vem registrado por padrão no Copilot CLI recente. Cada plugin agrega
skills/agents/instructions que o assistente carrega automaticamente conforme
o contexto do trabalho.

Sem um baseline definido:

- Cada dev instala plugins diferentes → sessões geram sugestões
  inconsistentes entre máquinas.
- Skills relevantes (Spring Boot 4 testing, JUnit 6, Testcontainers) ficam
  disponíveis só para quem "sabia" instalar.
- Convém padronizar o mesmo conjunto no CI/agent cloud.

## Decisão

O template estabelece o seguinte **baseline obrigatório** de plugins do
marketplace `awesome-copilot`, também listado em `.github/copilot-instructions.md`:

| Plugin | Escopo relevante | Skills adicionadas |
|---|---|---|
| `java-development` | Spring Boot, JUnit, Javadoc, Java 21 idioms | 4 |
| `testing-automation` | JUnit 5/6, Testcontainers, integração, E2E | 5 |
| `security-best-practices` | Revisão de segurança em código Java/JPA/web | 1 |
| `modernize-java` | Planos de upgrade de Java / Boot | (agent CLI) |
| `openapi-to-application-java-spring-boot` | Scaffold de controllers/services a partir de OpenAPI | 1 |

Instalação (reproduzível, uma vez por máquina):

```bash
copilot plugin install java-development@awesome-copilot
copilot plugin install testing-automation@awesome-copilot
copilot plugin install security-best-practices@awesome-copilot
copilot plugin install modernize-java@awesome-copilot
copilot plugin install openapi-to-application-java-spring-boot@awesome-copilot
```

Verificação: `copilot plugin list` deve mostrar os cinco plugins acima.

## Por que estes cinco

Cada escolha é justificada pelo que já existe *neste* template
(não instalamos preventivamente):

- **`java-development`** — cobre o dia-a-dia de código Spring/JPA/Actuator
  presente no `pom.xml`.
- **`testing-automation`** — bate diretamente com o padrão dev-services de
  Testcontainers (ADR-0002) e o starter split `*-test` de Boot 4 (ADR-0001).
- **`security-best-practices`** — como o template usa JPA e web, riscos de
  injection e handling de segredos aparecem naturalmente.
- **`modernize-java`** — este template inicia em Java 21 / Boot 4.1; a
  próxima transição (Java 25 quando LTS estável) precisará de plano
  estruturado. Ter o plugin já instalado evita improviso.
- **`openapi-to-application-java-spring-boot`** — projetos derivados que
  expõem APIs REST normalmente têm um contrato OpenAPI upstream (BB Digital
  ou equivalente). O plugin gera controllers/services no layout de pacote
  correto (`com.example.template.*`).

## Alternativas consideradas / rejeitadas

- **`spring-boot-testing` (skill individual)** — inicialmente pesquisado no
  índice `llms.txt` do `awesome-copilot`. Descoberto que **os plugins
  instaláveis são bundles**, não skills individuais. O bundle
  `java-development` já contém a skill equivalente.
- **`java-mcp-development`** — só relevante se este template for para
  construir servidores MCP, o que não é o caso.
- **`azure-*`, `aws-*`** — não vinculamos nuvem específica no template.
- **`java-modernization-studio`** — plugin de "canvas interativo" mais pesado;
  `modernize-java` (CLI) atende ao caso de uso corporativo comum.
- **`context7`** — poderia ser útil para docs Boot 4.1 sempre atualizadas,
  mas exige key de API (context7.com). Deixamos como sugestão opcional em
  `docs/reproducing.md`, não no baseline.

## Consequências

### Positivas

- Todo dev/CI que rodar `copilot plugin install <...>` obtém o mesmo
  conjunto de skills.
- Sessões geram código no padrão do template (Spring Boot 4 starters
  corretos, testes com JUnit 6 + AssertJ + Testcontainers).
- Extensão futura é aditiva: novo plugin → novo item na tabela + comando
  documentado.

### Negativas / atenção

- Plugins do marketplace são **third-party**; auditar antes de fazer bump
  de versão (o próprio README do `awesome-copilot` avisa isso).
- Se o CI usa Copilot CLI, precisa executar `copilot plugin install`
  como parte do setup — automatizar em `.github/workflows/` do projeto
  derivado.
- Alguns plugins podem ser deprecados/renomeados. Verificar semestralmente
  com `copilot plugin marketplace browse awesome-copilot`.

## Manutenção do baseline

- Adicionar plugin: atualizar tabela acima **e** a seção do
  `.github/copilot-instructions.md` no mesmo PR.
- Remover plugin: registrar no ADR sucessor explicando por que a skill
  deixou de ser relevante para este template.
- Nunca instalar plugins fora do baseline em ambiente compartilhado sem
  registrar aqui — evita "shadow tooling".
