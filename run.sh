#!/usr/bin/env sh
# Сборка (при изменении исходников) и запуск depviz.
# Все аргументы передаются программе: ./run.sh --package Newtonsoft.Json --package-version 13.0.3
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$DIR/target/depviz.jar"

if [ ! -f "$JAR" ] || [ -n "$(find "$DIR/src" "$DIR/pom.xml" -newer "$JAR" 2>/dev/null)" ]; then
    mvn -q -f "$DIR/pom.xml" package -DskipTests
fi

exec java -jar "$JAR" "$@"
