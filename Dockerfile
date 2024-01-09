# syntax=docker/dockerfile:1

FROM openjdk:8-jdk-alpine

COPY .mvn/ .mvn
COPY mvnw pom.xml ./

COPY src ./src

CMD ["./mvnw", "spring-boot:run"]