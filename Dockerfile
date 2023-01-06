# syntax=docker/dockerfile:experimental
FROM --platform=linux/amd64 gradle:jdk17-alpine AS gradle
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.gradle gradle bootJar --debug --stacktrace

FROM --platform=linux/amd64 openjdk:17 as runtime
WORKDIR /app

EXPOSE 8080

COPY --from=gradle /app/build/libs/*.jar /app/
RUN chown -R 1000:1000 /app
USER 1000:1000

ENTRYPOINT ["java", "-jar", "arctgkolbasy-0.0.1-SNAPSHOT.jar"]
