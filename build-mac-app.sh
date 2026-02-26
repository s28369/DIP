#!/bin/bash
set -e

echo "=== Budowanie Fleet Management .dmg dla macOS ==="

# 1. Zbuduj fat JAR
echo "[1/3] Kompilacja projektu..."
mvn clean package -DskipTests -q

# 2. Skopiuj JavaFX JARy dla arm64 do osobnego katalogu
echo "[2/3] Przygotowanie modułów JavaFX..."
M2="$HOME/.m2/repository/org/openjfx"
FX_VER="21.0.1"
C="mac-aarch64"
MODS="target/javafx-mods"
INPUT="target/app-input"

mkdir -p "$MODS" "$INPUT"

cp "$M2/javafx-base/$FX_VER/javafx-base-$FX_VER-$C.jar"       "$MODS/"
cp "$M2/javafx-graphics/$FX_VER/javafx-graphics-$FX_VER-$C.jar" "$MODS/"
cp "$M2/javafx-controls/$FX_VER/javafx-controls-$FX_VER-$C.jar" "$MODS/"
cp "$M2/javafx-fxml/$FX_VER/javafx-fxml-$FX_VER-$C.jar"         "$MODS/"

# Tylko fat JAR jako wejście dla jpackage
cp target/fleet-management-1.0-SNAPSHOT.jar "$INPUT/"

# 3. Utwórz .dmg
echo "[3/3] Tworzenie instalatora .dmg..."
rm -rf dist

jpackage \
  --input "$INPUT" \
  --name "FleetManagement" \
  --main-jar fleet-management-1.0-SNAPSHOT.jar \
  --type dmg \
  --module-path "$MODS" \
  --add-modules javafx.controls,javafx.fxml,java.sql,java.naming,java.management,java.instrument,java.desktop,java.xml,jdk.unsupported \
  --app-version 1.0 \
  --dest dist

echo ""
echo "=== Gotowe! Instalator: dist/FleetManagement-1.0.dmg ==="
