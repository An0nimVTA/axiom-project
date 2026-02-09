#!/bin/bash
# Скрипт настройки VPS для раздачи файлов обновлений

echo "🚀 Настройка VPS для AXIOM Updates"

# Установка nginx
apt update
apt install -y nginx

# Создание директории для файлов
mkdir -p /var/www/axiom/updates

# Настройка nginx
cat > /etc/nginx/sites-available/axiom << 'EOF'
server {
    listen 80;
    server_name _;
    
    root /var/www/axiom;
    
    location /updates/ {
        alias /var/www/axiom/updates/;
        autoindex on;
        add_header Access-Control-Allow-Origin *;
    }
}
EOF

# Активация конфига
ln -sf /etc/nginx/sites-available/axiom /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

# Перезапуск nginx
systemctl restart nginx
systemctl enable nginx

echo "✅ Nginx настроен"
echo "📁 Загружайте файлы в: /var/www/axiom/updates/"
echo "🌐 URL: http://193.23.201.6/updates/"
