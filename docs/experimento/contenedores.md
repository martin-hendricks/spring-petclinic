# Contenedores — Fase 5

Artefactos para ejecutar la aplicación modernizada (Java 21 + capa `@Service` de F-03 + API REST
de F-05) en contenedores, sin tocar el `docker-compose.yml` existente (ese solo provisiona bases
de datos para desarrollo local, según documenta la cartografía A2).

## Archivos

| Archivo | Propósito |
|---|---|
| `Dockerfile` | Build multi-stage: JDK 21 → JRE 21 Alpine, usuario no root |
| `.dockerignore` | Excluye `target/`, `build/`, `.git/`, `docs/`, `*.md`, IDE |
| `docker-compose.app.yml` | Levanta la app + PostgreSQL con `depends_on: condition: service_healthy` |

## Cómo construir y ejecutar

```bash
# Build + run con la BD incluida
docker compose -f docker-compose.app.yml up -d --build

# Verificar
curl http://localhost:8080/api/owners/1     # JSON (F-05)
curl http://localhost:8080/owners/1         # HTML (Thymeleaf, sin regresión)
curl http://localhost:8080/actuator/health  # {"status":"UP"}

# Apagar
docker compose -f docker-compose.app.yml down -v
```

Para construir la imagen sola (sin compose), o correrla contra otro perfil (`mysql`, `h2`):

```bash
docker build -t spring-petclinic .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=mysql \
  -e MYSQL_URL=jdbc:mysql://host/petclinic spring-petclinic
```

## Decisiones de diseño

- **Multi-stage (build JDK 21 + runtime JRE 21 Alpine).** El stage de build necesita el JDK
  completo y Maven (vía `./mvnw`, coherente con el resto del proyecto) para compilar y empaquetar;
  el stage de runtime solo necesita ejecutar bytecode ya compilado, así que un JRE basta. Separar
  ambos evita cargar en la imagen final el compilador, las dependencias de test y el propio código
  fuente — solo viaja el JAR.
- **Cache de dependencias por capas.** Se copian primero `pom.xml`/`.mvn`/`mvnw` y se ejecuta
  `dependency:go-offline` **antes** de copiar `src/`; así, mientras no cambien las dependencias,
  Docker reutiliza esa capa aunque el código fuente cambie constantemente entre builds.
- **JRE Alpine, no JDK, en runtime.** `eclipse-temurin:21-jre-alpine` reduce tanto la superficie de
  ataque (sin compilador, sin herramientas de desarrollo) como el tamaño de imagen frente a una
  base JDK o Debian/Ubuntu completa. Alpine trae `wget` (vía busybox), suficiente para el
  `HEALTHCHECK` sin instalar `curl` aparte.
- **Usuario no root.** Se crea el usuario/grupo `spring` en el stage de runtime y se cambia a él
  con `USER spring:spring` antes del `ENTRYPOINT`. Si el proceso Java fuera comprometido, no
  tendría privilegios de root dentro del contenedor. Verificado en caliente: `docker compose exec
  app whoami` → `spring`, `id` → `uid=100(spring) gid=101(spring)`.
- **`SPRING_PROFILES_ACTIVE` parametrizable, con `postgres` como valor por defecto en el
  `Dockerfile`.** Coincide con el perfil que usa `docker-compose.app.yml`; se puede sobreescribir
  con `-e SPRING_PROFILES_ACTIVE=mysql`/`h2` al correr el contenedor suelto, sin reconstruir la
  imagen.
- **`docker-compose.app.yml` como archivo nuevo y separado.** El `docker-compose.yml` existente
  documentado en la cartografía A2 solo levanta bases de datos (sin la app); modificarlo habría
  invalidado esa evidencia. El nuevo compose usa `depends_on: condition: service_healthy` sobre un
  `HEALTHCHECK` de `pg_isready` en PostgreSQL, para que la app no arranque a intentar conectarse
  antes de que la base esté realmente lista.

## Verificación (ejecutada en esta sesión)

```
$ docker compose -f docker-compose.app.yml build
...
 Image spring-petclinic-app Built

$ docker compose -f docker-compose.app.yml up -d
 Container spring-petclinic-postgres-1  Healthy
 Container spring-petclinic-app-1       Started
 (HEALTHCHECK del contenedor app: healthy tras ~15s)

$ curl -s http://localhost:8080/api/owners/1
{"id":1,"firstName":"George","lastName":"Franklin", ... ,"pets":[...]}
→ HTTP 200

$ curl -s -o /dev/null -w "%{http_code} %{content_type}" http://localhost:8080/owners/1
→ 200 text/html;charset=UTF-8

$ curl -s http://localhost:8080/actuator/health
{"groups":["liveness","readiness"],"status":"UP"}

$ docker compose -f docker-compose.app.yml exec app whoami
spring

$ docker images spring-petclinic-app --format "{{.Size}}"
357MB
```

**Tamaño final de la imagen: 357 MB** (base `eclipse-temurin:21-jre-alpine` + JAR de Spring Boot
con todas las dependencias empaquetadas en `BOOT-INF/`).

No se crearon workflows de GitHub Actions ni ningún pipeline de CI/CD para esta fase: está fuera
del alcance del experimento (ver `CLAUDE.md`, sección "FUERA de alcance").
