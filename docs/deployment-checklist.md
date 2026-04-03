# CLDA 部署指南

## 一、本地开发环境（dev）

### 前置依赖

本地开发需要以下服务运行（可通过 Docker 或本地安装）：

| 服务 | 默认地址 | 用途 |
|------|---------|------|
| MySQL 8.0+ | localhost:3306 | 数据库（库名：`clda`） |
| Redis 7+ | localhost:6379 | 缓存/会话 |
| MinIO | localhost:9000 | 对象存储（可选） |

### 快速启动

```bash
# 1. 创建数据库（如果还没有）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS clda CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p clda < sql/clda-20250615.sql

# 2. 编译
mvn clean install -DskipTests

# 3. 启动 Admin 服务（默认 dev profile，端口 8080）
mvn spring-boot:run -pl clda-admin

# 4. 启动 Chat 服务（端口 8082，需要配置 LLM/TTS 密钥）
mvn spring-boot:run -pl clda-chat

# 5. 启动前端（端口 80）
cd clda-ui
npm install --registry=https://registry.npmmirror.com
npm run dev
```

### dev 环境特性

- **无需任何环境变量**，所有配置均有本地开发默认值
- Swagger UI 启用：http://localhost:8080/swagger-ui.html
- Druid 监控台启用：http://localhost:8080/druid（用户名 clda / 密码 123456）
- 日志级别 DEBUG，devtools 热部署启用
- CORS 允许所有来源，JWT 使用弱密钥（仅限开发）
- 文件上传路径：`./uploadPath`（项目根目录下）
- `/profile/**`、`/minio/**` 文件端点匿名可访问
- 前端 WebSocket 通过 vite proxy 转发到 `ws://localhost:8082`
- 人脸识别模型从本地 `public/models/face-api/` 加载（无需外网）

### 开发环境可选配置

如需覆盖默认值，可设置环境变量或创建 `application-local.yml`（已在 .gitignore 中）：

```bash
# 示例：使用不同的数据库密码
export SPRING_DATASOURCE_DRUID_MASTER_PASSWORD=mypassword

# 示例：配置 LLM 用于数据结构化
export LLM_API_KEY=sk-your-key
export LLM_BASE_URL=https://api.deepseek.com
export LLM_MODEL=deepseek-chat

# 示例：配置 Chat 服务的 LLM
export CHAT_CLIENT_APIKEY=your-zhipu-key
```

---

## 二、生产环境部署（prod）

### 开发 → 生产切换要点

| 项目 | dev（默认） | prod |
|------|-----------|------|
| 激活方式 | 无需配置 | `SPRING_PROFILES_ACTIVE=prod` |
| JWT 密钥 | 弱密钥（dev profile 提供） | **必须**通过 `TOKEN_SECRET` 环境变量设置（≥32字符） |
| MinIO 密钥 | `minioadmin`（dev profile 提供） | **必须**通过 `MINIO_ACCESS_KEY`/`MINIO_SECRET_KEY` 设置 |
| Swagger | 启用 | 禁用 |
| Druid 控制台 | 匿名可访问 | 需要 JWT 认证 |
| 文件端点 `/profile/**` `/minio/**` | 匿名可访问 | 需要 JWT 认证 |
| CORS | 允许所有来源 | 限制为 `CORS_ALLOWED_ORIGINS` 指定域名 |
| 日志级别 | DEBUG | INFO |
| devtools | 启用 | 禁用 |
| 文件路径 | `./uploadPath` | `/home/clda/uploadPath` |
| 数据库密码 | `root`（dev profile 提供） | 通过环境变量设置 |
| WebSocket 前端 | `ws://{hostname}/clda/v1`（vite proxy） | `wss://{hostname}/clda/v1`（nginx 反代，自动适配） |
| WebSocket 设备 | 本机 IP 自动拼接 | 通过 `CHAT_WS_URL` 配置公网地址 |
| 启动安全检查 | 跳过 | `TOKEN_SECRET` 缺失或弱密钥 → 拒绝启动 |

### 安全基线

prod profile 启动时 `SecurityStartupValidator` 会强制校验：

- `TOKEN_SECRET` 已设置、非弱密钥、长度 ≥ 32 字符
- `MINIO_SECRET_KEY` 不为默认值 `minioadmin`

**未通过校验 → 应用拒绝启动**，日志输出具体缺失项。若需本地开发，设置 `SPRING_PROFILES_ACTIVE=dev` 即可跳过校验。

### Docker 部署步骤

```bash
# 1. 准备环境变量
cd docker
cp .env.example .env
# 编辑 .env，替换所有 CHANGE_ME 值（见下方必填清单）

# 2. 构建后端
cd ..
mvn clean install -DskipTests

# 3. 构建前端
cd clda-ui
npm install --registry=https://registry.npmmirror.com
npm run build:prod

# 4. 启动所有服务
cd ../docker
docker compose up -d

# 5. 查看启动状态
docker compose ps
docker compose logs -f clda-admin
```

### 必须设置的环境变量

```bash
# docker/.env 中必须替换的值：

MYSQL_ROOT_PASSWORD=<强密码>                          # 数据库密码
TOKEN_SECRET=<openssl rand -base64 48>                # JWT 签名密钥（≥32字符）
MINIO_ACCESS_KEY=<自定义用户名>                        # MinIO 访问密钥
MINIO_SECRET_KEY=<强密码>                              # MinIO 存储密码
DRUID_STAT_PASSWORD=<强密码>                           # Druid 监控台密码
CORS_ALLOWED_ORIGINS=https://your.domain.com          # 前端域名
CHAT_WS_URL=wss://your.domain.com/clda/v1             # ESP32 设备 WebSocket 地址（公网可达）
LLM_API_KEY=sk-xxx                                    # Admin 端 LLM 密钥
CHAT_LLM_API_KEY=xxx                                  # Chat 端 LLM 密钥
VOLC_TTS_APPID=xxx                                    # 火山 TTS AppID
VOLC_TTS_TOKEN=xxx                                    # 火山 TTS Token
```

### 非 Docker 部署

如果不使用 Docker，直接用 `java -jar` 运行：

```bash
# 设置环境变量后启动 Admin
export SPRING_PROFILES_ACTIVE=prod
export TOKEN_SECRET=$(openssl rand -base64 48)
export SPRING_DATASOURCE_DRUID_MASTER_PASSWORD=your_db_password
export MINIO_ACCESS_KEY=your_minio_user
export MINIO_SECRET_KEY=your_minio_secret
export CORS_ALLOWED_ORIGINS=https://your.domain.com
export CHAT_WS_URL=wss://your.domain.com/clda/v1
# ... 其他环境变量

java -jar clda-admin/target/clda-admin.jar

# 启动 Chat
export CHAT_CLIENT_APIKEY=your-llm-key
export VOLC_TTS_APPID=your-appid
export VOLC_TTS_TOKEN=your-token

java -jar clda-chat/target/clda-chat.jar
```

### nginx WebSocket 反代

生产环境中，前端 WebSocket 和 ESP32 设备均通过 nginx 反代连接 Chat 服务。`docker/nginx.conf` 已配置：

```nginx
location /clda/ {
    proxy_pass http://clda-chat:8082;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 3600s;
}
```

前端自动根据页面协议选择 `ws://` 或 `wss://`，走同源反代路径，无需手动配置端口。

### 人脸识别模型（离线部署）

人脸识别所需的模型文件已内置在 `clda-ui/public/models/face-api/` 目录：

| 文件 | 大小 | 用途 |
|------|------|------|
| `tiny_face_detector_model-weights_manifest.json` | 3 KB | 人脸检测模型描述 |
| `tiny_face_detector_model.bin` | 189 KB | 人脸检测模型权重 |
| `face_landmark_68_tiny_model-weights_manifest.json` | 5 KB | 面部特征点模型描述 |
| `face_landmark_68_tiny_model.bin` | 75 KB | 面部特征点模型权重 |
| `face_recognition_model-weights_manifest.json` | 19 KB | 人脸识别模型描述 |
| `face_recognition_model.bin` | 6.1 MB | 人脸识别模型权重 |

这些文件随前端构建一起打包到 `dist/models/face-api/`，**无需外网访问**，支持离线内网部署。模型来自 `@vladmandic/face-api@1.7.14`。

如需更新模型，从 [vladmandic/face-api](https://github.com/nicehero/nicmedia) 下载对应版本的 `.bin` 文件和 `manifest.json` 替换即可。

---

## 三、部署后验证

### 安全检查

```bash
# 确认源码中无硬编码凭据
grep -r "f895602aec3d43239ef684a643193b6f" --include="*.yml" .   # 应无输出
grep -r "ANs9RmifAoVlz5rqRj0vG532TsFU5oGO" --include="*.yml" . # 应无输出
grep -r "password: root" --include="*.yml" .                     # 应无输出

# 确认无旧名称残留
grep -r "ry-xiaozhi" --include="*.yml" --include="*.java" .     # 应无输出
grep -r "@author ruoyi" --include="*.java" . | wc -l            # 应为 0
```

### 功能验证

- [ ] 前端页面正常加载
- [ ] 管理员登录成功（admin / admin123，**首次登录后立即修改密码**）
- [ ] 登录后默认跳转到 `/business`（不是 `/admin`）
- [ ] 生产环境 Swagger UI 不可访问
- [ ] Druid 控制台需要认证（生产环境）
- [ ] `/profile/**`、`/minio/**` 文件端点需要认证（生产环境）
- [ ] ESP32 设备 OTA 接口正常，返回正确的 WebSocket 地址
- [ ] 语音对话功能正常（WebSocket 通过 nginx 反代）
- [ ] 数据上传：全部失败时前端显示错误、部分失败时显示警告
- [ ] 数据删除：MinIO 中的源文件和中间产物同步清理
- [ ] 人脸识别：机器人登录页模型加载成功（无需外网）
- [ ] 桌面端和机器人端 AI 助手功能一致（无占位页）

---

## 四、配置文件结构

```
clda-admin/src/main/resources/
├── application.yml          # 共享配置（环境变量占位符，无弱默认值）
├── application-dev.yml      # 开发环境（Swagger 启用、DEBUG 日志、弱密钥）
├── application-prod.yml     # 生产环境（Swagger 禁用、INFO 日志、CORS 限制）
└── application-druid.yml    # 数据源配置（dev/prod 共享）

clda-chat/src/main/resources/
└── application.yml          # Chat 服务配置（所有密钥通过环境变量注入）

clda-ui/
├── .env.development         # 前端开发环境变量
├── .env.production          # 前端生产环境变量（可选 VITE_APP_WS_URL 覆盖 WebSocket 地址）
└── public/models/face-api/  # 人脸识别模型文件（离线可用）

docker/
├── .env.example             # 环境变量模板（复制为 .env 使用）
├── .env                     # 实际密钥文件（.gitignore 忽略）
├── docker-compose.yml       # Docker 编排（自动激活 prod profile）
└── nginx.conf               # nginx 配置（含 WebSocket 反代）
```

### Profile 激活机制

```
application.yml
  └── spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}
  └── spring.profiles.group:
        dev:  [druid]    → 加载 application-dev.yml + application-druid.yml
        prod: [druid]    → 加载 application-prod.yml + application-druid.yml
```

- 本地开发：不设任何环境变量，自动使用 `dev` profile
- Docker 部署：`docker-compose.yml` 中设置 `SPRING_PROFILES_ACTIVE=prod`，自动切换生产配置
- 漏配 prod 保护：`SecurityStartupValidator` 在非 dev 环境下校验关键密钥，缺失即拒绝启动
