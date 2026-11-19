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

# Detect HiDPI scaling on Linux so Swing text and forms render large and sharp
UI_SCALE_OPT=""
if [ -n "${CAMPUSFIX_SCALE:-}" ]; then
    UI_SCALE_OPT="-Dsun.java2d.uiScale=${CAMPUSFIX_SCALE}"
elif [ -n "${GDK_SCALE:-}" ] && [ "${GDK_SCALE}" != "1" ]; then
    UI_SCALE_OPT="-Dsun.java2d.uiScale=${GDK_SCALE}"
else
    DETECTED_SCALE=""
    # 1. Check Xft.dpi if xrdb is available
    if command -v xrdb >/dev/null 2>&1; then
        XFT_DPI="$(xrdb -query 2>/dev/null | awk '/Xft\.dpi:/ {print $2; exit}')"
        if [ -n "$XFT_DPI" ]; then
            DETECTED_SCALE="$(awk -v dpi="$XFT_DPI" 'BEGIN { if (dpi >= 115) printf "%.2f", dpi/96.0 }')"
        fi
    fi
    # 2. Fallback: check primary display resolution via xrandr
    if [ -z "$DETECTED_SCALE" ] && command -v xrandr >/dev/null 2>&1; then
        RES_W="$(xrandr --current 2>/dev/null | awk '/\*/ {split($1,a,"x"); print a[1]; exit}')"
        if [ -n "$RES_W" ]; then
            if [ "$RES_W" -ge 3400 ]; then
                DETECTED_SCALE="2.0"
            elif [ "$RES_W" -ge 2400 ]; then
                DETECTED_SCALE="1.6"
            elif [ "$RES_W" -ge 2000 ]; then
                DETECTED_SCALE="1.35"
            fi
        fi
    fi
    if [ -n "$DETECTED_SCALE" ]; then
        UI_SCALE_OPT="-Dsun.java2d.uiScale=${DETECTED_SCALE}"
    fi
fi

echo "Launching CampusFix – Smart Campus Maintenance and Complaint Management System..."
if [ -n "$UI_SCALE_OPT" ]; then
    $JAVA_CMD -Dsun.java2d.uiScale.enabled=true "$UI_SCALE_OPT" -cp out edu.nie.campusfix.Main "$@"
else
    $JAVA_CMD -Dsun.java2d.uiScale.enabled=true -cp out edu.nie.campusfix.Main "$@"
fi
