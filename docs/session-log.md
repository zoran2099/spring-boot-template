# Session Log — bootstrap do template

Registro cronológico bruto da sessão de criação deste template, para auditoria
e reprodução. Comandos, respostas e decisões estão em ordem de execução.

**Ambiente da sessão:**
- Host: Linux (openSUSE Leap 15.6), Rancher Desktop como Docker runtime.
- Copilot CLI: `1.0.79`
- Java: `openjdk 21.0.2 2024-01-16` (build `21.0.2+13-58`)
- Docker: `29.1.4-rd` (Rancher Desktop; daemon reporta `Server Version: 28.5.1-ce`)
- Diretório de trabalho: `/home/ext-zoran.miranda/projetos/bbts/arquitetura/spring-boot-template`

---

## 1. Estado inicial

- Diretório completamente vazio (apenas `.` e `..`).
- Nenhum arquivo de scaffold, configuração de IA ou documentação preexistente.
- Objetivo declarado pelo usuário: usar este diretório como **template Spring
  Boot** que será clonado para novos projetos.

## 2. Decisão de scaffold via Spring Initializr

**Parâmetros escolhidos** ("defaults sensatos", confirmados via `ask_user`):

- `type=maven-project`
- `language=java`
- `javaVersion=21`
- `bootVersion` inicialmente pretendido: `3.3.4`
- `groupId=com.example`, `artifactId=spring-boot-template`
- `packageName=com.example.template`
- `dependencies=web,data-jpa,validation,actuator,lombok,postgresql,testcontainers,devtools`

## 3. Primeira tentativa: Boot 3.3.4 rejeitada pelo Initializr

```
HTTP 400
{"timestamp":"2026-08-12T12:47:26.157Z","status":400,"error":"Bad Request",
 "message":"Invalid Spring Boot version '3.3.4', Spring Boot compatibility
 range is >=4.0.0","path":"/starter.tgz"}
```

Consulta ao metadata do Initializr:

```
curl https://start.spring.io/metadata/client
→ bootVersion default: 4.1.0.RELEASE
→ javaVersion default: 17
```

Opções disponíveis para `bootVersion`:
- `4.1.1.BUILD-SNAPSHOT`
- `4.1.0.RELEASE` ← default
- `4.0.8.BUILD-SNAPSHOT`
- `4.0.7.RELEASE`

**Ação:** aceitar `4.1.0.RELEASE`, manter Java 21 (override do default 17). Ver **ADR-0001**.

## 4. Segunda tentativa: download OK, scaffold extraído

```
curl -sSL "https://start.spring.io/starter.tgz?type=maven-project&language=java
 &javaVersion=21&bootVersion=4.1.0.RELEASE&groupId=com.example
 &artifactId=spring-boot-template&name=spring-boot-template
 &packageName=com.example.template&dependencies=web,data-jpa,validation,
 actuator,lombok,postgresql,testcontainers,devtools"
→ HTTP 200
→ starter.tgz: gzip compressed data
```

Extração produziu:

```
.gitattributes  .gitignore  HELP.md
.mvn/  mvnw  mvnw.cmd  pom.xml
src/main/java/com/example/template/SpringBootTemplateApplication.java
src/main/resources/application.properties
src/test/java/com/example/template/SpringBootTemplateApplicationTests.java
src/test/java/com/example/template/TestSpringBootTemplateApplication.java
src/test/java/com/example/template/TestcontainersConfiguration.java
```

## 5. Análise do scaffold

- `SpringBootTemplateApplication.java` — entry point sem config de DB.
- `TestcontainersConfiguration.java` — `@Bean PostgreSQLContainer` com `@ServiceConnection`.
- `TestSpringBootTemplateApplication.java` — `main()` de teste que compõe
  `SpringApplication.from(SpringBootTemplateApplication::main).with(TestcontainersConfiguration.class)`.
- `application.properties` — apenas `spring.application.name=spring-boot-template`.
- `pom.xml` — Boot `4.1.0.RELEASE`, Java 21, starters renomeados (`spring-boot-starter-webmvc`),
  test starters split, Lombok em `default-compile` e `default-testCompile`.

Padrões arquiteturais identificados foram formalizados em:
- **ADR-0001** (scaffold), **ADR-0002** (dev-services), **ADR-0003** (Lombok),
  **ADR-0004** (application.properties minimalista).

## 6. Criação de `.github/copilot-instructions.md`

Motivação, estrutura e responsabilidades: **ADR-0005**.

Arquivo criado em `.github/copilot-instructions.md`, contendo:
- Build/test/run commands.
- Padrão Testcontainers de três arquivos.
- Renomes de starters Boot 4.x.
- Regra de Lombok dual-execution.
- Convenções de pacote e `application.properties`.

## 7. Consulta ao usuário sobre `awesome-copilot`

Usuário perguntou pela relevância de `github/awesome-copilot`.

Verificação: `copilot plugin --help` mostra que o marketplace `awesome-copilot`
já vem registrado por padrão no CLI 1.0.79.

Listagem via `awesome-copilot.github.com/llms.txt` mostrou skills granulares
(`spring-boot-testing`, `java-springboot`, etc.) — mas tentativa de instalação
de `spring-boot-testing@awesome-copilot` falhou:

```
Failed to install plugin: Error: Plugin "spring-boot-testing" not found in
marketplace "awesome-copilot". Available plugins: ... (lista de bundles)
```

Descoberta: **plugins instaláveis são bundles**, não skills individuais.
Bundles relevantes ao stack identificados via
`copilot plugin marketplace browse awesome-copilot`: `java-development`,
`testing-automation`, `security-best-practices`, `modernize-java`,
`openapi-to-application-java-spring-boot`.

## 8. Instalação do baseline de plugins (ADR-0006)

```
copilot plugin install java-development@awesome-copilot
→ Installed 4 skills.

copilot plugin install testing-automation@awesome-copilot
→ Installed 5 skills.

copilot plugin install security-best-practices@awesome-copilot
→ Installed 1 skill.

copilot plugin install modernize-java@awesome-copilot
→ Plugin installed (v1.22.0)

copilot plugin install openapi-to-application-java-spring-boot@awesome-copilot
→ Installed 1 skill.
```

Verificação com `copilot plugin list`: 5 plugins ativos.

Seção "Recommended Copilot CLI plugins" adicionada ao
`.github/copilot-instructions.md` com comandos de reinstalação e propósito de cada.

## 9. Validação do build (correções aplicadas)

Ao rodar `./mvnw compile`, o build falhou:

```
Non-resolvable parent POM for com.example:spring-boot-template:0.0.1-SNAPSHOT:
The following artifacts could not be resolved:
 org.springframework.boot:spring-boot-starter-parent:pom:4.1.0.RELEASE (absent)
```

### 9.1 Investigação da versão

Probe direto em Maven Central:

| Versão | HTTP Central |
|---|---|
| `spring-boot-starter-parent:4.1.0.RELEASE` | 404 |
| `spring-boot-starter-parent:4.0.7.RELEASE` | 404 |
| `spring-boot-starter-parent:4.1.0` | **200** ✓ |
| `spring-boot-starter-parent:4.0.0` | **200** ✓ |

**Conclusão:** o Initializr publica metadata com sufixo `.RELEASE` que não
existe no Central. Registrado em **ADR-0007**.

### 9.2 Patch aplicado

```bash
sed -i 's|<version>4.1.0.RELEASE</version>|<version>4.1.0</version>|' pom.xml
sed -i 's|4.1.0.RELEASE|4.1.0|g' HELP.md
```

Rebuild:

```
./mvnw -B -ntp -q compile
→ (sem output = sucesso)
```

### 9.3 Testes falham com Ryuk

```
./mvnw -B -ntp test
→ Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
→ Caused by: java.lang.IllegalStateException: Could not connect to Ryuk
  at localhost:32768
```

Ambiente é **Rancher Desktop**, incompatível com Ryuk. Registrado em **ADR-0008**.

### 9.4 Tentativas de fix (documentadas para posteridade)

| Tentativa | Resultado |
|---|---|
| Criar `src/test/resources/testcontainers.properties` com `ryuk.disabled=true` | Ainda falhava — Surefire não lê a tempo |
| Exportar `TESTCONTAINERS_RYUK_DISABLED=true` no shell | ✓ Passou |
| `<systemPropertyVariables>` no surefire (só `testcontainers.ryuk.disabled=true`) | ✗ Não teve efeito |
| `<environmentVariables>` no surefire (`TESTCONTAINERS_RYUK_DISABLED=true`) | ✓ Passou |

**Solução final:** ambos `<systemPropertyVariables>` **e**
`<environmentVariables>` no surefire, para robustez futura. O arquivo
`testcontainers.properties` foi mantido como fallback documental.

### 9.5 Resultados finais

```
./mvnw -B -ntp test
→ Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
→ BUILD SUCCESS

./mvnw -B -ntp spring-boot:test-run
→ Container postgres:latest started in PT2.016725271S
→ Tomcat started on port 8080 (http)
→ Started SpringBootTemplateApplication in 6.39 seconds

curl http://localhost:8080/actuator/health
→ HTTP 200
→ {"groups":["liveness","readiness"],"status":"UP"}

./mvnw -B -ntp clean verify
→ BUILD SUCCESS
→ target/spring-boot-template-0.0.1-SNAPSHOT.jar
```

## 10. Documentação final (esta pasta)

Criada a estrutura `docs/` com:

- `README.md` — índice e overview.
- `adr/0001..0008-*.md` — 8 ADRs cobrindo cada decisão.
- `session-log.md` — este arquivo.
- `reproducing.md` — passo a passo para recriar do zero.

## 11. Validação em Dev Container (segunda rodada)

Usuário solicitou repetir os testes dentro de um **Dev Container** — a
validação anterior havia sido feita no host (openSUSE), o que expõe o
template a variações de daemon Docker (usuário mencionou que Rancher
Desktop estava desligado quando os primeiros testes rodaram, confirmando
que era Docker CE nativo — daemon `28.5.1-ce` / OS `openSUSE Leap 15.6`).

### 11.1 Instalação do CLI de devcontainer

```
npm install -g @devcontainers/cli
→ devcontainer 0.88.0
```

### 11.2 Primeira tentativa: `devcontainer up`

Erro:
```
Error fetching image details: self-signed certificate in certificate chain
```

Causa: o CLI Node.js não confia nas CAs corporativas ao falar com `ghcr.io`
(registry das features). Workaround: `NODE_EXTRA_CA_CERTS=/etc/ssl/ca-bundle.pem`.

### 11.3 Erro seguinte: certificado dentro do container

Com `NODE_EXTRA_CA_CERTS` set, o build da imagem prossegue mas a feature
`docker-in-docker` falha:

```
curl failed to verify the legitimacy of the server ...
ERROR: Feature "Docker (Docker-in-Docker)" failed to install!
```

O container também precisa das CAs corporativas.

### 11.4 Identificação da chain de MITM

```
docker run --rm mcr.microsoft.com/devcontainers/java:1-21-bookworm \
  openssl s_client -connect packages.microsoft.com:443 -showcerts
```

Chain observada:
- `CN=packages.microsoft.com` (server real)
- `CN=ca.bbts.goskope.com` (intermediária Netskope customizada para BBTS)
- `CN=certadmin, O=Netskope Inc.` (raiz Netskope)

**Conclusão:** o proxy corporativo é **Netskope**, não uma CA já listada em
`/etc/pki/trust/anchors/`. A intermediária e a raiz precisam ser extraídas
diretamente da chain.

### 11.5 Extração e uso das CAs Netskope

```bash
openssl s_client -connect packages.microsoft.com:443 -showcerts </dev/null | \
  awk '/BEGIN CERT/,/END CERT/' > /tmp/chain.pem
awk '/-----BEGIN CERTIFICATE-----/{n++} n>1{print > ("/tmp/cert_" n ".pem")}' /tmp/chain.pem
cp /tmp/cert_2.pem .devcontainer/certs/netskope-intermediate-bbts.pem
cp /tmp/cert_3.pem .devcontainer/certs/netskope-root-ca.pem
```

Além das duas CAs Netskope, também foram copiadas ~80 CAs BBTS de
`/etc/pki/trust/anchors/BBcerts_*.pem` (por segurança, caso algum endpoint
use chain BBTS direta).

### 11.6 Estrutura final do `.devcontainer/`

```
.devcontainer/
├── devcontainer.json   # spec principal
├── Dockerfile          # base + install CAs
├── certs/              # CAs (NÃO versionadas)
│   └── .gitignore      # bloqueia tudo
└── README.md           # instruções + script para popular certs
```

Detalhes técnicos:
- Base: `mcr.microsoft.com/devcontainers/java:1-21-bookworm` (Java 21.0.8
  Microsoft OpenJDK + Maven já bundled).
- Feature: `docker-in-docker:2` (isola daemon do host).
- Dockerfile roda `update-ca-certificates` **e** `keytool -importcert`
  para o `cacerts` do JDK.
- `certs/` versionado apenas como `.gitignore` que bloqueia todo o conteúdo.

Registrado em **ADR-0009**.

### 11.7 Validação final no devcontainer

```
NODE_EXTRA_CA_CERTS=/etc/ssl/ca-bundle.pem devcontainer up --workspace-folder .
→ Container started, remoteUser: vscode

devcontainer exec ... ./mvnw -B -ntp clean verify
→ [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
→ [INFO] BUILD SUCCESS
→ target/spring-boot-template-0.0.1-SNAPSHOT.jar

Postgres container observado nos logs de teste:
  jdbc:postgresql://172.18.0.1:32768/test
  Database version: 18.4
```

O DinD roda o Postgres **dentro** do próprio devcontainer, sem tocar no
daemon do host. Ryuk permanece desabilitado pelo `pom.xml` (ADR-0008)
mesmo com `containerEnv: TESTCONTAINERS_RYUK_DISABLED=false` no
`devcontainer.json` — o `<environmentVariables>` do surefire tem
precedência, o que é o comportamento desejado (consistência entre
ambientes).

## Estatísticas da sessão

- Iterações necessárias para `./mvnw test` verde no host: **4** (após scaffold).
- Iterações necessárias para `devcontainer up` verde: **4** (CA node, CA container,
  feature java falhou, feature dind precisou de CA Netskope).
- Correções permanentes aplicadas ao `pom.xml`: **2**
  - Versão do parent (`4.1.0.RELEASE` → `4.1.0`).
  - Surefire com Ryuk desabilitado.
- Arquivos adicionados aos gerados pelo Initializr: **6**
  - `.github/copilot-instructions.md`
  - `src/test/resources/testcontainers.properties`
  - `.devcontainer/devcontainer.json`
  - `.devcontainer/Dockerfile`
  - `.devcontainer/certs/.gitignore`
  - `.devcontainer/README.md`
  - `docs/` (esta pasta inteira).
- Nenhum código de aplicação foi escrito além do que o Initializr gera — este
  é intencionalmente um template, não uma amostra funcional.
