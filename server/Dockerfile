FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradlew ./
COPY gradle/ gradle/
COPY buildSrc/ buildSrc/
COPY common/ common/
COPY server/ server/
# Remove modules that are not needed
RUN printf 'rootProject.name = "seriput"\ninclude("common", "server")\n' > settings.gradle.kts
# Override common module's Java 17 toolchain to use the available JDK 25
RUN sed -i 's/JavaLanguageVersion.of(17)/JavaLanguageVersion.of(25)/' common/build.gradle.kts
RUN chmod +x gradlew && ./gradlew :server:installDist --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/server/build/install/server/ ./
ARG SERIPUT_PORT=8080
ENV SERIPUT_PORT=${SERIPUT_PORT}
EXPOSE ${SERIPUT_PORT}
ENTRYPOINT ["bin/server"]
