
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 10001 appuser
COPY --from=build /app/target/whatsapp-chatbot-integration-0.0.1-SNAPSHOT.jar app.jar

USER 10001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
