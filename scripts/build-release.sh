#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_JAVA_HOME="$HOME/.local/share/mise/installs/java/17.0.2"

java_major() {
  java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p'
}

if command -v java >/dev/null 2>&1; then
  CURRENT_JAVA_MAJOR="$(java_major)"
else
  CURRENT_JAVA_MAJOR=""
fi

if [[ "$CURRENT_JAVA_MAJOR" != "17" && -d "$DEFAULT_JAVA_HOME" ]]; then
  export JAVA_HOME="$DEFAULT_JAVA_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: java is not available on PATH. Install JDK 17 and set JAVA_HOME." >&2
  exit 1
fi

JAVA_MAJOR="$(java_major)"
if [[ -z "$JAVA_MAJOR" ]]; then
  echo "ERROR: Unable to detect Java version." >&2
  exit 1
fi

if (( JAVA_MAJOR != 17 )); then
  echo "ERROR: Android release builds require JDK 17. Current major version: $JAVA_MAJOR" >&2
  exit 1
fi

cd "$ROOT_DIR"
./gradlew clean assembleRelease bundleRelease

echo "\nRelease artifacts are available under:"
echo "  app/build/outputs/apk/"
echo "  app/build/outputs/bundle/"
