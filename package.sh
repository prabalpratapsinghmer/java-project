#!/usr/bin/env bash
set -euo pipefail

VERSION="1.0.0"
APP_NAME="CampusFix"
JAR_NAME="${APP_NAME}-${VERSION}.jar"
ZIP_NAME="${APP_NAME}-${VERSION}-Portable.zip"
DIST_DIR="dist"

# Locate JDK tools (jar, java, jpackage)
if command -v jar >/dev/null 2>&1; then
    JAR_CMD="jar"
elif [ -x "$HOME/.jdk/current/bin/jar" ]; then
    JAR_CMD="$HOME/.jdk/current/bin/jar"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/jar" ]; then
    JAR_CMD="$JAVA_HOME/bin/jar"
else
    echo "Error: jar tool not found. Please install JDK 17+ or set JAVA_HOME." >&2
    exit 1
fi

if [ -x "$HOME/.jdk/current/bin/java" ]; then
    JAVA_CMD="$HOME/.jdk/current/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA_CMD="java"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
else
    echo "Error: java runtime not found." >&2
    exit 1
fi

JPACKAGE_CMD=""
if [ -x "$HOME/.jdk/current/bin/jpackage" ]; then
    JPACKAGE_CMD="$HOME/.jdk/current/bin/jpackage"
elif command -v jpackage >/dev/null 2>&1; then
    JPACKAGE_CMD="jpackage"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/jpackage" ]; then
    JPACKAGE_CMD="$JAVA_HOME/bin/jpackage"
fi

echo "=================================================="
echo "Packaging ${APP_NAME} v${VERSION}"
echo "=================================================="

# 1. Clean compile & run full 30-test gate
./compile.sh
"$JAVA_CMD" -cp out edu.nie.campusfix.tests.TestRunner

# 2. Prepare clean dist directory
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR/stage" "$DIST_DIR/portable/${APP_NAME}-${VERSION}"

# 3. Build cross-platform runnable JAR (Windows, macOS, Linux)
echo "Building ${DIST_DIR}/${JAR_NAME}..."
"$JAR_CMD" --create \
    --file "${DIST_DIR}/${JAR_NAME}" \
    --main-class edu.nie.campusfix.Main \
    -C out .

# Verify JAR boots cleanly in headless mode
"$JAVA_CMD" -jar "${DIST_DIR}/${JAR_NAME}" --headless >/dev/null
echo "Verified executable JAR: ${DIST_DIR}/${JAR_NAME}"

# 4. Build Cross-Platform Portable Bundle (Windows + macOS + Linux)
PORTABLE_DIR="${DIST_DIR}/portable/${APP_NAME}-${VERSION}"
cp "${DIST_DIR}/${JAR_NAME}" "${PORTABLE_DIR}/${JAR_NAME}"

cat > "${PORTABLE_DIR}/Launch-Windows.bat" << 'EOF'
@echo off
title CampusFix - Smart Campus Maintenance System
where javaw >nul 2>&1
if %errorlevel%==0 (
    start "" javaw -Dsun.java2d.uiScale.enabled=true -jar "%~dp0CampusFix-1.0.0.jar"
) else (
    java -Dsun.java2d.uiScale.enabled=true -jar "%~dp0CampusFix-1.0.0.jar"
    if %errorlevel% neq 0 (
        echo.
        echo Java 17 or higher is required to run CampusFix.
        echo Download OpenJDK 17+ from https://adoptium.net/
        pause
    )
)
EOF

cat > "${PORTABLE_DIR}/Launch-macOS.command" << 'EOF'
#!/usr/bin/env bash
cd "$(dirname "$0")"
java -Dsun.java2d.uiScale.enabled=true -jar "CampusFix-1.0.0.jar"
EOF
chmod +x "${PORTABLE_DIR}/Launch-macOS.command"

cat > "${PORTABLE_DIR}/Launch-Linux.sh" << 'EOF'
#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

UI_SCALE_OPT=""
if [ -n "${CAMPUSFIX_SCALE:-}" ]; then
    UI_SCALE_OPT="-Dsun.java2d.uiScale=${CAMPUSFIX_SCALE}"
elif [ -n "${GDK_SCALE:-}" ] && [ "${GDK_SCALE}" != "1" ]; then
    UI_SCALE_OPT="-Dsun.java2d.uiScale=${GDK_SCALE}"
elif command -v xrandr >/dev/null 2>&1; then
    RES_W="$(xrandr --current 2>/dev/null | awk '/\*/ {split($1,a,"x"); print a[1]; exit}')"
    if [ -n "$RES_W" ]; then
        if [ "$RES_W" -ge 3400 ]; then UI_SCALE_OPT="-Dsun.java2d.uiScale=2.0"
        elif [ "$RES_W" -ge 2400 ]; then UI_SCALE_OPT="-Dsun.java2d.uiScale=1.6"
        elif [ "$RES_W" -ge 2000 ]; then UI_SCALE_OPT="-Dsun.java2d.uiScale=1.35"
        fi
    fi
fi

if [ -n "$UI_SCALE_OPT" ]; then
    exec java -Dsun.java2d.uiScale.enabled=true "$UI_SCALE_OPT" -jar "CampusFix-1.0.0.jar" "$@"
else
    exec java -Dsun.java2d.uiScale.enabled=true -jar "CampusFix-1.0.0.jar" "$@"
fi
EOF
chmod +x "${PORTABLE_DIR}/Launch-Linux.sh"

cat > "${PORTABLE_DIR}/QUICKSTART.txt" << 'EOF'
CampusFix v1.0.0 - Smart Campus Maintenance & Complaint Management System
=========================================================================

Prerequisite: Java 17 or higher (https://adoptium.net/)

Windows:
  Double-click "Launch-Windows.bat"
  (or run: java -jar CampusFix-1.0.0.jar)

macOS:
  Double-click "Launch-macOS.command"
  (or run in Terminal: java -jar CampusFix-1.0.0.jar)

Linux:
  Run "./Launch-Linux.sh"
  (or run: java -jar CampusFix-1.0.0.jar)

First Launch:
  Configure your Initial Administrator account on the welcome screen,
  then create technician accounts and register student/staff users.
EOF

echo "Building ${DIST_DIR}/${ZIP_NAME}..."
if command -v zip >/dev/null 2>&1; then
    (cd "${DIST_DIR}/portable" && zip -qr "../${ZIP_NAME}" "${APP_NAME}-${VERSION}")
else
    "$JAR_CMD" -cMf "${DIST_DIR}/${ZIP_NAME}" -C "${DIST_DIR}/portable" "${APP_NAME}-${VERSION}"
fi

# 5. Build native Debian/Ubuntu/Kali .deb installer via jpackage (if available on Linux)
if [ -n "$JPACKAGE_CMD" ] && command -v dpkg-deb >/dev/null 2>&1; then
    echo "Building native Linux .deb package with $JPACKAGE_CMD..."
    cp "${DIST_DIR}/${JAR_NAME}" "${DIST_DIR}/stage/${JAR_NAME}"
    "$JPACKAGE_CMD" \
        --type deb \
        --name campusfix \
        --app-version "${VERSION}" \
        --vendor "NIE CSE Team" \
        --description "CampusFix - Smart Campus Maintenance and Complaint Management System" \
        --input "${DIST_DIR}/stage" \
        --main-jar "${JAR_NAME}" \
        --main-class edu.nie.campusfix.Main \
        --dest "${DIST_DIR}" \
        --linux-shortcut \
        --linux-menu-group "Education;Utility" \
        --java-options "-Dsun.java2d.uiScale.enabled=true"
else
    echo "Skipping .deb build (jpackage or dpkg-deb not available on this host)."
fi

# Clean staging dirs and generate SHA-256 checksums
rm -rf "${DIST_DIR}/stage" "${DIST_DIR}/portable"
(cd "${DIST_DIR}" && sha256sum * > SHA256SUMS.txt)

echo "=================================================="
echo "Release artifacts generated in ${DIST_DIR}/:"
ls -lh "${DIST_DIR}"
echo "=================================================="
