# Multi-stage production container for MetaShield Enclave
# Integrates Java Spring Boot, Python 3 Sanitization Core, and C++ Stream Guard

# --- STAGE 1: Compile C++ Stream Guard ---
FROM gcc:12 AS cpp-builder
WORKDIR /build
COPY engine/stream_guard.cpp .
RUN g++ -O2 -static -std=c++11 stream_guard.cpp -o stream_guard

# --- STAGE 2: Java & Python Unified Production Runtime ---
FROM eclipse-temurin:21-jre-jammy

# Install Python 3 and system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    python3-venv \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Set up Python virtual environment and install metadata engines
RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

RUN pip install --no-cache-dir \
    pillow \
    pypdf \
    python-docx \
    lxml

# Copy compiled C++ binary from Stage 1
COPY --from=cpp-builder /build/stream_guard /app/engine/stream_guard
RUN chmod +x /app/engine/stream_guard

# Copy Python engine scripts
COPY engine /app/engine

# Copy application directories
RUN mkdir -p /app/data /app/temp/staging/uploads /app/temp/staging/cleaned

# Copy packaged Spring Boot JAR
COPY target/metashield-core-1.0.0.jar /app/app.jar

# Security: Non-root execution
RUN useradd -u 10001 metashield && \
    chown -R metashield:metashield /app
USER metashield

# Expose Spring Boot web port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f http://localhost:8080/api/files || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
