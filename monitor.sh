#!/bin/bash

# AXIOM Monitoring System
# Мониторинг состояния сервера

SERVER_DIR="/home/an0nimvta/axiom plugin/server"
LOG_FILE="/home/an0nimvta/axiom-monitor.log"

check_server() {
  if pgrep -f "mohist.*server.jar" > /dev/null; then
    echo "✅ Сервер работает"
    return 0
  else
    echo "❌ Сервер не запущен"
    return 1
  fi
}

check_memory() {
  local mem=$(ps aux | grep "mohist.*server.jar" | grep -v grep | awk '{print $4}')
  echo "💾 Использование памяти: ${mem}%"
}

check_players() {
  if [ -f "$SERVER_DIR/logs/latest.log" ]; then
    local players=$(grep -c "logged in with entity id" "$SERVER_DIR/logs/latest.log" 2>/dev/null || echo "0")
    echo "👥 Игроков подключалось: $players"
  fi
}

check_errors() {
  if [ -f "$SERVER_DIR/logs/latest.log" ]; then
    local errors=$(grep -c "ERROR" "$SERVER_DIR/logs/latest.log" 2>/dev/null || echo "0")
    echo "⚠️  Ошибок в логах: $errors"
  fi
}

check_disk() {
  local disk=$(df -h "$SERVER_DIR" | tail -1 | awk '{print $5}')
  echo "💿 Использование диска: $disk"
}

# Запуск проверок
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║                    AXIOM Server Monitor                              ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""
echo "🕐 $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

check_server
check_memory
check_players
check_errors
check_disk

echo ""

# Логирование
{
  echo "$(date '+%Y-%m-%d %H:%M:%S') - Status check"
  check_server
  check_memory
} >> "$LOG_FILE"
