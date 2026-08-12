# Documentação do template `spring-boot-template`

Esta pasta registra **todas** as decisões arquiteturais e de configuração feitas
durante a criação deste template, para servir de base de revisão quando o repositório
for clonado ou forkado para novos projetos.

## Como ler este diretório

- **`adr/`** — Architecture Decision Records numerados sequencialmente. Cada ADR
  responde: qual foi a decisão, qual era o contexto, quais alternativas existiam,
  e quais são as consequências (positivas e negativas). Este é o material principal
  para revisão arquitetural.
- **`session-log.md`** — Log cronológico bruto e verificável da sessão de bootstrap
  do template (comandos executados, respostas de APIs externas, arquivos criados).
  Use para auditoria e para reproduzir o mesmo scaffold do zero.
- **`reproducing.md`** — Passo a passo condensado para recriar o template a partir
  de um diretório vazio, sem depender desta sessão.

## Índice de ADRs

| # | Título | Status |
|---|---|---|
| [0001](adr/0001-spring-initializr-scaffold.md) | Scaffold via Spring Initializr (Boot 4.1.0, Java 21, Maven) | Accepted |
| [0002](adr/0002-testcontainers-dev-services.md) | Testcontainers como dev-service (sem docker-compose) | Accepted |
| [0003](adr/0003-lombok-dual-annotation-processor.md) | Lombok com annotation processor em `default-compile` e `default-testCompile` | Accepted |
| [0004](adr/0004-minimal-application-properties.md) | `application.properties` intencionalmente minimalista | Accepted |
| [0005](adr/0005-copilot-instructions.md) | `.github/copilot-instructions.md` como fonte de verdade para sessões de IA | Accepted |
| [0006](adr/0006-awesome-copilot-plugins.md) | Baseline de plugins do `awesome-copilot` marketplace | Accepted |
| [0007](adr/0007-boot-version-patch.md) | Patch da versão do Spring Boot: `4.1.0.RELEASE` → `4.1.0` | Accepted |
| [0008](adr/0008-testcontainers-ryuk-disabled.md) | Desabilitar Testcontainers Ryuk no Surefire | Accepted |

## Quando forkar/clonar este template

1. Leia todos os ADRs em ordem.
2. Renomeie o `groupId`/`artifactId`/pacote base (`com.example.template`) para o do
   novo projeto — os ADRs 0001 e 0005 documentam os pontos que exigem atualização
   coordenada (raiz de component-scan, referências em testes, `.github/copilot-instructions.md`).
3. Rode `copilot plugin list` e instale o baseline do ADR 0006.
4. Se qualquer decisão for revertida no projeto derivado, adicione um novo ADR
   com status `Supersedes ADR-XXXX` em vez de editar o ADR original.
