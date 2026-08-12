# ADR 0004 — `application.properties` intencionalmente minimalista

- **Status:** Accepted
- **Data:** 2026-08-12

## Contexto

O `application.properties` de um projeto Spring Boot é o ponto natural para
configurar datasource, logging, actuator, JPA, etc. Templates ruins costumam
"pré-encher" esse arquivo com valores plausíveis (`spring.datasource.url=jdbc:postgresql://localhost:5432/dev`,
credenciais dummy, `spring.jpa.hibernate.ddl-auto=update`), o que:

- Vaza pressupostos de ambiente para dentro do artefato.
- Convida a ter senhas versionadas.
- Colide com o padrão dev-services de Testcontainers (ADR-0002), que já injeta
  o datasource programaticamente.
- Confunde novos projetos derivados sobre o que é "template" e o que é
  "configuração real".

## Decisão

Manter o `src/main/resources/application.properties` reduzido ao mínimo
absoluto:

```properties
spring.application.name=spring-boot-template
```

**Nada mais.** Especificamente:

- ❌ Sem `spring.datasource.*` — é injetado por `@ServiceConnection` no dev/test
  (ADR-0002) e vem do ambiente externo em produção.
- ❌ Sem `spring.jpa.*` — os defaults do Spring Boot 4 são bons; sobrescrever
  é decisão de projeto derivado.
- ❌ Sem `management.endpoints.*` — os defaults conservadores do Actuator
  (apenas `/health` exposto) são o comportamento seguro.
- ❌ Sem `logging.*` — herda os padrões do Boot.
- ❌ Sem `server.port` — permite override por env var em runtime.
- ❌ Sem `spring.profiles.active` — profile é responsabilidade de quem deploya.

## Alternativas consideradas

1. **Pré-configurar datasource dummy** para "compilar out of the box" —
   descartado porque:
   - Um `spring.datasource.url` inválido faria o `spring-boot:run` falhar de
     forma silenciosa/confusa.
   - `spring-boot:test-run` já fornece um datasource via Testcontainers.
   - Encoraja anti-padrão de senha versionada.

2. **`application.yaml`** em vez de `.properties` — decisão neutra. Ficamos
   com `.properties` porque é o formato gerado pelo Initializr e é o padrão
   nas convenções do time. Um projeto derivado pode migrar sem custo.

3. **Múltiplos `application-<profile>.properties` no template** (`-dev`, `-prod`,
   `-test`) — descartado. Cada projeto derivado tem realidade diferente sobre
   quantos profiles precisa. Fornecer stubs vazios sugere um contrato que o
   template não sustenta.

## Consequências

### Positivas

- Zero segredo/URL/host versionado — auditoria de conformidade trivial.
- `spring-boot:test-run` funciona sem configuração adicional (ADR-0002 cobre).
- Projeto derivado adiciona só o que efetivamente precisa, evitando arrastar
  configuração morta.

### Negativas / atenção

- `./mvnw spring-boot:run` (sem `test-`) **falha por design** se o operador
  não fornecer `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
  `SPRING_DATASOURCE_PASSWORD` via env var ou arquivo externo. Isso é
  documentado em `.github/copilot-instructions.md`.
- Quem vem de projetos que "só rodam" pode estranhar. A mitigação é o
  `HELP.md` do Initializr + `docs/reproducing.md`.

## Regra para projetos derivados

Ao configurar um novo projeto:

1. Adicionar propriedades **apenas** conforme necessidade real, não
   preventivamente.
2. Segredos/URLs de produção vão em variáveis de ambiente
   (`SPRING_DATASOURCE_URL` etc.) ou em Config Server / Vault, **nunca**
   em `application.properties` versionado.
3. Se precisar de defaults locais que não são segredo (ex.:
   `logging.level.com.exemplo=DEBUG`), use `application-dev.properties`
   e documente no README do projeto derivado.
