# ADR 0002 — Testcontainers como dev-service (sem `docker-compose`)

- **Status:** Accepted
- **Data:** 2026-08-12
- **Decisor:** Sessão de bootstrap (padrão do Spring Initializr aceito
  conscientemente)

## Contexto

O template precisa de um Postgres real disponível tanto para:

1. **Desenvolvimento local** — subir a aplicação e experimentar endpoints sem
   configurar DB manualmente.
2. **Testes de integração** — garantir que JPA/queries funcionem contra o mesmo
   engine que produção (Postgres), não contra H2.

Havia duas famílias de solução:

- **`docker-compose.yml` externo** + `application-dev.properties` com credenciais
  fixas apontando para `localhost:5432`.
- **Testcontainers "dev services"** — padrão introduzido pelo Spring Boot 3.1+
  onde o container é declarado como `@Bean` e reaproveitado tanto em testes
  quanto no `spring-boot:test-run`.

## Decisão

Adotar o **padrão dev-services do Spring Boot** com Testcontainers, com uma
arquitetura de três arquivos:

1. **`src/main/java/com/example/template/SpringBootTemplateApplication.java`** —
   entry point de **produção**. Sem qualquer configuração de DB. Roda com
   `spring-boot:run` e requer datasource externo.

2. **`src/test/java/com/example/template/TestcontainersConfiguration.java`** —
   `@TestConfiguration(proxyBeanMethods = false)` declarando o container:
   ```java
   @Bean
   @ServiceConnection
   PostgreSQLContainer postgresContainer() {
       return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
   }
   ```
   A anotação `@ServiceConnection` é o mecanismo do Boot que **injeta
   automaticamente** JDBC URL, usuário e senha do container no `DataSource` do
   Spring — não é preciso mexer em `spring.datasource.*`.

3. **`src/test/java/com/example/template/TestSpringBootTemplateApplication.java`** —
   um `main()` sob `src/test` que compõe a aplicação de produção com a config
   de container:
   ```java
   SpringApplication.from(SpringBootTemplateApplication::main)
       .with(TestcontainersConfiguration.class)
       .run(args);
   ```
   Este é o entry point invocado por `./mvnw spring-boot:test-run` e é o **modo
   recomendado de rodar localmente**.

O mesmo `TestcontainersConfiguration` é usado em `@SpringBootTest` normais para
prover o Postgres nos testes de integração.

## Alternativas consideradas

1. **`docker-compose.yml` + profile `dev`** — mais explícito, mas exige:
   - Manter dois lugares em sincronia (compose + properties).
   - Documentação extra sobre `docker compose up -d` antes de rodar a app.
   - Segredo hardcoded no properties (mesmo que dummy).
   Descartado por adicionar cerimônia sem ganho técnico.

2. **H2 em memória** — descartado porque JPQL/DDL varia entre engines; testes que
   passam em H2 podem quebrar em Postgres (ex.: funções de string, tipos JSON,
   sequências).

3. **`@Testcontainers` clássico (JUnit extension)** — funciona só em testes, não
   cobre o caso "quero rodar a app localmente". A abordagem dev-service unifica
   os dois.

## Consequências

### Positivas

- **Um único comando local:** `./mvnw spring-boot:test-run` sobe tudo, inclusive
  DB, sem configuração adicional.
- **Zero segredos versionados** — sem senhas dummy em properties, sem `.env` no
  repositório.
- Testes de integração usam o **mesmo engine** que produção.
- Adicionar novos serviços (Redis, Kafka, MongoDB, etc.) segue o mesmo padrão:
  novo `@Bean ... @ServiceConnection` em `TestcontainersConfiguration`.

### Negativas / atenção

- **Docker precisa estar rodando** em qualquer máquina de dev ou CI que rode
  `spring-boot:test-run` ou `@SpringBootTest` que carregue essa config. Isso
  deve estar documentado no README do projeto derivado.
- **Ryuk (resource reaper) do Testcontainers está desabilitado** neste template
  para compatibilidade com Rancher Desktop / remote Docker / WSL2. Ver
  **ADR-0008** para detalhes e trade-offs.
- **`postgres:latest` é uma tag móvel**. Para produção-parity, os projetos
  derivados devem trocar por uma tag fixa (`postgres:16.4` etc.) alinhada com a
  versão de produção. `HELP.md` alerta explicitamente sobre isso.
- Primeira execução tem latência de pull da imagem. Em CI, cachear imagens
  Docker acelera.
- Não substitui um datasource de produção — o `SpringBootTemplateApplication`
  original **exige** configuração externa quando rodado com `spring-boot:run`.
  Isso é intencional e deve permanecer assim em projetos derivados.

## Anti-padrões a evitar em projetos derivados

- ❌ Colocar `spring.datasource.url=jdbc:tc:postgresql:...` em
  `application.properties`. Isso quebra o modo produção.
- ❌ Duplicar container em cada classe `@SpringBootTest`. Importe
  `TestcontainersConfiguration` via `@Import`.
- ❌ Adicionar `docker-compose.yml` "só para facilitar". Se a intenção é rodar
  local, use `spring-boot:test-run`; se é rodar contra DB externo, configure via
  variáveis de ambiente.
