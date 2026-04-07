FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]