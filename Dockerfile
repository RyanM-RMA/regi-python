FROM eclipse-temurin:21-jdk AS build
WORKDIR /home/gradle/project

# 1. Copy Gradle wrapper and configuration files first.
# This includes the 'gradle' directory with the wrapper and libs.versions.toml
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

COPY regi-headless/build.gradle regi-headless/
COPY buildSrc buildSrc

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# 2. Pre-download dependencies.
# This layer is cached until you change your version catalog or build scripts.
RUN ./gradlew :regi-headless:dependencies --no-daemon

# 3. Copy the actual source code and build the distribution
COPY . .
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew :regi-headless:installDist --no-daemon

# Download the OpenTelemetry Java Agent
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /home/gradle/project/opentelemetry-javaagent.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the application and the OTel agent
COPY --from=build /home/gradle/project/regi-headless/build/install/regi-headless ./
COPY --from=build /home/gradle/project/opentelemetry-javaagent.jar ./

# AWS Batch/Lambda often prefer non-root, and it's better for OTel file permissions
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN chown -R appuser:appgroup /app
USER appuser

RUN chmod +x /app/bin/regi-headless

ENTRYPOINT ["/app/bin/regi-headless"]
