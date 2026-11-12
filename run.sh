#!/usr/bin/env bash
set -e

# CampusFix Application Launcher
# Locate JRE/JDK
if command -v java >/dev/null 2>&1; then
    JAVA_CMD="java"
elif [ -x "$HOME/.jdk/current/bin/java" ]; then
    JAVA_CMD="$HOME/.jdk/current/bin/java"
elif [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
else
    echo "Error: java runtime not found. Please install JRE/JDK 17+ or set JAVA_HOME." >&2
    exit 1
fi

if [ ! -d "out" ] || [ -z "$(find out -name "*.class" 2>/dev/null)" ]; then
    echo "Compiled classes not found. Running compile.sh first..."
    ./compile.sh
fi

echo "Launching CampusFix – Smart Campus Maintenance and Complaint Management System..."
$JAVA_CMD -cp out edu.nie.campusfix.Main "$@"
