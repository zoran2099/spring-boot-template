# Documentação do `catalog-api`

Esta pasta preserva as decisões arquiteturais do template de origem e registra a
evolução do projeto derivado `catalog-api`. Os ADRs 0001–0009 são históricos; o
ADR 0010 representa a arquitetura atual da API.

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
| [0009](adr/0009-devcontainer.md) | Dev Container com Docker-in-Docker e CAs corporativas | Accepted |
| [0010](adr/0010-catalog-read-only-api.md) | API de catálogo somente leitura com Data REST | Accepted |

## Origem do projeto

1. Leia os ADRs 0001–0009 para entender as decisões herdadas do template.
2. Leia o ADR 0010 para entender quais decisões foram substituídas no projeto atual.
3. Popule `.devcontainer/certs/` seguindo `.devcontainer/README.md` (ADR 0009),
   pois certificados corporativos não são versionados.
4. Registre mudanças arquiteturais futuras em novos ADRs, sem reescrever o histórico.
