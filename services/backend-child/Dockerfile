FROM gradle:8.10.2-jdk21-alpine AS build
WORKDIR /workspace
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle --no-daemon bootJar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
COPY Bdd ./Bdd
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
