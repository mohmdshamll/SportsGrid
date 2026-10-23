#!/usr/bin/env bash
# ==============================================================================
# SportGrid macOS Packaging Script
# Builds the Java Swing desktop application and packages it into a native
# macOS application bundle: dist/SportGrid.app
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== SportGrid macOS Application Packager ==="

# 1. Environment and Tool Checks
if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "ERROR: This packaging script must be run on macOS." >&2
    exit 1
fi

JPACKAGE_BIN="${JAVA_HOME:-}/bin/jpackage"
if [[ ! -x "$JPACKAGE_BIN" ]]; then
    JPACKAGE_BIN="$(command -v jpackage || true)"
fi

if [[ -z "$JPACKAGE_BIN" || ! -x "$JPACKAGE_BIN" ]]; then
    echo "ERROR: 'jpackage' tool not found. Please install JDK 17+ (JDK 21 LTS recommended) and ensure JAVA_HOME is set." >&2
    exit 1
fi

echo "Using jpackage: $JPACKAGE_BIN"

# 2. Build desktop JAR using Maven Wrapper
echo "Building SportGrid desktop artifacts with Maven..."
./mvnw clean package -DskipTests

DESKTOP_JAR="target/sportgrid-1.0.0.jar"
if [[ ! -f "$DESKTOP_JAR" ]]; then
    echo "ERROR: Expected desktop jar '$DESKTOP_JAR' was not generated." >&2
    exit 1
fi

# 3. Verify MySQL JDBC driver
MYSQL_DRIVER="lib/mysql-connector-j-8.4.0.jar"
if [[ ! -f "$MYSQL_DRIVER" ]]; then
    MYSQL_DRIVER="lib/mysql-connector-j-26.10.0.jar"
fi

if [[ ! -f "$MYSQL_DRIVER" ]]; then
    echo "ERROR: MySQL JDBC connector jar not found in lib/." >&2
    exit 1
fi
echo "Using MySQL Driver: $MYSQL_DRIVER"

# 4. Icon verification or regeneration
ICON_PATH="resources/macos/SportGrid.icns"
if [[ ! -f "$ICON_PATH" ]]; then
    echo "Warning: Icon not found at $ICON_PATH. Proceeding without custom icon."
    ICON_FLAG=()
else
    ICON_FLAG=(--icon "$ICON_PATH")
fi

# 5. Prepare jpackage input staging directory
INPUT_DIR="target/package-input"
rm -rf "$INPUT_DIR"
mkdir -p "$INPUT_DIR"
cp "$DESKTOP_JAR" "$INPUT_DIR/"
cp "$MYSQL_DRIVER" "$INPUT_DIR/"

# 6. Build macOS Application Image (.app)
OUTPUT_DIR="dist"
rm -rf "$OUTPUT_DIR/SportGrid.app"
mkdir -p "$OUTPUT_DIR"

echo "Invoking jpackage to create SportGrid.app..."
"$JPACKAGE_BIN" \
    --type app-image \
    --name "SportGrid" \
    --app-version "1.0.0" \
    --input "$INPUT_DIR" \
    --main-jar "$(basename "$DESKTOP_JAR")" \
    --main-class "main.Main" \
    "${ICON_FLAG[@]}" \
    --dest "$OUTPUT_DIR" \
    --mac-package-name "SportGrid" \
    --mac-package-identifier "com.sportgrid.desktop" \
    --add-modules "java.base,java.desktop,java.sql,java.naming,java.management,java.security.sasl,java.prefs,java.logging,java.xml" \
    --java-options "-Dapple.awt.application.name=SportGrid" \
    --java-options "-Dapple.laf.useScreenMenuBar=true"

# 7. Verification
if [[ -d "$OUTPUT_DIR/SportGrid.app" ]]; then
    echo ""
    echo "================================================================="
    echo "SUCCESS: macOS application bundle created successfully!"
    echo "Location: $SCRIPT_DIR/$OUTPUT_DIR/SportGrid.app"
    echo "To launch from Terminal: open \"$OUTPUT_DIR/SportGrid.app\""
    echo "To install: Drag and drop SportGrid.app into /Applications"
    echo "================================================================="
else
    echo "ERROR: Failed to generate $OUTPUT_DIR/SportGrid.app." >&2
    exit 1
fi
