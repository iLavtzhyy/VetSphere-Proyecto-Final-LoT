# ── VetSphere · Mini servicio SOAP (Spring Boot) ──────────────────────────
# Etapa 1: compilar con Maven
FROM maven:3.9-eclipse-temurin-17 AS compilacion
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Etapa 2: imagen de ejecución mínima
FROM eclipse-temurin:17-jre
WORKDIR /app
EXPOSE 8090
COPY --from=compilacion /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
