#!/usr/bin/env bash
set -e

# CampusFix Build Script
# Locate JDK
if command -v javac >/dev/null 2>&1; then
    JAVAC_CMD="javac"
elif [ -x "$HOME/.jdk/current/bin/javac" ]; then
    JAVAC_CMD="$HOME/.jdk/current/bin/javac"
elif [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC_CMD="$JAVA_HOME/bin/javac"
else
    echo "Error: javac not found. Please install JDK 17+ or set JAVA_HOME." >&2
    exit 1
fi

echo "Using compiler: $($JAVAC_CMD -version 2>&1)"
mkdir -p out
mkdir -p data/config data/users data/complaints reports

echo "Compiling CampusFix sources..."
$JAVAC_CMD -encoding UTF-8 -Xlint:all -d out $(find src -name "*.java")

echo "Build successful! Classes written to out/"
