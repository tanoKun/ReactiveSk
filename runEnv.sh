#!/usr/bin/env bash
set -euo pipefail

# runEnv.sh - generic runner
# Usage: ./runEnv.sh [--server NAME.jar] [--skript Skript-vX.Y.Z.jar] [--reactive ReactiveSk-dir-or-name] [--java /path/to/java] [--no-start] [--detach] [--stop]

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
TEST_DIR="$ROOT_DIR/test"
SERVERS_DIR="$TEST_DIR/servers"
SKRIPT_PLUGINS_DIR="$TEST_DIR/skript-plugins"
SCRIPTS_DIR="$TEST_DIR/scripts"
DEFAULT_REACTIVE_DIR="$ROOT_DIR/ReactiveSk-skript-v2_6_3/build/libs"

SERVER_DEFAULT="1.12.2.jar"
SKRIPT_DEFAULT="Skript-v2.6.3.jar"
JAVA_DEFAULT="java"
NO_START=false
REACTIVE_ARG=
DETACH=false
STOP=false

PID_FILE=""

# parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    --server)
      SERVER_ARG="$2"; shift 2;;
    --skript)
      SKRIPT_ARG="$2"; shift 2;;
    --reactive)
      REACTIVE_ARG="$2"; shift 2;;
    --java)
      JAVA_ARG="$2"; shift 2;;
    --no-start|--noStart)
      NO_START=true; shift;;
    --detach)
      DETACH=true; shift;;
    --stop)
      STOP=true; shift;;
    --help|-h)
      echo "Usage: $0 [--server NAME.jar] [--skript Skript-vX.Y.Z.jar] [--reactive ReactiveSk-dir-or-name] [--java /path/to/java] [--no-start] [--detach] [--stop]"; exit 0;;
    *)
      echo "Unknown arg: $1"; exit 1;;
  esac
done

SERVER_NAME=${SERVER_ARG:-$SERVER_DEFAULT}
SKRIPT_NAME=${SKRIPT_ARG:-$SKRIPT_DEFAULT}
JAVA_EXEC=${JAVA_ARG:-$JAVA_DEFAULT}

# resolve reactive directory
if [[ -n "${REACTIVE_ARG:-}" ]]; then
  REACTIVE_DIR="$REACTIVE_ARG/build/libs"
else
  REACTIVE_DIR="$DEFAULT_REACTIVE_DIR"
fi

SERVER_BASENAME="${SERVER_NAME%.jar}"
SERVER_DIR="$SERVERS_DIR/$SERVER_BASENAME"
PLUGINS_DIR="$SERVER_DIR/plugins"
SKRIPT_DEST_DIR="$PLUGINS_DIR/Skript/scripts"
PID_FILE="$SERVER_DIR/server.pid"

# initialize plugins dir: remove existing plugins to start clean
if [ -d "$PLUGINS_DIR" ]; then
  rm -rf "$PLUGINS_DIR"
fi

# recreate server and plugins directories (skript dest will be created under plugins)
mkdir -p "$SERVER_DIR" "$PLUGINS_DIR" "$SKRIPT_DEST_DIR"

# copy server jar (strict)
if [ -f "$SERVERS_DIR/$SERVER_NAME" ]; then
  cp -f "$SERVERS_DIR/$SERVER_NAME" "$SERVER_DIR/server.jar"
fi

# write eula in ASCII
printf "eula=true\n" > "$SERVER_DIR/eula.txt"

# copy ReactiveSk plugin if exists
if [ -d "$REACTIVE_DIR" ]; then
  found=$(find "$REACTIVE_DIR" -maxdepth 1 -type f -name "*.jar" | head -n 1 || true)
  if [ -n "$found" ]; then
    cp -f "$found" "$PLUGINS_DIR/"
  fi
fi

# copy specified skript jar (strict)
if [ -f "$SKRIPT_PLUGINS_DIR/$SKRIPT_NAME" ]; then
  cp -f "$SKRIPT_PLUGINS_DIR/$SKRIPT_NAME" "$PLUGINS_DIR/"
fi

# copy .sk files
shopt -s nullglob
for f in "$SCRIPTS_DIR"/*.sk; do
  cp -f "$f" "$SKRIPT_DEST_DIR/"
done
shopt -u nullglob

if [ "$STOP" = true ]; then
  if [ -f "$PID_FILE" ]; then
    pid=$(head -n 1 "$PID_FILE" || true)
    if [ -n "$pid" ]; then
      echo "Stopping process id $pid"
      if kill "$pid" >/dev/null 2>&1; then
        echo "Sent TERM to $pid"
      else
        echo "Failed to send TERM to $pid, trying KILL"
        kill -9 "$pid" >/dev/null 2>&1 || true
      fi
    fi
    rm -f "$PID_FILE"
  else
    echo "PID file not found: $PID_FILE"
  fi
  exit 0
fi

if [ "$NO_START" = true ]; then
  echo "Files prepared under: $SERVER_DIR (no start)"
  exit 0
fi

# start server in foreground (original behaviour)
if [ "$DETACH" = true ]; then
  echo "Starting server (detached) in $SERVER_DIR using java: $JAVA_EXEC"
  if command -v "$JAVA_EXEC" >/dev/null 2>&1; then
    # change working directory to server dir so server reads eula.txt from correct location
    cd "$SERVER_DIR"
    # start with nohup and redirect output to server.log
    nohup "$JAVA_EXEC" -jar "server.jar" nogui > "$SERVER_DIR/server.log" 2>&1 &
    pid=$!
    echo "$pid" > "$PID_FILE"
    disown || true
    echo "Wrote PID $pid to $PID_FILE"
    exit 0
  else
    echo "java executable not found: $JAVA_EXEC" >&2
    exit 1
  fi
fi

# original foreground exec
if command -v "$JAVA_EXEC" >/dev/null 2>&1; then
  cd "$SERVER_DIR"
  exec "$JAVA_EXEC" -jar "server.jar" nogui
else
  echo "java executable not found: $JAVA_EXEC" >&2
  exit 1
fi
