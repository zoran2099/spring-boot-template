# ADR 0001 — Scaffold via Spring Initializr (Boot 4.1.0, Java 21, Maven)

- **Status:** Accepted
- **Data:** 2026-08-12
- **Decisor:** Sessão de bootstrap do template (usuário + Copilot CLI)

## Contexto

O diretório `spring-boot-template/` estava vazio. Era necessário definir a base
tecnológica que serviria como ponto de partida para todos os microsserviços
Spring Boot derivados deste template.

Requisitos implícitos, extraídos da confirmação "defaults sensatos" do usuário:

- Stack Spring Boot moderna e atualmente suportada.
- Persistência relacional com Postgres.
- Suporte a testes de integração com container real (não H2 em memória).
- Boilerplate mínimo (ex.: Lombok para reduzir código repetitivo).
- Ferramenta de build padrão do ecossistema Java corporativo.

## Decisão

Gerar o scaffold via **Spring Initializr** (`https://start.spring.io/starter.tgz`)
com os seguintes parâmetros:

| Parâmetro | Valor |
|---|---|
| `type` | `maven-project` |
| `language` | `java` |
| `javaVersion` | `21` |
| `bootVersion` | `4.1.0.RELEASE` (⚠ patch: ver ADR-0007 — o `pom.xml` final usa `4.1.0`) |
| `groupId` | `com.example` |
| `artifactId` | `spring-boot-template` |
| `packageName` | `com.example.template` |
| `dependencies` | `web`, `data-jpa`, `validation`, `actuator`, `lombok`, `postgresql`, `testcontainers`, `devtools` |

Uso obrigatório do **Maven Wrapper** (`./mvnw`) — o `mvn` do sistema **não** é
suporte oficial deste template. Isso garante reprodutibilidade da versão do Maven.

## Alternativas consideradas

1. **Gradle Kotlin DSL** — mais moderno, mas Maven é ainda o padrão dominante no
   ambiente corporativo alvo. Descartado para reduzir atrito de adoção.
2. **Spring Boot 3.3.x** — a tentativa inicial foi 3.3.4, rejeitada pelo Initializr
   com HTTP 400: `"Spring Boot compatibility range is >=4.0.0"`. Aceitamos o
   padrão atual do serviço (4.1.0.RELEASE).
3. **Java 17** (padrão do Initializr) — sobrescrevemos para 21 (LTS mais recente,
   com virtual threads e pattern matching estáveis).
4. **Dependências mínimas (só web + actuator)** — descartado; o template precisa
   demonstrar o padrão completo (JPA + Postgres + Testcontainers + Validation).

## Consequências

### Positivas

- Boot 4.1.0 já vem com o namespace `jakarta.*` puro (Java EE legacy `javax.*`
  não é mais tolerado). Um projeto derivado nunca terá de fazer migração
  javax→jakarta.
- Java 21 habilita virtual threads no Tomcat/servlet stack sem workarounds.
- `devtools` acelera o loop de desenvolvimento local (hot reload).
- Actuator já preparado para observabilidade em produção.

### Negativas / atenção

- Spring Boot 4.x introduziu **renomes de starters** que quebram a intuição de
  quem vem da 3.x — ver ADR-0005 e a seção "Spring Boot 4.x starter naming" em
  `.github/copilot-instructions.md`:
  - `spring-boot-starter-web` → `spring-boot-starter-webmvc`
  - `spring-boot-starter-test` foi **dividido** em starters por concern:
    `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`,
    `spring-boot-starter-validation-test`, `spring-boot-starter-actuator-test`.
  Ao adicionar cobertura de teste para um novo concern, o starter correspondente
  precisa ser adicionado explicitamente no `pom.xml`.
- **⚠ A versão `4.1.0.RELEASE` retornada pelo Initializr NÃO existe em Maven
  Central** — o artefato publicado é `4.1.0` (sem sufixo). O `pom.xml` gerado
  precisa de patch. Ver **ADR-0007** para o detalhe e o comando de fix.
- Boot 4.1 é recente — documentação de terceiros (Stack Overflow, blogs) ainda
  é escassa; sempre preferir `HELP.md` (links oficiais 4.1.0) e o plugin
  `context7` (ver ADR-0006).
- O `pom.xml` gerado pelo Initializr contém overrides vazios de `<license>`,
  `<developers>`, `<scm>`, `<url>` para bloquear herança do POM pai. Se algum
  projeto derivado quiser efetivamente herdar, precisa remover esses blocos —
  ver `HELP.md` seção "Maven Parent overrides".

## Estrutura resultante

```
spring-boot-template/
├── .mvn/wrapper/            (Maven wrapper)
├── mvnw, mvnw.cmd
├── pom.xml
├── HELP.md
├── .gitignore, .gitattributes
├── src/
│   ├── main/
│   │   ├── java/com/example/template/SpringBootTemplateApplication.java
│   │   └── resources/application.properties
│   └── test/
│       └── java/com/example/template/
│           ├── SpringBootTemplateApplicationTests.java
│           ├── TestSpringBootTemplateApplication.java
│           └── TestcontainersConfiguration.java
```

## Como reproduzir

```bash
curl -sSL "https://start.spring.io/starter.tgz?type=maven-project&language=java&javaVersion=21&bootVersion=4.1.0.RELEASE&groupId=com.example&artifactId=spring-boot-template&name=spring-boot-template&packageName=com.example.template&dependencies=web,data-jpa,validation,actuator,lombok,postgresql,testcontainers,devtools" -o starter.tgz
tar -xzf starter.tgz && rm starter.tgz
```

Verificar versão atual do Initializr antes:
```bash
curl -sS "https://start.spring.io/metadata/client" | jq '.bootVersion.default, .javaVersion.default'
```
