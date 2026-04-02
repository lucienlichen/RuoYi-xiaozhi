#!/bin/bash
# CLDA 一键停止脚本

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info() { echo -e "${GREEN}[CLDA]${NC} $1"; }
warn() { echo -e "${YELLOW}[CLDA]${NC} $1"; }

LOG_DIR="/tmp/clda-logs"

# 停止 Java 服务和前端
for port in 8080 8082 80; do
    pid=$(lsof -ti :$port 2>/dev/null || true)
    if [ -n "$pid" ]; then
        info "停止端口 $port (PID: $pid)"
        kill $pid 2>/dev/null || true
    fi
done

# 清理 PID 文件
rm -f "$LOG_DIR"/*.pid 2>/dev/null

info "所有 CLDA 服务已停止"
info "(MySQL/Redis/Colima 保持运行，如需停止请手动执行)"
