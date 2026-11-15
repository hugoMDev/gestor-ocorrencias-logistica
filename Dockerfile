# =========================================================================
# Etapa 1: Build da aplicação utilizando Maven e Eclipse Temurin JDK 21
# =========================================================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copia o descritor do projeto e o código-fonte
COPY pom.xml .
COPY src ./src

# Compila a aplicação e gera o JAR executável pulando os testes unitários
RUN mvn clean package -DskipTests && cp target/gestor-ocorrencias-logistica-*.jar app.jar

# =========================================================================
# Etapa 2: Imagem leve para execução (Runtime) com Eclipse Temurin JRE 21
# =========================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Criação de usuário não-root por segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copia o executável JAR consolidado da etapa de build
COPY --from=build /app/app.jar app.jar

# Configura permissões
RUN chown -R appuser:appgroup /app
USER appuser

# Expõe a porta (o Render injetará a variável de ambiente $PORT em tempo de execução)
EXPOSE 8080

# Executa o Spring Boot respeitando as variáveis de ambiente
ENTRYPOINT ["java", "-jar", "app.jar"]
