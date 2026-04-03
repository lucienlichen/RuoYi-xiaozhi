#!/bin/bash
# CLDA 一键启动脚本
# 启动顺序: Colima(Docker) -> MySQL/Redis -> Admin(8080) -> Chat(8082) -> UI(80)

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="/tmp/clda-logs"
mkdir -p "$LOG_DIR"

# 颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${GREEN}[CLDA]${NC} $1"; }
warn()  { echo -e "${YELLOW}[CLDA]${NC} $1"; }
error() { echo -e "${RED}[CLDA]${NC} $1"; }

# 等待端口就绪
wait_port() {
    local port=$1 name=$2 timeout=${3:-60}
    local elapsed=0
    while ! lsof -ti :$port >/dev/null 2>&1; do
        if [ $elapsed -ge $timeout ]; then
            error "$name 启动超时 (port $port), 查看日志: $LOG_DIR"
            return 1
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done
    info "$name 已就绪 (port $port) [${elapsed}s]"
}

# ========== 1. Docker (Colima) ==========
if ! colima status >/dev/null 2>&1; then
    info "启动 Colima..."
    colima start --memory 4 --cpu 2 2>&1 | tail -1
else
    info "Colima 已在运行"
fi

# ========== 2. MySQL & Redis ==========
start_container() {
    local name=$1
    local status
    status=$(docker inspect -f '{{.State.Running}}' "$name" 2>/dev/null || echo "not_found")
    if [ "$status" = "true" ]; then
        info "$name 已在运行"
    elif [ "$status" = "false" ]; then
        info "启动 $name..."
        docker start "$name" >/dev/null
    else
        error "$name 容器不存在，请先创建"
        return 1
    fi
}

start_container "vibeproto-mysql"
start_container "vibeproto-redis"

# 等待 MySQL 就绪
info "等待 MySQL 就绪..."
for i in $(seq 1 30); do
    if docker exec vibeproto-mysql mysql -uroot -proot -e "SELECT 1" >/dev/null 2>&1; then
        info "MySQL 已就绪"
        break
    fi
    if [ $i -eq 30 ]; then
        error "MySQL 启动超时"
        exit 1
    fi
    sleep 2
done

# ========== 3. 构建项目 ==========
cd "$PROJECT_DIR"

# 检查是否需要重新构建
if [ "$1" = "--skip-build" ]; then
    info "跳过构建 (--skip-build)"
elif [ "$1" = "--build" ] || [ ! -f clda-admin/target/clda-admin.jar ]; then
    info "构建项目..."
    mvn clean install -DskipTests -q
    info "构建完成"
else
    info "使用已有构建 (加 --build 强制重新构建)"
fi

# ========== 4. 停止旧进程 ==========
for port in 8080 8082; do
    pid=$(lsof -ti :$port 2>/dev/null || true)
    if [ -n "$pid" ]; then
        warn "停止端口 $port 上的旧进程 (PID: $pid)"
        kill $pid 2>/dev/null || true
        sleep 2
    fi
done

# 停止旧的前端进程 (node/vite on port 80)
pid=$(lsof -ti :80 2>/dev/null || true)
if [ -n "$pid" ]; then
    warn "停止端口 80 上的旧进程 (PID: $pid)"
    kill $pid 2>/dev/null || true
    sleep 1
fi

# ========== 5. 启动 Admin Server (8080) ==========
info "启动 Admin Server..."
mvn spring-boot:run -pl clda-admin > "$LOG_DIR/admin.log" 2>&1 &
ADMIN_PID=$!
echo $ADMIN_PID > "$LOG_DIR/admin.pid"

# ========== 6. 启动 Chat Server (8082) ==========
info "启动 Chat Server..."
mvn spring-boot:run -pl clda-chat > "$LOG_DIR/chat.log" 2>&1 &
CHAT_PID=$!
echo $CHAT_PID > "$LOG_DIR/chat.pid"

# ========== 7. 启动前端 (80) ==========
info "启动前端..."
cd "$PROJECT_DIR/clda-ui"
npm run dev > "$LOG_DIR/ui.log" 2>&1 &
UI_PID=$!
echo $UI_PID > "$LOG_DIR/ui.pid"
cd "$PROJECT_DIR"

# ========== 8. 等待所有服务就绪 ==========
info "等待服务启动..."
wait_port 8080 "Admin Server" 90
wait_port 8082 "Chat Server"  90
wait_port 80   "Frontend"     30

echo ""
info "========================================="
info "  CLDA 所有服务已启动!"
info "========================================="
info "  前端:        http://localhost"
info "  Admin API:   http://localhost:8080"
info "  Chat WS:     ws://localhost:8082/clda/v1"
info "  Druid 监控:  http://localhost:8080/druid"
info "========================================="
info "  日志目录: $LOG_DIR"
info "  停止服务: $PROJECT_DIR/stop.sh"
info "========================================="
