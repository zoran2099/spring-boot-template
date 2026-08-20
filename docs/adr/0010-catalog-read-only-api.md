# ADR 0010 — API de catálogo somente leitura com Data REST

- **Status:** Accepted
- **Data:** 2026-08-20
- **Decisor:** Implementação do projeto derivado `catalog-api`
- **Supersede parcialmente:** ADR-0001, ADR-0002 e ADR-0004

## Contexto

O template precisava ser transformado em uma aplicação de referência que expusesse
um catálogo somente leitura, com autenticação, documentação, observabilidade e uma
execução local integralmente containerizada. A base já usava Java 21, Spring Boot
4.1, JPA, PostgreSQL e Testcontainers.

## Decisão

- Renomear o projeto para `br.com.bbts:catalog-api` e o pacote para
  `br.com.bbts.catalog`.
- Manter Spring Boot 4.1.0, apesar de o ADR de origem mencionar a família 3.x,
  preservando a base atual e os starters modularizados do Boot 4.
- Modelar produtos e categorias e expor somente repositórios explicitamente
  anotados pelo Spring Data REST em `/api/v1`.
- Usar HAL, projeção `productDetails`, paginação de 20 itens, limite de 100 e
  buscas pagináveis por nome, status e categoria.
- Desabilitar `POST`, `PUT`, `PATCH` e `DELETE` para recursos de coleção e item.
- Autenticar via Basic Auth stateless com credenciais externas e papéis `READER`
  e `ADMIN`. Entra ID fica para um ADR sucessor.
- Tornar health/liveness/readiness públicos e restringir Swagger e demais
  endpoints expostos do Actuator a `ADMIN`.
- Manter os caminhos do OpenAPI explicitamente, desabilitando a inferência automática
  do Spring Data REST que é incompatível com a combinação atual de HAL e Boot 4.1.
- Usar Hibernate `update` no perfil local e `validate` no perfil de produção,
  sem Flyway e sem carga automática de dados.
- Adotar Docker Compose para a pilha local completa e manter Testcontainers para
  testes. O banco fica em rede interna e sem porta publicada no host.
- Terminar HTTPS fora da aplicação, no gateway, ingress ou proxy reverso.

## Consequências

### Positivas

- A superfície HTTP de negócio é pequena, consistente e protegida.
- Compose oferece uma execução integrada; Testcontainers mantém isolamento nos testes.
- Segredos não recebem valores padrão nem são versionados.
- A aplicação está preparada para probes e documentação operacional protegida.

### Negativas e riscos

- HAL acopla consumidores ao formato hipermídia do Spring Data REST.
- Sem migrações versionadas, produção depende de provisionamento externo de schema;
  `ddl-auto=validate` interrompe a inicialização quando houver divergência.
- O banco local nasce vazio e a API não oferece forma HTTP de carga.
- Basic Auth é transitório e precisa ser substituído por OAuth2 Resource Server com
  Microsoft Entra ID antes do uso produtivo definitivo.

## Critérios de validação

- Apenas operações de leitura ficam expostas e escritas retornam HTTP 405.
- A API exige `READER` ou `ADMIN`; Swagger e Actuator administrativo exigem `ADMIN`.
- Health funciona anonimamente.
- Testes de persistência usam PostgreSQL real via Testcontainers.
- O Compose publica somente a porta 8080 da aplicação.
