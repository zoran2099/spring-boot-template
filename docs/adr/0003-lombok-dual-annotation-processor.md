# ADR 0003 — Lombok com annotation processor em `default-compile` **e** `default-testCompile`

- **Status:** Accepted
- **Data:** 2026-08-12
- **Decisor:** Configuração gerada pelo Spring Initializr, revisada e mantida.

## Contexto

Lombok é usado para reduzir boilerplate (`@Data`, `@Getter`, `@Builder`,
`@RequiredArgsConstructor` etc.). Ele funciona como **annotation processor**
em tempo de compilação: se não for registrado, o compilador não gera os métodos
e o build falha com erros do tipo "cannot find symbol: method getX()".

A partir do JDK moderno, o Maven **exige** que os annotation processors sejam
declarados explicitamente em `<annotationProcessorPaths>` de cada `execution` do
`maven-compiler-plugin` (não basta ter a dependência no classpath).

## Decisão

O `pom.xml` registra Lombok como annotation processor em **duas execuções
separadas** do `maven-compiler-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <executions>
        <execution>
            <id>default-compile</id>
            <phase>compile</phase>
            <goals><goal>compile</goal></goals>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </execution>
        <execution>
            <id>default-testCompile</id>
            <phase>test-compile</phase>
            <goals><goal>testCompile</goal></goals>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </execution>
    </executions>
</plugin>
```

E a dependência é `<optional>true</optional>` para não vazar Lombok como
dependência transitiva:

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

## Alternativas consideradas

1. **Apenas `default-compile`** — código principal compilaria, mas testes com
   `@Builder` em POJOs de teste falhariam. Descartado.
2. **Configuração global do plugin** (fora de `<executions>`) — parece mais
   simples, mas não é o padrão gerado pelo Initializr e pode conflitar quando
   outros annotation processors forem adicionados. Mantemos o padrão.
3. **Não usar Lombok** — trade-off legítimo, mas descartado porque foi listado
   nos "defaults sensatos" do usuário e é convenção estabelecida no ambiente
   alvo.

## Consequências

### Positivas

- Compilação previsível tanto de `src/main` quanto de `src/test`.
- Adicionar outros processors (MapStruct, Micronaut inject, QueryDSL, Immutables,
  etc.) é uma extensão natural: acrescentar mais `<path>` **nos dois blocos**.

### Negativas / atenção

- **⚠ Sempre adicionar novos annotation processors em AMBOS os `<execution>`.**
  Este é o erro mais comum ao evoluir o `pom.xml`. Sintoma: testes falham
  compilando mesmo quando `mvn compile` passa.
- MapStruct + Lombok exigem ordem específica: `lombok-mapstruct-binding` deve vir
  antes de `mapstruct-processor`. Documentar em novo ADR se/quando MapStruct for
  adicionado.
- IDEs (IntelliJ/VS Code) precisam do plugin Lombok instalado e da opção "Enable
  annotation processing" ativada — não é problema do Maven, mas causa confusão
  no primeiro clone. Considerar mencionar no README do projeto derivado.

## Reforço no `copilot-instructions.md`

A seção **"Lombok"** do arquivo `.github/copilot-instructions.md` alerta
explicitamente sobre a necessidade de duplicar novos processors. Isso existe
para prevenir que sessões futuras do Copilot "consertem" o `pom.xml` removendo
a "duplicação" aparentemente redundante.
