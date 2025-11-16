FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
RUN apk add --no-cache bash maven
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/Event-Feedback-Analyzer-0.0.1-SNAPSHOT.jar"]
