# syntax=docker/dockerfile:1.7
ARG BASE_IMAGE=mcr.microsoft.com/devcontainers/java:1-25-bookworm
FROM ${BASE_IMAGE}

# --- Corporate CA trust ------------------------------------------------------
# Populate ./certs/ on the host before building (see .devcontainer/README.md).
# All *.pem/*.crt files copied here are installed into the system trust store
# so that apt/curl/npm/mvn behind a TLS-inspecting proxy work.
USER root
COPY certs/ /usr/local/share/ca-certificates/corporate/
RUN set -eux; \
    # Remove third-party APT sources that may fail signature verification behind
    # the corporate proxy — they are re-added later by devcontainer features if needed
    rm -f /etc/apt/sources.list.d/yarn.list /etc/apt/sources.list.d/nodesource.list; \
    # Normalize extensions (Debian expects *.crt PEM files here)
    find /usr/local/share/ca-certificates/corporate/ -type f \
        \( -name '*.pem' -o -name '*.cer' \) \
        -exec sh -c 'mv "$1" "${1%.*}.crt"' _ {} \; ; \
    # ca-certificates is already installed in the MS base image; refresh trust
    update-ca-certificates

# Also seed common toolchains
ENV NODE_EXTRA_CA_CERTS=/etc/ssl/certs/ca-certificates.crt
ENV REQUESTS_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt
ENV SSL_CERT_FILE=/etc/ssl/certs/ca-certificates.crt

# Trust the same CAs inside the JVM cacerts (used by mvn/gradle)
RUN set -eux; \
    JAVA_HOME_DIR="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"; \
    CACERTS="${JAVA_HOME_DIR}/lib/security/cacerts"; \
    for crt in /usr/local/share/ca-certificates/corporate/*.crt; do \
        alias="corp-$(basename "$crt" .crt | tr '[:upper:] ' '[:lower:]-')"; \
        keytool -importcert -noprompt -trustcacerts \
            -alias "$alias" -file "$crt" \
            -keystore "$CACERTS" -storepass changeit >/dev/null 2>&1 || true; \
    done; \
    echo "Imported corporate CAs into ${CACERTS}"

USER vscode
