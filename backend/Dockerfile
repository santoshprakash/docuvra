FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl libreoffice imagemagick font-dejavu font-liberation

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENV DOCUVRA_STORAGE_BASE_PATH=/app/uploads

ENTRYPOINT ["java", "-jar", "app.jar"]
