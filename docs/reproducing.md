# Reproduzindo este template do zero

Este documento condensa o **caminho feliz** para reconstruir o template
inteiro a partir de um diretório vazio, aplicando desde o início todas as
correções e patches descobertos durante o bootstrap (ver `session-log.md`
para o caminho completo com erros e investigação).

## Pré-requisitos

| Item | Versão mínima | Verificação |
|---|---|---|
| Java | 21 | `java -version` deve mostrar `21` |
| Maven Wrapper | (bundled) | `./mvnw --version` após scaffold |
| Docker | qualquer daemon compatível | `docker ps` responde |
| `curl`, `tar`, `sed` | qualquer | padrão em Linux/macOS |
| Copilot CLI (opcional) | `1.0.79+` | `copilot --version` |

Docker precisa estar rodando. Docker Desktop, Rancher Desktop, Colima e
remote Docker são suportados — a configuração do template desabilita Ryuk
justamente para portabilidade.

## Passo 1 — Scaffold via Spring Initializr

Verifique **antes** se a versão de Boot atualmente exposta como default é
`4.1.0` ou mais recente:

```bash
curl -sS https://start.spring.io/metadata/client | python3 -c "
import json, sys
d = json.load(sys.stdin)
print('boot default:', d['bootVersion']['default'])
print('java default:', d['javaVersion']['default'])
"
```

Baixe o starter:

```bash
curl -sSL \
  "https://start.spring.io/starter.tgz?type=maven-project&language=java&javaVersion=21&bootVersion=4.1.0.RELEASE&groupId=com.example&artifactId=spring-boot-template&name=spring-boot-template&packageName=com.example.template&dependencies=web,data-jpa,validation,actuator,lombok,postgresql,testcontainers,devtools" \
  -o starter.tgz \
&& tar -xzf starter.tgz \
&& rm starter.tgz
```

> **Se você usar outro `groupId`/`artifactId`/`packageName`**, ajuste
> também os nomes de classe (`SpringBootTemplateApplication`,
> `TestcontainersConfiguration`, `TestSpringBootTemplateApplication`),
> caminhos de pacote em `src/**/java/**`, e referências em
> `.github/copilot-instructions.md` e nos ADRs.

## Passo 2 — Patch da versão do Spring Boot (ADR-0007)

O Initializr entrega `<version>4.1.0.RELEASE</version>` mas Maven Central
publica apenas `4.1.0`. Corrija:

```bash
sed -i 's|<version>4.1.0.RELEASE</version>|<version>4.1.0</version>|' pom.xml
sed -i 's|4.1.0.RELEASE|4.1.0|g' HELP.md
```

Confirme a versão pretendida está publicada antes de bumpar:

```bash
curl -sS -o /dev/null -w "%{http_code}\n" \
  https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/4.1.0/spring-boot-starter-parent-4.1.0.pom
# → 200
```

## Passo 3 — Configurar Surefire com Ryuk desabilitado (ADR-0008)

Edite `pom.xml` e adicione o plugin `maven-surefire-plugin` **antes** de
`maven-compiler-plugin` dentro de `<build><plugins>`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <systemPropertyVariables>
            <testcontainers.ryuk.disabled>true</testcontainers.ryuk.disabled>
        </systemPropertyVariables>
        <environmentVariables>
            <TESTCONTAINERS_RYUK_DISABLED>true</TESTCONTAINERS_RYUK_DISABLED>
        </environmentVariables>
    </configuration>
</plugin>
```

Adicione também o fallback documental de classpath:

```bash
mkdir -p src/test/resources
cat > src/test/resources/testcontainers.properties <<'EOF'
# Disable Ryuk (resource reaper container) — required for Docker daemons that
# do not expose their internal port mapping cleanly to the host (Rancher
# Desktop, remote Docker, WSL2 without host.docker.internal, corporate
# proxies). Containers are still stopped by the JVM shutdown hook.
ryuk.disabled=true
EOF
```

## Passo 4 — Validação do build

```bash
./mvnw -B -ntp clean verify
```

Esperado:
- Compila `src/main` e `src/test`.
- Roda `SpringBootTemplateApplicationTests.contextLoads` — sobe o
  Postgres via Testcontainers, `Tests run: 1, Failures: 0, Errors: 0`.
- Empacota `target/spring-boot-template-0.0.1-SNAPSHOT.jar`.
- Final: `BUILD SUCCESS`.

## Passo 5 — Validação do runtime

```bash
./mvnw spring-boot:test-run
```

Aguarde os logs até:

```
Started SpringBootTemplateApplication in X.XX seconds
Tomcat started on port 8080 (http)
```

Em outro shell:

```bash
curl -sS -w "\nHTTP:%{http_code}\n" http://localhost:8080/actuator/health
# → {"groups":["liveness","readiness"],"status":"UP"}
# → HTTP:200
```

`Ctrl+C` para parar. O shutdown hook derruba o container Postgres.

## Passo 6 — Copiar/criar arquivos de IA

### `.github/copilot-instructions.md`

Copie o arquivo deste repositório na íntegra. Se tiver alterado o
`groupId`/`packageName`, substitua `com.example.template` para o novo pacote.

### `docs/`

Copie a pasta inteira (`README.md`, `adr/*.md`, `session-log.md`, este
arquivo). Ao evoluir o projeto derivado:

- **Não edite ADRs existentes.** Se uma decisão mudar, crie um novo ADR
  com status `Supersedes ADR-XXXX`.
- Atualize o índice em `docs/README.md`.

## Passo 7 — Instalar plugins do Copilot CLI (ADR-0006)

Uma vez por máquina de dev / node de CI:

```bash
copilot plugin install java-development@awesome-copilot
copilot plugin install testing-automation@awesome-copilot
copilot plugin install security-best-practices@awesome-copilot
copilot plugin install modernize-java@awesome-copilot
copilot plugin install openapi-to-application-java-spring-boot@awesome-copilot

copilot plugin list
```

Se o marketplace não estiver registrado:

```bash
copilot plugin marketplace add github/awesome-copilot
```

## Passo 8 — Renomeações no projeto derivado

Ao clonar este template para um projeto novo:

1. **Coordenar** a mudança de `groupId`/`artifactId`/`packageName` em:
   - `pom.xml` (`<groupId>`, `<artifactId>`, `<name>`).
   - Diretórios sob `src/main/java` e `src/test/java`.
   - Nomes de classe (`SpringBootTemplateApplication` → `NomedoProjetoApplication` etc.).
   - `.github/copilot-instructions.md` (referências ao pacote base).
   - Docs em `docs/adr/*.md` (referências ao pacote).
2. Atualizar `spring.application.name` em `application.properties`.
3. Trocar `postgres:latest` em `TestcontainersConfiguration` para tag fixa
   alinhada com produção (ex.: `postgres:16.4`) — ver ADR-0002.
4. Criar o próprio `README.md` do projeto derivado explicando *o que*
   ele faz (o template documenta *como* está montado, não o negócio).

## Checklist final

- [ ] `java -version` → 21
- [ ] `docker ps` → conecta
- [ ] `./mvnw -B -ntp clean verify` → BUILD SUCCESS
- [ ] `./mvnw spring-boot:test-run` → app sobe em `localhost:8080`
- [ ] `curl localhost:8080/actuator/health` → `{"status":"UP"}`
- [ ] `.github/copilot-instructions.md` presente
- [ ] `docs/adr/*.md` presentes (8 arquivos)
- [ ] `copilot plugin list` → 5 plugins do baseline

Se qualquer item falhar, consulte a seção correspondente do
`session-log.md` ou o ADR indicado.
