#!/bin/sh
set -eu
VERSION=3.9.11
BASE="${HOME}/.m2/wrapper/dists/apache-maven-${VERSION}"
if [ ! -x "${BASE}/bin/mvn" ]; then
  mkdir -p "${BASE}"
  TMP="$(mktemp -d)"
  trap 'rm -rf "$TMP"' EXIT
  curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${VERSION}/binaries/apache-maven-${VERSION}-bin.tar.gz" -o "${TMP}/maven.tar.gz"
  tar -xzf "${TMP}/maven.tar.gz" -C "${TMP}"
  cp -R "${TMP}/apache-maven-${VERSION}/." "${BASE}/"
fi
exec "${BASE}/bin/mvn" "-Dmaven.repo.local=${PWD}/.m2/repository" "$@"
