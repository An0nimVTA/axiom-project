#!/bin/bash
# AXIOM Server Setup Script

set -e

SERVER_DIR="/home/an0nimvta/axiom plugin/server"
PLUGIN_DIR="$SERVER_DIR/plugins"
MODPACKS_DIR="/home/an0nimvta/axiom plugin/modpacks"

echo "=== AXIOM Server Setup ==="

# 1. Собрать плагин
echo "[1/5] Сборка плагина..."
cd "/home/an0nimvta/axiom plugin"
mvn clean package -DskipTests -q

# 2. Копировать плагин на сервер
echo "[2/5] Установка плагина..."
cp -f "$SERVER_DIR/../axiom-core/target/axiom-core-2.0.0.jar" "$PLUGIN_DIR/" 2>/dev/null || true

# Основной плагин уже должен быть собран в src
if [ -f "target/axiom-geopolitical-engine-1.0.0.jar" ]; then
    cp -f "target/axiom-geopolitical-engine-1.0.0.jar" "$PLUGIN_DIR/"
fi

# 3. Настроить server.properties
echo "[3/5] Настройка сервера..."
cat > "$SERVER_DIR/server.properties" << 'EOF'
server-port=25565
motd=AXIOM Geopolitical Server
max-players=100
online-mode=false
enable-command-block=true
spawn-protection=0
view-distance=10
simulation-distance=8
allow-flight=true
pvp=true
difficulty=normal
gamemode=survival
level-name=world
EOF

# 4. Настроить EULA
echo "eula=true" > "$SERVER_DIR/eula.txt"

# 5. Создать стартовый скрипт
echo "[4/5] Создание скриптов запуска..."
cat > "$SERVER_DIR/start.sh" << 'EOF'
#!/bin/bash
cd "$(dirname "$0")"
java -Xms2G -Xmx4G -jar mohist.jar nogui
EOF
chmod +x "$SERVER_DIR/start.sh"

# 6. Проверить конфиг плагина
echo "[5/5] Проверка конфигурации плагина..."
mkdir -p "$PLUGIN_DIR/AXIOM"
if [ ! -f "$PLUGIN_DIR/AXIOM/config.yml" ]; then
    cat > "$PLUGIN_DIR/AXIOM/config.yml" << 'EOF'
# AXIOM Configuration
database:
  type: sqlite
  file: axiom.db

autosave:
  enabled: true
  intervalSeconds: 300

nations:
  maxClaimsPerNation: 1000
  minDistanceBetweenCapitals: 500
  
economy:
  startingBalance: 1000
  taxRate: 0.05
  
war:
  enabled: true
  minPlayersToStart: 2
  siegeDurationMinutes: 30
EOF
fi

echo ""
echo "=== Готово! ==="
echo ""
echo "Плагины установлены:"
ls -la "$PLUGIN_DIR"/*.jar
echo ""
echo "Запуск сервера:"
echo "  cd '$SERVER_DIR' && ./start.sh"
