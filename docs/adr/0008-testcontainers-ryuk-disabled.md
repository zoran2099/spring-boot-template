# ADR 0008 — Desabilitar Testcontainers Ryuk no Surefire

- **Status:** Accepted
- **Data:** 2026-08-12

## Contexto

Após corrigir a versão do Boot (ADR-0007), a suite de teste ainda falhava:

```
Caused by: java.lang.IllegalStateException: Could not connect to Ryuk
at localhost:32769
```

**Ryuk** é um sidecar container que o Testcontainers sobe automaticamente
para "reaper" (matar) containers órfãos se o processo Java for morto de
forma abrupta. Ele estabelece um socket TCP ligado a uma porta efêmera
mapeada pelo Docker.

Ryuk falha quando:

- O Docker daemon não expõe portas de containers no host da mesma forma
  que Docker Desktop clássico (caso do **Rancher Desktop**, usado neste
  ambiente).
- Docker roda via SSH remoto ou socket montado.
- WSL2 sem `host.docker.internal` configurado.
- Proxies corporativos filtram tráfego para `localhost:32xxx`.

Verificação empírica no ambiente (`docker ps` mostrou containers Rancher
Desktop rodando; comando `.rd/bin/docker` no `PATH`).

## Decisão

Configurar o **surefire plugin** no `pom.xml` para passar a variável de
ambiente `TESTCONTAINERS_RYUK_DISABLED=true` para o JVM dos testes:

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

Configuramos **os dois formatos** (system property + env var) porque:

- `testcontainers.ryuk.disabled` (system property) é o formato canônico
  documentado.
- `TESTCONTAINERS_RYUK_DISABLED` (env var) é o formato que efetivamente
  funciona em todas as versões do Testcontainers (verificado nesta
  sessão — só a env var pegou em Testcontainers 2.0.5).

Também criado `src/test/resources/testcontainers.properties`:

```properties
# Fallback / documentation. Testcontainers reads this file from ~/… or the
# classpath, but Surefire's forked JVM does not always pick it up in time.
ryuk.disabled=true
```

Esse arquivo é **fallback documental**: sob Surefire ele nem sempre é lido
antes de o Ryuk tentar subir, mas serve como pista para quem investigar
o comportamento fora do Maven (IDE, `java -jar`, etc.).

## Alternativas consideradas

1. **Só `testcontainers.properties` no classpath** — testado e insuficiente:
   Surefire não garantiu a leitura antes da instanciação de Ryuk. Descartado
   como solução única.

2. **Só system property (`-Dtestcontainers.ryuk.disabled=true`)** — testado
   nesta sessão e **não teve efeito** com Testcontainers 2.0.5. Confirmação
   empírica: env var funcionou; system property sozinha não.

3. **Só env var externa** (`TESTCONTAINERS_RYUK_DISABLED=true ./mvnw test`) —
   funciona mas é frágil: quebra em CI/IDE se alguém esquecer de exportar.
   Encapsular no `pom.xml` torna a build reprodutível.

4. **Configurar Docker Desktop em vez de Rancher** — não é decisão do
   template. Rancher é a instalação padrão do ambiente corporativo alvo
   (licenciamento). O template precisa funcionar em ambos.

5. **Ryuk habilitado, aceitando falha em Rancher** — inaceitável: metade
   do time usaria testes que falham sem culpa própria.

## Consequências

### Positivas

- `./mvnw test` funciona out-of-the-box em Docker Desktop, Rancher Desktop,
  Colima, remote Docker, e no cloud agent do Copilot — verificado nesta
  sessão em Rancher.
- Ninguém precisa exportar env var manualmente.
- Configuração é auditável no `pom.xml` (não escondida em `~/.testcontainers.properties`).

### Negativas / atenção

- ⚠ **Containers órfãos:** se o processo Java for morto com `kill -9`
  (não `Ctrl+C` normal), o Postgres container **pode ficar rodando**
  ocupando porta. Ryuk é quem faria essa limpeza. Mitigação:
  - JVM shutdown hook do Testcontainers cobre `SIGTERM`/`Ctrl+C` (99% dos
    casos).
  - Docker Desktop / Rancher têm limites de recursos que evitam explosão.
  - Um script de manutenção local pode ser `docker ps --filter "label=org.testcontainers=true" -q | xargs -r docker rm -f`.
- Log de teste emite warning:
  ```
  Ryuk has been disabled. This can cause unexpected behavior in your environment.
  ```
  Isso é **esperado** — não é bug do template, é lembrete do Testcontainers.
  Documentado em `.github/copilot-instructions.md` para prevenir "fix"
  bem-intencionado que remova a config.
- Se um dia o time padronizar Docker Desktop (com Ryuk funcional), esta
  decisão pode ser revertida por ADR sucessor. Não é urgente.

## Reforço no `copilot-instructions.md`

Adicionada seção específica sobre Ryuk explicitando que:
1. A configuração no `pom.xml` **não deve ser removida**.
2. Rodar testes fora do Maven exige `TESTCONTAINERS_RYUK_DISABLED=true`
   como env var.
3. O arquivo `testcontainers.properties` é fallback, não fonte primária.

## Validação empírica desta sessão

```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

E, no `spring-boot:test-run`:
```
Container postgres:latest started in PT2.016725271S
Tomcat started on port 8080 (http)
Started SpringBootTemplateApplication in 6.39 seconds
```

Endpoint `GET /actuator/health` → `HTTP 200`, `{"status":"UP"}`.
