# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-21 AS dependencies

WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline

FROM dependencies AS build
COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre AS runtime

RUN groupadd --system catalog \
	&& useradd --system --gid catalog --home-dir /app --shell /usr/sbin/nologin catalog

WORKDIR /app
COPY --from=build --chown=catalog:catalog /workspace/target/catalog-api-*.jar app.jar

USER catalog
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
