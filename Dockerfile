FROM maven:3.9.12-eclipse-temurin-25 AS build
ARG SERVICE_MODULE
WORKDIR /workspace
COPY pom.xml .
COPY services ./services
RUN mvn -B -pl services/${SERVICE_MODULE} -am package -DskipTests

FROM eclipse-temurin:25-jre
ARG SERVICE_MODULE
WORKDIR /app
RUN apt-get update \
    && apt-get install --no-install-recommends --yes curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/services/${SERVICE_MODULE}/target/${SERVICE_MODULE}-0.1.0-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
