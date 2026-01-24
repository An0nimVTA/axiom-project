#!/bin/bash

# AXIOM Auto-Build Script
# Автоматическая сборка всех компонентов

set -e

echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║                    AXIOM Auto-Build System                           ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"

PROJECT_DIR="/home/an0nimvta/axiom plugin"
BUILD_DIR="$PROJECT_DIR/builds"
DATE=$(date +%Y%m%d_%H%M%S)

# Создать директорию для сборок
mkdir -p "$BUILD_DIR/$DATE"

echo ""
echo "📦 Сборка компонентов..."
echo ""

# 1. Plugin
echo "1️⃣  Сборка AXIOM Plugin..."
cd "$PROJECT_DIR"
mvn clean package -DskipTests -q
cp axiom-plugin/target/axiom-plugin-*.jar "$BUILD_DIR/$DATE/axiom-plugin.jar"
echo "   ✅ Plugin собран"

# 2. UI Mod
echo "2️⃣  Сборка AXIOM UI Mod..."
cd "$PROJECT_DIR/axiom-mod-integration"
GRADLE_USER_HOME=$PWD/.gradle ./gradlew clean build -x test -q
cp build/libs/axiomui-*.jar "$BUILD_DIR/$DATE/axiomui-mod.jar"
echo "   ✅ UI Mod собран"

# 3. Launcher
echo "3️⃣  Сборка AXIOM Launcher..."
cd "$PROJECT_DIR/axiom-launcher-kotlin"
./gradlew clean build -x test -q
cp build/libs/axiom-launcher-*.jar "$BUILD_DIR/$DATE/axiom-launcher.jar"
echo "   ✅ Launcher собран"

echo ""
echo "📊 Результаты сборки:"
echo ""
ls -lh "$BUILD_DIR/$DATE/"

echo ""
echo "✅ Все компоненты успешно собраны!"
echo "📁 Директория: $BUILD_DIR/$DATE/"
echo ""
