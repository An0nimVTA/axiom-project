#!/bin/bash
# Скрипт настройки VPS для AXIOM Updates (HTTP :8080)

echo "🚀 Настройка VPS (Port 8080)..."

# Установка nginx
apt update
apt install -y nginx

# Создание директории
mkdir -p /var/www/axiom/updates

# Конфигурация Nginx (только HTTP на 8080)
cat > /etc/nginx/nginx.conf << 'EOF'
worker_processes 1;
events { worker_connections 1024; }

http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile        on;
    keepalive_timeout  65;

    server {
        listen 8080;
        server_name _;
        
        # Раздача файлов обновлений
        location /updates/ {
            alias /var/www/axiom/updates/;
            autoindex on;
            add_header Access-Control-Allow-Origin *;
        }
        
        # Главная страница
        location / {
            return 200 "AXIOM Updates Server is Running!";
        }
    }
}
EOF

# Перезапуск
nginx -t && systemctl restart nginx
systemctl enable nginx

echo "✅ Готово! Сервер: http://193.23.201.6:8080/updates/"
