#!/bin/bash
# AXIOM Full Stack Deployment

set -e

BASE_DIR="/home/an0nimvta/axiom plugin"
SERVER_DIR="$BASE_DIR/server"
BACKEND_DIR="$BASE_DIR/axiom-backend-kotlin"
LAUNCHER_DIR="$BASE_DIR/axiom-launcher-kotlin"
MODPACKS_DIR="$BASE_DIR/modpacks"

# Цвета
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║       AXIOM Full Stack Deploy          ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""

# 1. Сборка Java плагина
echo -e "${YELLOW}[1/4] Сборка Java плагина...${NC}"
cd "$BASE_DIR"
mvn clean package -DskipTests -q
echo -e "${GREEN}✓ Плагин собран${NC}"

# 2. Сборка Kotlin бэкенда
echo -e "${YELLOW}[2/4] Сборка бэкенда...${NC}"
cd "$BACKEND_DIR"
./gradlew build -q
echo -e "${GREEN}✓ Бэкенд собран${NC}"

# 3. Сборка Kotlin лаунчера
echo -e "${YELLOW}[3/4] Сборка лаунчера...${NC}"
cd "$LAUNCHER_DIR"
./gradlew build -q
echo -e "${GREEN}✓ Лаунчер собран${NC}"

# 4. Настройка сервера
echo -e "${YELLOW}[4/4] Настройка сервера...${NC}"
"$BASE_DIR/setup-server.sh" > /dev/null
echo -e "${GREEN}✓ Сервер настроен${NC}"

echo ""
echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║            Всё готово!                 ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""
echo "Запуск компонентов:"
echo ""
echo -e "${YELLOW}1. PostgreSQL${NC} (должен быть запущен)"
echo "   sudo systemctl start postgresql"
echo ""
echo -e "${YELLOW}2. Бэкенд API${NC} (порт 5000)"
echo "   cd '$BACKEND_DIR'"
echo "   MODPACKS_DIR='$MODPACKS_DIR' ./gradlew run"
echo ""
echo -e "${YELLOW}3. Minecraft сервер${NC} (порт 25565)"
echo "   cd '$SERVER_DIR'"
echo "   ./start.sh"
echo ""
echo -e "${YELLOW}4. Лаунчер${NC}"
echo "   cd '$LAUNCHER_DIR'"
echo "   ./gradlew run"
echo ""
