#!/bin/sh
# Local Maven bootstrap: downloads Apache Maven into .tools/ (gitignored).
set -e
ROOT="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
MVN_VERSION="3.9.11"
INSTALL_DIR="$ROOT/.tools/apache-maven-$MVN_VERSION"
MVN_BIN="$INSTALL_DIR/bin/mvn"
URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MVN_VERSION}/apache-maven-${MVN_VERSION}-bin.tar.gz"

if [ ! -x "$MVN_BIN" ]; then
  mkdir -p "$ROOT/.tools"
  ARCHIVE="$ROOT/.tools/apache-maven-${MVN_VERSION}-bin.tar.gz"
  if [ ! -f "$ARCHIVE" ]; then
    echo "Downloading Maven $MVN_VERSION..." >&2
    curl -fsSL -o "$ARCHIVE" "$URL"
  fi
  tar -xzf "$ARCHIVE" -C "$ROOT/.tools"
fi

resolve_java_home() {
  if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    printf '%s\n' "$JAVA_HOME"
    return 0
  fi

  # macOS: Apple's java_home helper (only sees JDKs registered with the OS).
  if [ -x /usr/libexec/java_home ]; then
    detected="$(/usr/libexec/java_home 2>/dev/null || true)"
    if [ -n "$detected" ] && [ -x "$detected/bin/java" ]; then
      printf '%s\n' "$detected"
      return 0
    fi
  fi

  # java on PATH: follow the binary up to its JDK home.
  if command -v java >/dev/null 2>&1; then
    java_bin="$(command -v java)"
    if [ -L "$java_bin" ]; then
      if command -v realpath >/dev/null 2>&1; then
        java_bin="$(realpath "$java_bin")"
      elif command -v readlink >/dev/null 2>&1; then
        java_bin="$(readlink -f "$java_bin" 2>/dev/null || readlink "$java_bin")"
      fi
    fi
    java_home="$(CDPATH= cd -- "$(dirname "$java_bin")/.." && pwd)"
    if [ -x "$java_home/bin/java" ]; then
      printf '%s\n' "$java_home"
      return 0
    fi
    # Homebrew shims often live at .../openjdk.jdk/Contents/Home/bin/java
    if [ -x "$java_home/Home/bin/java" ]; then
      printf '%s\n' "$java_home/Home"
      return 0
    fi
  fi

  # Common install locations (Homebrew, Linux distros).
  for candidate in \
      /opt/homebrew/opt/openjdk \
      /opt/homebrew/opt/java \
      /usr/local/opt/openjdk \
      /usr/local/opt/java \
      /usr/lib/jvm/default-java
  do
    if [ -x "$candidate/bin/java" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  # Newest Homebrew Cellar OpenJDK, if the opt symlink is missing.
  for cellar in /opt/homebrew/Cellar/openjdk /usr/local/Cellar/openjdk; do
    if [ -d "$cellar" ]; then
      latest="$(ls -1d "$cellar"/*/libexec/openjdk.jdk/Contents/Home 2>/dev/null | tail -n 1 || true)"
      if [ -n "$latest" ] && [ -x "$latest/bin/java" ]; then
        printf '%s\n' "$latest"
        return 0
      fi
    fi
  done

  return 1
}

JAVA_HOME="$(resolve_java_home)" || {
  echo "error: could not find a JDK. Install Java 17+ or set JAVA_HOME." >&2
  exit 1
}
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

# Maven 3.9 still uses Jansi (JNI), Guava/Guice (sun.misc.Unsafe), and Sisu
# (reflective final-field writes). JDK 24+ warns; JDK 26 will start blocking
# those calls. These flags are Maven's, not j-mod's. Probe first so JDK 17
# still launches (unrecognized options would abort).
append_maven_opt() {
  flag="$1"
  if "$JAVA_HOME/bin/java" "$flag" -version >/dev/null 2>&1; then
    case " ${MAVEN_OPTS:-} " in
      *" $flag "*) ;;
      *) MAVEN_OPTS="$flag${MAVEN_OPTS:+ $MAVEN_OPTS}" ;;
    esac
  fi
}
append_maven_opt "--enable-native-access=ALL-UNNAMED"
append_maven_opt "--sun-misc-unsafe-memory-access=allow"
append_maven_opt "--enable-final-field-mutation=ALL-UNNAMED"
export MAVEN_OPTS

# Bare `./mvnw` (or only CLI flags like -q) has no Maven goal; default to package.
has_goal=false
for arg in "$@"; do
  case "$arg" in
    -*) ;;
    *) has_goal=true; break ;;
  esac
done
if [ "$has_goal" = false ]; then
  set -- "$@" package
fi

exec "$MVN_BIN" "$@"
