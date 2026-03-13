FROM eclipse-temurin:17-jre-alpine

ARG JAR_FILE=target/promption-gateway-0.0.1-SNAPSHOT.jar

WORKDIR /app

COPY ${JAR_FILE} app.jar

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

