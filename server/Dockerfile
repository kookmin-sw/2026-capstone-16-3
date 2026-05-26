FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]