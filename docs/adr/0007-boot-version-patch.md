# ADR 0007 — Patch da versão do Spring Boot: `4.1.0.RELEASE` → `4.1.0`

- **Status:** Accepted
- **Data:** 2026-08-12

## Contexto

Durante a validação do build (`./mvnw compile`), o Maven falhou com:

```
Non-resolvable parent POM ...
org.springframework.boot:spring-boot-starter-parent:pom:4.1.0.RELEASE was not
found in https://repo.maven.apache.org/maven2 ...
```

Investigação:

- `curl https://start.spring.io/metadata/client` retorna o default de Boot
  como `4.1.0.RELEASE`.
- O download em `https://start.spring.io/starter.tgz?...&bootVersion=4.1.0.RELEASE`
  responde **HTTP 200** e gera um `pom.xml` referenciando
  `<version>4.1.0.RELEASE</version>`.
- Consulta ao Maven Central:
  - `https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/4.1.0.RELEASE/spring-boot-starter-parent-4.1.0.RELEASE.pom` → **404**
  - `https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/4.1.0/spring-boot-starter-parent-4.1.0.pom` → **200**
- Também consultamos `search.maven.org`: `spring-boot-starter-parent` só
  publica versões sem sufixo `.RELEASE`.

Conclusão: o **metadata do Spring Initializr diverge do que efetivamente
está publicado em Maven Central**. O sufixo `.RELEASE` é herança da era
Boot 1.x/2.x (antes do esquema semver puro) e não é usado em Boot 3.x/4.x.

## Decisão

No `pom.xml` gerado, substituir a versão:

```xml
<!-- ANTES (gerado pelo Initializr) -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0.RELEASE</version>
    ...
</parent>

<!-- DEPOIS (o que está em Maven Central) -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
    ...
</parent>
```

Também aplicado o mesmo `sed` em `HELP.md` para que os links de documentação
apontem para `docs.spring.io/spring-boot/4.1.0/...` (páginas existentes)
em vez de `4.1.0.RELEASE/...` (páginas 404).

## Alternativas consideradas

1. **Voltar para Spring Boot 3.5.3** (última LTS publicada real) — descartado
   porque:
   - O Initializr atual **rejeita** `bootVersion=3.5.3` com HTTP 400:
     `"Spring Boot compatibility range is >=4.0.0"`.
   - Perderíamos a validade do ADR-0001 (Java 21 + Boot 4.x).
   - Boot 4.x já é a linha suportada pelo Initializr; não faz sentido
     regredir.

2. **Ignorar o erro e apontar para um repositório Spring interno** — descartado
   porque `repo.spring.io/milestone` e `/snapshot` também retornam 404 para
   `4.1.0.RELEASE`. A versão simplesmente não existe com esse nome.

3. **Abrir issue no Spring Initializr e esperar correção** — sim, deve ser
   feito paralelamente, mas não bloqueia o template. O patch local é trivial.

## Consequências

### Positivas

- Build funciona **imediatamente** após scaffold (compile, test, package).
- Links do `HELP.md` levam a páginas reais da documentação.
- Convenção alinhada com o esquema de versionamento moderno de Spring Boot
  (`3.5.3`, `4.0.0`, `4.1.0`, `4.1.1`, ...).

### Negativas / atenção

- **Ao regenerar o scaffold** (ex.: adicionar dependência via Initializr no
  futuro), o `sed` precisa ser reaplicado. Está documentado em
  `docs/reproducing.md`.
- **Ao bumpar Boot** (ex.: `4.1.0` → `4.1.1`), verificar antes se a nova
  versão está publicada em Maven Central:
  ```bash
  curl -o /dev/null -w "%{http_code}\n" \
    https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/4.1.1/spring-boot-starter-parent-4.1.1.pom
  ```
  Deve retornar `200`. Se `404`, aguardar a publicação — não fazer
  workaround criativo.
- Se o Initializr um dia corrigir o metadata (ou publicar de fato o
  artefato `.RELEASE`), este ADR pode ser fechado como `Superseded` sem
  ação retroativa.

## Reforço no `copilot-instructions.md`

A seção "Spring Boot 4.x starter naming" já foi atualizada para explicitar
o patch: "**note: the `pom.xml` uses `4.1.0`, not `4.1.0.RELEASE` — the
Spring Initializr generates the `.RELEASE` suffix but that exact artifact
is not published to Maven Central**". Isto previne sessões de IA de
"restaurar" o sufixo por engano.
