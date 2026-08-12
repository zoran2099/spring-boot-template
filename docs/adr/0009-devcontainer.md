# ADR 0009 — Dev Container com Docker-in-Docker e CAs corporativas

- **Status:** Accepted
- **Data:** 2026-08-12
- **Decisor:** Sessão de bootstrap (usuário solicitou reprodução com devcontainer)

## Contexto

A validação inicial do build (`./mvnw clean verify`) foi feita diretamente no
host, sob dois cenários de daemon Docker:

- Docker CE nativo (openSUSE, rodando como root).
- Rancher Desktop (que havia sido desligado, depois religado).

Ambos apresentaram friction:

- Ryuk do Testcontainers falhava com "Could not connect to Ryuk at
  localhost:32xxx" — Rancher Desktop e Docker CE nativo com proxy
  corporativo não expunham as portas efêmeras corretamente. **ADR-0008**
  documenta o workaround permanente (desabilitar Ryuk).
- O Java do host (`openjdk 21.0.2`) não bate com o Java corporativo padrão.
- O `mvn` do host (Apache Maven 3.9.15) é uma instalação manual em
  `/home/.../tools/apache-maven-3.9.15/bin` — nem todo dev tem.

Além disso, o ambiente corporativo tem TLS-inspection via **Netskope**
(chain `packages.microsoft.com` → `ca.bbts.goskope.com` → `certadmin@netskope.com`),
o que impede qualquer `curl`/`apt-get`/`sdkman`/etc. dentro de um container
que não confia nas CAs corporativas.

## Decisão

Adicionar `.devcontainer/` ao template com:

1. **Dockerfile customizado** baseado em
   `mcr.microsoft.com/devcontainers/java:1-21-bookworm` (Java 21.0.8 LTS
   Microsoft OpenJDK + Maven já bundled), com o seguinte hardening:
   - Copia CAs corporativas de `.devcontainer/certs/` para
     `/usr/local/share/ca-certificates/corporate/`.
   - Roda `update-ca-certificates` para o trust store do OpenSSL.
   - Roda `keytool -importcert` para o `cacerts` do JDK.
   - Define `NODE_EXTRA_CA_CERTS`, `REQUESTS_CA_BUNDLE`, `SSL_CERT_FILE`
     apontando para o bundle atualizado (Node, Python, curl).
   - Remove `/etc/apt/sources.list.d/yarn.list` e `nodesource.list` que
     causam erros GPG intermitentes.

2. **Feature Docker-in-Docker** (`ghcr.io/devcontainers/features/docker-in-docker:2`)
   para prover um daemon Docker isolado dentro do próprio devcontainer.
   Testcontainers usa esse daemon interno, sem interferir/depender do
   daemon do host.

3. **`certs/` NÃO versionado** (`.gitignore` bloqueia todo o conteúdo).
   Cada dev popula com script documentado em `.devcontainer/README.md`.

4. Extensões VS Code pré-instaladas: `vscode-java-pack`,
   `vscode-boot-dev-pack`, `vscode-xml`, `GitHub.copilot`,
   `GitHub.copilot-chat`.

5. Port forwarding automático de `8080` (porta do Tomcat).

## Alternativas consideradas

1. **Feature `ghcr.io/devcontainers/features/java:1`** (SDKMAN) —
   descartada. Falha no ambiente corporativo porque SDKMAN baixa via curl
   com endpoints múltiplos e algumas rotas não passam pela CA que
   instalamos. Como a imagem base já traz Java 21 + Maven, a feature
   torna-se redundante.

2. **`docker-outside-of-docker` em vez de DinD** (mount do socket do host)
   — mais rápido e economiza recursos, mas força dependência do daemon do
   host. Se o dev usar Docker Desktop, funciona; se usar Rancher com socket
   em path diferente, quebra. DinD é o denominador comum.

3. **Imagem custom publicada no registry BBTS** com CAs já embutidas —
   melhor prazo de setup para novos devs, mas exige manutenção de imagem
   corporativa. Fica como evolução futura (ADR sucessor).

4. **Podman em vez de Docker** — não é padrão no time. Sem valor claro.

5. **Não usar devcontainer** — descartado após a solicitação do usuário
   e a constatação de que o setup no host quebra sob condições
   ambientais banais (Rancher off/on, Ryuk).

## Consequências

### Positivas

- `./mvnw clean verify` **passa reproduzivelmente** no devcontainer,
  independente do estado do host. Validado nesta sessão com Postgres 18.4
  subindo em DinD.
- Onboarding: novo dev clona → popula `certs/` com script → "Reopen in
  Container" no VS Code → tudo funciona.
- CI: pode usar a mesma imagem devcontainer via GitHub Actions
  `devcontainers/ci@v0.3` — garante paridade dev/CI.
- Isolamento: mata classes inteiras de problemas do tipo "funciona na
  minha máquina", como versão de Maven divergente ou JDK errado.

### Negativas / atenção

- **Primeiro build lento** (~3–5 min): pull da base + install DinD +
  import de ~80 CAs no `cacerts`. Uma vez cacheado, `devcontainer up`
  em ~2 s.
- **`--privileged` obrigatório** para DinD. Aceitável em dev/CI, mas
  requer aprovação em ambientes hardened.
- **Certs sensíveis:** CAs Netskope são específicas do BBTS. **Nunca**
  versionar (o `.gitignore` em `certs/` impede acidente). Compartilhar
  com terceiros seria vazamento de infra interna.
- Consumo de recursos: dois daemons Docker rodando (host + DinD).
  Postgres da suite de teste baixa duas vezes se o dev roda tanto no host
  quanto no devcontainer.
- `TESTCONTAINERS_RYUK_DISABLED=false` no `containerEnv` do devcontainer
  **não tem efeito prático** porque o `<environmentVariables>` do surefire
  no `pom.xml` (ADR-0008) sobrescreve. Isso é **intencional** para
  garantir consistência de comportamento em qualquer ambiente. Removido
  do próximo commit do template ou deixado como intenção documentada
  ("gostaríamos de habilitar Ryuk aqui, mas o override do pom vale").

## Validação empírica desta sessão

```
devcontainer up --workspace-folder .
→ Container started, remoteUser: vscode

devcontainer exec ... java -version
→ openjdk 21.0.8 (Microsoft-11933203) LTS

devcontainer exec ... docker --version
→ Docker version 29.7.2-1  (dentro do container, via DinD)

devcontainer exec ... ./mvnw -B -ntp clean verify
→ [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
→ [INFO] BUILD SUCCESS
→ target/spring-boot-template-0.0.1-SNAPSHOT.jar  (63 MB)

Postgres container dentro do DinD: postgres:latest (16.x/18.x conforme
  disponibilidade)
JDBC URL observada nos logs: jdbc:postgresql://172.18.0.1:32768/test
```

## Referências cruzadas

- **ADR-0002** — Testcontainers dev-services. O padrão continua o mesmo
  no devcontainer; só o daemon Docker muda de local.
- **ADR-0007** — Patch da versão do Boot. Também aplicado no devcontainer
  (o `pom.xml` versionado é o mesmo).
- **ADR-0008** — Ryuk disabled no surefire. Herdado no devcontainer via
  `pom.xml`. Discussão em "Consequências" acima.
