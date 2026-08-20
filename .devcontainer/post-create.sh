#!/usr/bin/env bash
set -euo pipefail

# The Temurin feature is installed after the Dockerfile is built. Import the
# corporate CAs into the active Temurin truststore as well.
for crt in /usr/local/share/ca-certificates/corporate/*.crt; do
	[[ -f "$crt" ]] || continue
	alias="corp-$(basename "$crt" .crt | tr '[:upper:] ' '[:lower:]-')"
	sudo "$JAVA_HOME/bin/keytool" -importcert -noprompt -trustcacerts \
		-alias "$alias" -file "$crt" \
		-keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit \
		>/dev/null 2>&1 || true
done

./mvnw -B -ntp -q dependency:go-offline || true
