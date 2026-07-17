FROM gradle:9.4.1-jdk17 AS build

WORKDIR /workspace
COPY . .
RUN gradle --no-daemon :backend:api:installDist

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /workspace/backend/api/build/install/api/ ./

RUN useradd --create-home --uid 10001 appuser
USER appuser

ENV PORT=8080
EXPOSE 8080

CMD ["./bin/api"]

