FROM gradle:9.4.1-jdk17 AS build

WORKDIR /workspace
COPY . .
RUN gradle --no-daemon :backend:api:installDist

FROM eclipse-temurin:17-jre

ARG DENO_VERSION=2.9.3

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl unzip poppler-utils tesseract-ocr tesseract-ocr-eng tesseract-ocr-tur python3 python3-pip \
    && curl --fail --location --silent --show-error \
        --output /tmp/deno.zip \
        "https://github.com/denoland/deno/releases/download/v${DENO_VERSION}/deno-x86_64-unknown-linux-gnu.zip" \
    && unzip -q /tmp/deno.zip -d /usr/local/bin \
    && deno --version \
    && pip3 install --no-cache-dir --break-system-packages 'yt-dlp[default]' \
    && rm -f /tmp/deno.zip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /workspace/backend/api/build/install/api/ ./

RUN useradd --create-home --uid 10001 appuser
USER appuser

ENV PORT=8080
EXPOSE 8080

CMD ["./bin/api"]
