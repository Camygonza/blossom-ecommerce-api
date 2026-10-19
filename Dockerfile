# Etapa 1: compila el jar (las dependencias se cachean mientras no cambie el pom)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# Etapa 2: solo el JRE y el jar, sin root
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 bloom
WORKDIR /app
COPY --from=build /workspace/target/ecommerce-api-*.jar app.jar
USER bloom
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
