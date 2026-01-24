#!/bin/bash

# AXIOM Backup System
# Автоматическое резервное копирование

set -e

BACKUP_DIR="/home/an0nimvta/axiom-backups"
PROJECT_DIR="/home/an0nimvta/axiom plugin"
SERVER_DIR="$PROJECT_DIR/server"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p "$BACKUP_DIR"

echo "🔄 Создание backup..."

# 1. Код проекта
echo "1️⃣  Backup кода..."
tar -czf "$BACKUP_DIR/code_$DATE.tar.gz" \
  --exclude="*/target/*" \
  --exclude="*/build/*" \
  --exclude="*/.gradle/*" \
  --exclude="*/.git/*" \
  --exclude="*/node_modules/*" \
  -C "$PROJECT_DIR" \
  axiom-plugin axiom-mod-integration axiom-launcher-kotlin

# 2. Сервер (без world)
echo "2️⃣  Backup сервера..."
tar -czf "$BACKUP_DIR/server_$DATE.tar.gz" \
  --exclude="*/world/*" \
  --exclude="*/logs/*" \
  --exclude="*/crash-reports/*" \
  -C "$SERVER_DIR" \
  plugins mods config

# 3. База данных
echo "3️⃣  Backup базы данных..."
if [ -f "$SERVER_DIR/plugins/AXIOM/axiom.db" ]; then
  cp "$SERVER_DIR/plugins/AXIOM/axiom.db" "$BACKUP_DIR/axiom_$DATE.db"
fi

# Удалить старые backup (>7 дней)
find "$BACKUP_DIR" -name "*.tar.gz" -mtime +7 -delete
find "$BACKUP_DIR" -name "*.db" -mtime +7 -delete

echo ""
echo "✅ Backup создан: $BACKUP_DIR"
ls -lh "$BACKUP_DIR" | tail -5
