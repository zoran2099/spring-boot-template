# catalog-api

API de referência de catálogo, somente leitura, construída com Java 25,
Spring Boot 4.1, Spring Data REST, Spring Security e PostgreSQL.

## Pré-requisitos

Para desenvolver e validar o projeto localmente, instale:

- Git;
- Eclipse Temurin JDK 25;
- Docker com o comando `docker compose` disponível.

Não é necessário instalar Maven nem PostgreSQL. O Maven Wrapper (`./mvnw`)
baixa a versão adequada do Maven, e o PostgreSQL pode ser iniciado pelo Docker
Compose ou pelo Testcontainers.

Confirme o ambiente antes de começar:

```bash
java -version
docker version
docker compose version
./mvnw -version
```

Com `mise`, instale globalmente a distribuição e a versão usadas pelo projeto:

```bash
mise use --global java@temurin-25
```

O daemon do Docker precisa estar em execução tanto para o Compose quanto para
os testes de integração.

## Configuração

### Variáveis usadas pelo Docker Compose

Crie o arquivo local de configuração a partir do exemplo:

```bash
cp .env.example .env
```

Edite `.env` e substitua todos os valores de exemplo:

| Variável | Obrigatória | Finalidade |
|---|---:|---|
| `DB_NAME` | Sim | Nome do banco PostgreSQL. |
| `DB_USER` | Sim | Usuário proprietário do banco. |
| `DB_PASSWORD` | Sim | Senha do banco. |
| `READER_USERNAME` | Sim | Usuário Basic Auth com acesso de leitura. |
| `READER_PASSWORD` | Sim | Senha do usuário `READER`. |
| `ADMIN_USERNAME` | Sim | Usuário Basic Auth administrativo. |
| `ADMIN_PASSWORD` | Sim | Senha do usuário `ADMIN`. |

O arquivo `.env` é ignorado pelo Git e não deve ser versionado. Não reutilize
as credenciais de exemplo fora de um ambiente local.

O Compose converte essas variáveis nas propriedades utilizadas pela aplicação.
Ao executar a aplicação diretamente, use os nomes abaixo:

| Variável da aplicação | Exemplo local |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/catalog` |
| `SPRING_DATASOURCE_USERNAME` | `catalog` |
| `SPRING_DATASOURCE_PASSWORD` | senha do PostgreSQL |
| `APP_SECURITY_READER_USERNAME` | `reader` |
| `APP_SECURITY_READER_PASSWORD` | senha do leitor |
| `APP_SECURITY_ADMIN_USERNAME` | `admin` |
| `APP_SECURITY_ADMIN_PASSWORD` | senha do administrador |

As configurações de banco e segurança não possuem credenciais padrão. A
aplicação falha na inicialização quando uma delas está ausente.

## Início rápido: aplicação completa em localhost

Este é o caminho recomendado para subir a API e o PostgreSQL juntos:

```bash
cp .env.example .env
# Edite o arquivo .env antes de continuar.
docker compose up --build -d
docker compose ps
docker compose logs -f app
```

Quando o log indicar que a aplicação iniciou, ela estará disponível em
`http://localhost:8080`. Pressione `Ctrl+C` para sair da visualização dos logs;
os containers continuarão em execução por causa da opção `-d`.

Valide a instalação:

```bash
curl http://localhost:8080/actuator/health
curl -u '<reader-username>:<reader-password>' \
  'http://localhost:8080/api/v1/products?page=0&size=20&sort=name,asc'
```

Na primeira inicialização, o perfil `local` cria ou atualiza as tabelas e a API
retorna uma coleção HAL vazia. Nenhum dado é carregado automaticamente.

Para acompanhar logs ou encerrar o ambiente:

```bash
docker compose logs -f app
docker compose down
```

`docker compose down` preserva o volume e os dados do PostgreSQL. Para também
apagar o banco local e recomeçar do zero, use conscientemente:

```bash
docker compose down --volumes
```

O PostgreSQL não publica a porta `5432` no host. Essa restrição é intencional;
apenas a aplicação e outros serviços da rede interna do Compose acessam o
banco.

### Consultar ou inserir dados no banco do Compose

Abra o cliente `psql` dentro do container:

```bash
docker compose exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Como a API não aceita escrita, dados manuais para desenvolvimento podem ser
inseridos diretamente no banco:

```sql
INSERT INTO categories (code, name)
VALUES ('BOOKS', 'Livros');

INSERT INTO products
    (sku, name, description, price, active, category_id)
SELECT
    'BOOK-001', 'Livro de arquitetura', 'Exemplo local', 99.90, true, id
FROM categories
WHERE code = 'BOOKS';
```

Saia do `psql` com `\q`.

## Rodar durante o desenvolvimento

### Opção 1: aplicação com PostgreSQL descartável via Testcontainers

Esse modo inicia automaticamente um PostgreSQL 16 em container e o remove ao
encerrar a JVM. Não é necessário configurar `SPRING_DATASOURCE_*`:

```bash
APP_SECURITY_READER_USERNAME=reader \
APP_SECURITY_READER_PASSWORD=reader-password \
APP_SECURITY_ADMIN_USERNAME=admin \
APP_SECURITY_ADMIN_PASSWORD=admin-password \
./mvnw spring-boot:test-run \
  -Dspring-boot.run.main-class=br.com.bbts.catalog.TestCatalogApiApplication \
  -Dspring-boot.run.profiles=local
```

A API ficará em `http://localhost:8080`. Encerre-a com `Ctrl+C`. Os dados desse
banco são temporários e não sobrevivem ao encerramento.

### Opção 2: aplicação com um PostgreSQL já existente

Configure um banco acessível pela máquina local e exporte todas as propriedades:

```bash
export SPRING_PROFILES_ACTIVE=local
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/catalog'
export SPRING_DATASOURCE_USERNAME='catalog'
export SPRING_DATASOURCE_PASSWORD='local-database-password'
export APP_SECURITY_READER_USERNAME='reader'
export APP_SECURITY_READER_PASSWORD='reader-password'
export APP_SECURITY_ADMIN_USERNAME='admin'
export APP_SECURITY_ADMIN_PASSWORD='admin-password'

./mvnw spring-boot:run
```

O banco do `compose.yaml` não serve diretamente para essa opção, pois sua porta
não é publicada no host. Use uma instância PostgreSQL externa ou prefira uma das
outras opções.

## Build

Use sempre o Maven Wrapper. O build completo compila, executa os testes e gera
o JAR executável:

```bash
./mvnw clean package
```

Como os testes usam Testcontainers, esse comando requer Docker em execução. O
artefato resultante é:

```text
target/catalog-api-0.0.1-SNAPSHOT.jar
```

Quando for necessário apenas verificar a compilação ou construir a imagem sem
executar testes locais:

```bash
./mvnw clean package -DskipTests
```

Não use `-DskipTests` como validação final de uma mudança.

### Rodar o JAR gerado

Com as variáveis da aplicação já exportadas e um PostgreSQL acessível:

```bash
java -jar target/catalog-api-0.0.1-SNAPSHOT.jar
```

Para usar outro perfil, altere `SPRING_PROFILES_ACTIVE` antes do comando. O
perfil `prod` exige que o schema já exista e seja compatível.

### Construir somente a imagem Docker

```bash
docker build -t catalog-api:local .
```

A imagem final executa com Eclipse Temurin 25, usa usuário não-root e espera que todas as
variáveis de banco e segurança sejam fornecidas na inicialização.

## Testes

Execute toda a suíte:

```bash
./mvnw test
```

Execute a verificação completa usada antes de entregar uma mudança:

```bash
./mvnw clean verify
docker compose --env-file .env.example config
```

Execute uma classe específica:

```bash
./mvnw -Dtest=ProductRepositoryIntegrationTests test
./mvnw -Dtest=CatalogApiHttpIntegrationTests test
```

Os testes iniciam PostgreSQL 16 real pelo Testcontainers e validam persistência,
filtros, HAL, paginação, projeção, autorização, métodos não permitidos,
Actuator e OpenAPI. Não é necessário criar `.env` para executar a suíte.

## API

Todos os recursos são HAL e ficam sob `/api/v1`:

| Método | Endpoint | Acesso |
|---|---|---|
| `GET` | `/api/v1/products` | `READER` ou `ADMIN` |
| `GET` | `/api/v1/products/{id}` | `READER` ou `ADMIN` |
| `GET` | `/api/v1/products/search/by-name?name=...` | `READER` ou `ADMIN` |
| `GET` | `/api/v1/products/search/by-active?active=true` | `READER` ou `ADMIN` |
| `GET` | `/api/v1/products/search/by-category?code=...` | `READER` ou `ADMIN` |
| `GET` | `/api/v1/categories` | `READER` ou `ADMIN` |
| `GET` | `/api/v1/categories/{id}` | `READER` ou `ADMIN` |

Paginação e ordenação usam `page`, `size` e `sort`; a página padrão tem 20
itens e o máximo é 100. A projeção detalhada de produto é selecionada com
`?projection=productDetails`.

Exemplos:

```bash
curl -u '<reader-username>:<reader-password>' \
  'http://localhost:8080/api/v1/products?projection=productDetails'

curl -u '<reader-username>:<reader-password>' \
  'http://localhost:8080/api/v1/products/search/by-name?name=arquitetura'

curl -u '<reader-username>:<reader-password>' \
  'http://localhost:8080/api/v1/products/search/by-active?active=true'

curl -u '<reader-username>:<reader-password>' \
  'http://localhost:8080/api/v1/products/search/by-category?code=BOOKS'
```

`POST`, `PUT`, `PATCH` e `DELETE` são desabilitados no nível do Spring Data
REST e respondem `405 Method Not Allowed`, inclusive para `ADMIN`.

## Segurança, documentação e observabilidade

- `READER` consulta o catálogo.
- `ADMIN` consulta o catálogo, Swagger e endpoints administrativos do Actuator.
- Health, liveness e readiness são públicos para probes.
- Swagger UI: `http://localhost:8080/swagger-ui.html`.
- OpenAPI: `http://localhost:8080/v3/api-docs`.
- Health: `http://localhost:8080/actuator/health`.
- Liveness: `http://localhost:8080/actuator/health/liveness`.
- Readiness: `http://localhost:8080/actuator/health/readiness`.
- Info: `http://localhost:8080/actuator/info`.
- Métricas: `http://localhost:8080/actuator/metrics`.

Exemplo de acesso administrativo:

```bash
curl -u '<admin-username>:<admin-password>' \
  http://localhost:8080/actuator/metrics
```

Basic Auth é a solução desta versão. Microsoft Entra ID será introduzido por
uma decisão arquitetural posterior. HTTPS deve terminar no ingress, gateway ou
proxy reverso; o perfil `prod` respeita os headers encaminhados por essa camada.

## Perfis

| Perfil | Hibernate | Uso |
|---|---|---|
| `local` | `ddl-auto=update` | Compose e desenvolvimento local. |
| `prod` | `ddl-auto=validate` | Produção com schema provisionado externamente. |

Não existe Flyway nesta versão. Portanto, iniciar com `prod` em um banco vazio
falhará por design.

## Solução de problemas

### Testes ou `spring-boot:test-run` não encontram o Docker

Confirme se `docker version` exibe informações do cliente e do servidor. Em
Linux, confirme também se o usuário atual possui acesso ao socket do Docker. O
devcontainer do repositório oferece Eclipse Temurin 25 e Docker-in-Docker para esse fluxo.

### O Compose informa que uma variável é obrigatória

Confirme que o arquivo se chama exatamente `.env`, está ao lado de
`compose.yaml` e contém todas as variáveis de `.env.example` com valores não
vazios. A configuração resolvida pode ser validada sem iniciar containers:

```bash
docker compose config
```

### A porta 8080 já está ocupada

Encerre o processo que usa a porta ou altere temporariamente o mapeamento do
serviço `app` em `compose.yaml`. A porta interna da aplicação deve continuar
sendo `8080`.

### Resposta `401 Unauthorized` ou `403 Forbidden`

`401` indica ausência ou falha da autenticação Basic. `403` normalmente indica
que um usuário `READER` tentou acessar uma rota exclusiva de `ADMIN`, como
Swagger, OpenAPI, `info` ou `metrics`.

### O perfil `prod` falha ao validar o schema

Esse perfil usa `spring.jpa.hibernate.ddl-auto=validate`. Provisione o schema
externamente antes de iniciar a aplicação ou use `local` durante o
desenvolvimento.

## Decisões arquiteturais

Os ADRs 0001–0009 registram a origem do template. O ADR 0010 descreve a
transformação para esta API de catálogo.
