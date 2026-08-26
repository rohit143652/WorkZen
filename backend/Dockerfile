# ============================================================
# WORKZEN backend - multi-stage Dockerfile
#
# Stage 1 (build): uses a Maven+JDK image to compile and package the jar.
# Stage 2 (runtime): copies ONLY the built jar into a slim JRE-only image -
# the final image doesn't carry Maven, the full JDK, or your source code,
# which keeps it much smaller and reduces attack surface.
#
# This project has no Maven wrapper (mvnw) checked in, so the build stage
# uses a base image that already has Maven installed, rather than
# depending on ./mvnw being present.
# ============================================================

# ---- Stage 1: build ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy just the POM first so Maven's dependency download is cached in its
# own Docker layer - it only re-runs when pom.xml itself changes, not on
# every source-code edit, which makes rebuilds much faster.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Matches pom.xml's <artifactId>workzen</artifactId> / <version>0.0.1-SNAPSHOT</version> -
# update this if you ever change either of those in pom.xml.
COPY --from=build /app/target/workzen-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# DB_URL, DB_USERNAME, DB_PASSWORD, and JWT_SECRET have no defaults anywhere in this
# project (application.yml) - they MUST be supplied when running this image, e.g.:
#   docker run -p 8080:8080 \
#     -e DB_URL=jdbc:mysql://<host>:3306/workforce_auth?useSSL=false&serverTimezone=UTC \
#     -e DB_USERNAME=... -e DB_PASSWORD=... -e JWT_SECRET=... \
#     -e SPRING_PROFILES_ACTIVE=prod \
#     workzen-backend
ENTRYPOINT ["java", "-jar", "app.jar"]
