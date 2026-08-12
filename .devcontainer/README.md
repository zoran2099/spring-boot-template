# `.devcontainer/` — ambiente isolado e reprodutível

Este diretório provê um **VS Code Dev Container** (spec `devcontainers.json`) que
isola o toolchain de desenvolvimento em um container Debian com Java 21, Maven
Wrapper, Docker-in-Docker e as CAs corporativas necessárias para o proxy
Netskope/BBTS.

## Por que existe

- Padroniza a versão de Java (21.0.8 LTS via Microsoft OpenJDK) e Maven em
  qualquer máquina, independentemente do que está instalado no host.
- Isola o daemon Docker usado por Testcontainers via **Docker-in-Docker**:
  o build de teste não polui/depende do daemon do host (Docker Desktop,
  Rancher Desktop, Docker CE nativo).
- Aceita o proxy corporativo de TLS-inspection sem exigir configuração
  manual em cada dev.

## Estrutura

```
.devcontainer/
├── devcontainer.json    # spec principal (features, extensões, env)
├── Dockerfile           # base image + instalação das CAs corporativas
├── certs/               # CAs (NÃO versionadas — ver certs/.gitignore)
│   └── .gitignore
└── README.md            # este arquivo
```

## Populando `certs/`

Os arquivos CA em `certs/` **não são versionados** (`.gitignore` bloqueia
tudo). Cada dev precisa popular o diretório antes do primeiro build.

### Automático (openSUSE / SLES corporativo)

```bash
cd .devcontainer/certs

# CAs BBTS já instaladas no host
cp /etc/pki/trust/anchors/BBcerts_*.pem . 2>/dev/null || true
cp /etc/pki/trust/anchors/ACBancodoBrasil*.crt . 2>/dev/null || true

# CAs do proxy Netskope (extraídas da chain apresentada)
openssl s_client -connect packages.microsoft.com:443 -showcerts </dev/null 2>/dev/null | \
  awk '/BEGIN CERT/,/END CERT/' > /tmp/chain.pem
awk '/-----BEGIN CERTIFICATE-----/{n++} n>1{print > ("/tmp/cert_" n ".pem")}' /tmp/chain.pem
cp /tmp/cert_2.pem netskope-intermediate-bbts.pem
cp /tmp/cert_3.pem netskope-root-ca.pem
```

### Manual (outros ambientes)

Copie qualquer CA PEM/CRT necessária para o `certs/`. O Dockerfile normaliza
extensões (`.pem` e `.cer` viram `.crt`) e roda `update-ca-certificates`
mais o import no `cacerts` do JDK.

## Subindo o devcontainer

### Via VS Code

Abra a pasta do projeto e execute o comando "Dev Containers: Reopen in
Container". A extensão "Dev Containers"
(`ms-vscode-remote.remote-containers`) precisa estar instalada.

### Via CLI

```bash
npm install -g @devcontainers/cli
NODE_EXTRA_CA_CERTS=/etc/ssl/ca-bundle.pem \
  devcontainer up --workspace-folder .
```

A env var `NODE_EXTRA_CA_CERTS` é necessária para o próprio CLI Node.js
falar com `ghcr.io` (registry dos devcontainer features) através do
proxy corporativo.

## Rodando comandos dentro

```bash
# Build + testes (Testcontainers usa Docker-in-Docker)
devcontainer exec --workspace-folder . ./mvnw -B -ntp clean verify

# Rodar a aplicação
devcontainer exec --workspace-folder . ./mvnw spring-boot:test-run
```

Verificado nesta sessão:
- `BUILD SUCCESS`, `Tests run: 1, Failures: 0, Errors: 0`.
- Testcontainers sobe `postgres:latest` **dentro** do daemon DinD do
  container, sem depender do host.
- Ryuk permanece desabilitado pelo `pom.xml` (ADR-0008) — funciona também
  aqui, e a warning `Ryuk has been disabled` aparece nos logs.

## Trade-offs

### Positivos

- Reprodutibilidade total: `java -version`, `docker --version` e
  `spring-boot:test-run` produzem o mesmo resultado em qualquer máquina
  com o container rodando.
- Zero interferência entre projetos: cada devcontainer tem seu próprio
  `~/.m2` (via volume ou dentro do container).
- Elimina dependência do daemon Docker do host — funciona em máquinas
  sem Docker instalado localmente, desde que o motor de container do dev
  container (Docker Desktop, Rancher, Podman) esteja ativo.

### Negativos / atenção

- **Primeiro build é lento** (~3–5 min) — pull da imagem base
  `mcr.microsoft.com/devcontainers/java:1-21-bookworm` (~1 GB), instalação
  do feature docker-in-docker, import das CAs no `cacerts`.
- Requer `--privileged` (habilitado por default pelo feature DinD).
  Aceitar essa flag em CI/enterprise pode exigir aprovação de segurança.
- Docker-in-Docker consome mais recursos: dois níveis de daemon, imagens
  postgres baixadas duas vezes (host e DinD). Considere `docker-outside-of-docker`
  se o dev quer compartilhar cache com o host (edite `devcontainer.json`
  trocando a feature).
- Os certs em `certs/` são **sensíveis** (CAs de MITM corporativo). Nunca
  versionar. Nunca compartilhar fora do time BBTS.
