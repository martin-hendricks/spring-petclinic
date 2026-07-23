# syntax=docker/dockerfile:1

# ---- Build stage: compila el JAR con JDK 21 + Maven (via el wrapper del proyecto) ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copiamos primero solo lo necesario para resolver dependencias, para que esa capa
# se reutilice de cache mientras no cambie el pom.xml.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests package

# ---- Runtime stage: solo JRE, usuario no root ----
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY --from=build /workspace/target/spring-petclinic-*.jar app.jar
RUN chown spring:spring app.jar

USER spring:spring

# Parametrizable: en docker-compose.app.yml se usa "postgres"; se puede sobreescribir
# (p. ej. -e SPRING_PROFILES_ACTIVE=mysql) al correr el contenedor suelto.
ENV SPRING_PROFILES_ACTIVE=postgres

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
	CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
