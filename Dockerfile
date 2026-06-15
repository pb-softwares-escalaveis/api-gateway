# Etapa 1: Construção
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Etapa 2: Imagem final de execução
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

EXPOSE 9999
USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]