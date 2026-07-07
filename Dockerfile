# Etapa 1: Construção (usa a arquitetura nativa do build)
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

#copia maven wrapper e o pom.xml primeiro
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml .

#baixa as dependências
RUN ./mvnw dependency:go-offline -B

#copia o código fonte e compila
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Etapa 2: Imagem final (usa a arquitetura alvo)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

EXPOSE 9999
USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]