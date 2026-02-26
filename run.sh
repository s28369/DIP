#!/bin/bash

M2="$HOME/.m2/repository/org/openjfx"
FX_VER="21.0.1"
CLASSIFIER="mac-aarch64"

MODULE_PATH="\
$M2/javafx-base/$FX_VER/javafx-base-$FX_VER-$CLASSIFIER.jar:\
$M2/javafx-graphics/$FX_VER/javafx-graphics-$FX_VER-$CLASSIFIER.jar:\
$M2/javafx-controls/$FX_VER/javafx-controls-$FX_VER-$CLASSIFIER.jar:\
$M2/javafx-fxml/$FX_VER/javafx-fxml-$FX_VER-$CLASSIFIER.jar"

JAR="$(dirname "$0")/target/fleet-management-1.0-SNAPSHOT.jar"

java \
  --module-path "$MODULE_PATH" \
  --add-modules javafx.controls,javafx.fxml \
  -jar "$JAR"
