#!/usr/bin/env bash
# ==============================================================================
# QuickShop - Automated VPS Provisioning & Deployment Script for Ubuntu
# Server Target: 221.121.5.123
# Author: Lê Đức Anh (DevOps & Security Lead)
# ==============================================================================

set -euo pipefail

echo "================================================================="
echo "  QuickShop - Cài đặt & Khởi tạo Môi trường VPS Ubuntu 22.04/24.04"
echo "  Máy chủ đích: 221.121.5.123"
echo "================================================================="

# 1. Update system packages
echo "[1/6] Cập nhật gói hệ điều hành Ubuntu..."
apt-get update -y
apt-get upgrade -y
apt-get install -y ca-certificates curl gnupg lsb-release git openssl ufw sshpass

# 2. Install Docker & Docker Compose plugin
echo "[2/6] Kiểm tra và cài đặt Docker Engine & Docker Compose plugin..."
if ! command -v docker &> /dev/null; then
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg

    echo \
      "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
      tee /etc/apt/sources.list.d/docker.list > /dev/null

    apt-get update -y
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    systemctl enable docker
    systemctl start docker
    echo "Docker đã cài đặt thành công."
else
    echo "Docker đã tồn tại, tiếp tục..."
fi

# 3. Configure UFW Firewall
echo "[3/6] Cấu hình tường lửa UFW (Mở cổng 22, 80, 443)..."
ufw allow 22/tcp comment 'SSH Port'
ufw allow 80/tcp comment 'HTTP Nginx Reverse Proxy'
ufw allow 443/tcp comment 'HTTPS SSL Termination'
ufw --force enable
echo "Tường lửa đã kích hoạt an toàn."

# 4. Create deployment folder & clone repo
echo "[4/6] Khởi tạo thư mục dự án tại /opt/quickshop..."
mkdir -p /opt/quickshop
cd /opt/quickshop

if [ ! -d ".git" ]; then
    echo "Cloning repository từ GitHub..."
    git clone https://github.com/Leducanh30112006/HN_KS24_CNTT2_OJT.git .
else
    echo "Repository đã tồn tại, pull code mới nhất từ main..."
    git fetch origin main
    git reset --hard origin/main
fi

# 5. Generate SSL Certificate for HTTPS (443)
echo "[5/6] Sinh chứng chỉ SSL tự ký cho IP 221.121.5.123..."
chmod +x ./deployments/nginx/generate-ssl.sh
bash ./deployments/nginx/generate-ssl.sh

# 6. Prepare .env configuration & Launch services
echo "[6/6] Khởi tạo tệp .env và khởi chạy hệ thống QuickShop..."
if [ ! -f ".env" ]; then
    cp .env.example .env
    echo "Đã tạo tệp .env từ .env.example"
fi

echo "Khởi chạy toàn bộ hạ tầng và các microservices bằng Docker Compose Production..."
docker compose -f docker-compose.prod.yml down || true
docker compose -f docker-compose.prod.yml up -d --build

echo "================================================================="
echo "  TRIỂN KHAI HOÀN TẤT TRÊN VPS 221.121.5.123!"
echo "  - Giao diện Web & API: http://221.121.5.123 hoặc https://221.121.5.123"
echo "  - Trạng thái container:"
docker compose -f docker-compose.prod.yml ps
echo "================================================================="
