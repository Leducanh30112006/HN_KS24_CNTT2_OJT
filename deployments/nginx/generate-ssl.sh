#!/usr/bin/env bash
# QuickShop - SSL Certificate Generator for Nginx Reverse Proxy
set -e

SSL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/ssl" && pwd)"
mkdir -p "$SSL_DIR"

if [ ! -f "$SSL_DIR/nginx.crt" ] || [ ! -f "$SSL_DIR/nginx.key" ]; then
    echo "Generating self-signed SSL certificate for QuickShop (HTTPS: 443)..."
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout "$SSL_DIR/nginx.key" \
        -out "$SSL_DIR/nginx.crt" \
        -subj "/C=VN/ST=Hanoi/L=Hanoi/O=QuickShop/CN=221.121.5.123"
    echo "SSL Certificate generated at $SSL_DIR"
else
    echo "SSL Certificate already exists at $SSL_DIR"
fi

