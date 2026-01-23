#!/bin/bash
# Setup PostgreSQL for AXIOM Backend

echo "=== AXIOM Database Setup ==="

# Check if PostgreSQL is running
if ! systemctl is-active --quiet postgresql; then
    echo "Starting PostgreSQL..."
    sudo systemctl start postgresql
fi

# Create database and user
sudo -u postgres psql << EOF
-- Create user if not exists
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'axiom_user') THEN
        CREATE USER axiom_user WITH PASSWORD 'axiom_password';
    END IF;
END
\$\$;

-- Create database if not exists
SELECT 'CREATE DATABASE axiom_launcher OWNER axiom_user'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'axiom_launcher')\gexec

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE axiom_launcher TO axiom_user;
EOF

echo "Database setup complete!"
echo ""
echo "Connection details:"
echo "  Host: localhost"
echo "  Port: 5432"
echo "  Database: axiom_launcher"
echo "  User: axiom_user"
echo "  Password: axiom_password"
