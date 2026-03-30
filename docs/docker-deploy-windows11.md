# RuoYi-Xiaozhi Windows 11 Docker 部署指南

## 1. 前置要求

**构建环境（开发机）：**
- Java 21 JDK（Azul Zulu 或 OpenJDK）
- Apache Maven 3.8+
- Node.js 18+

**部署环境（Windows 11）：**
- Docker Desktop for Windows（启用 WSL 2 后端）
- 至少 8GB 可用内存

## 2. 部署架构

```
                    ┌─────────────┐
                    │   Nginx     │ :80
                    │  (前端+代理) │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              ▼                         ▼
     ┌────────────────┐       ┌─────────────────┐
     │ xiaozhi-admin  │ :8080 │  xiaozhi-chat   │ :8082
     │  (REST API)    │       │  (WebSocket)    │
     └───────┬────────┘       └────────┬────────┘
             │                         │
             │    ┌──────────┐         │
             ├───►│  MySQL   │ :3306   │
             │    └──────────┘         │
             │    ┌──────────┐         │
             └───►│  Redis   │ :6379   │
                  └──────────┘
```

## 3. 构建步骤

### 3.1 构建后端

```bash
# 在项目根目录执行
mvn clean install -DskipTests
```

构建完成后会生成：
- `xiaozhi-admin/target/xiaozhi-admin.jar`
- `xiaozhi-chat/target/xiaozhi-chat.jar`

### 3.2 构建前端

```bash
cd xiaozhi-ui
npm install --registry=https://registry.npmmirror.com
npm run build:prod
```

构建产物在 `xiaozhi-ui/dist/` 目录。

### 3.3 下载 ASR 模型

```bash
# 下载 SenseVoice 语音识别模型（约 1GB）
wget https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17.tar.bz2
tar xvf sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17.tar.bz2
```

将解压后的目录放到项目根目录下的 `models/` 文件夹中。

## 4. 配置变更清单

以下配置通过 Docker 环境变量覆盖，**无需修改源码文件**：

| 配置项 | 源码默认值 | Docker 覆盖值 | 配置文件 |
|--------|-----------|--------------|---------|
| MySQL 地址 | `localhost:3306` | `mysql:3306` | application-druid.yml |
| MySQL 密码 | `password` | 自定义 | application-druid.yml |
| MySQL 字符集 | `utf8` | `utf8mb4` | application-druid.yml |
| Redis 地址 | `localhost` | `redis` | application.yml (admin) |
| 文件上传路径 | `D:/ruoyi/uploadPath` | `/opt/ruoyi/uploadPath` | application.yml (admin) |
| Feign 管理地址 | `http://localhost:8080` | `http://xiaozhi-admin:8080` | DeviceClient.java 默认值 |
| ASR 模型路径 | 本地绝对路径 | `/opt/models/sherpa-onnx-sense-voice-...` | application.yml (chat) |
| LLM API Key | 空 | 你的 API Key | application.yml (chat) |
| LLM URL | 空 | 模型服务地址 | application.yml (chat) |
| TTS 配置 | 空 | 火山引擎 appid/token | application.yml (chat) |

### 关于原生库

以下原生库已打包在 Maven JAR 中，Docker Linux 容器会自动加载对应平台版本，**无需额外操作**：
- sherpa-onnx JNI（`sherpa-onnx/src/main/resources/native/linux-x64/`）
- LWJGL + Opus（Maven classifier `natives-linux`）
- FFmpeg/JavaCV（Maven classifier `linux-x86_64`）

## 5. Docker 文件

### 5.1 目录结构

```
project-root/
├── docker/
│   ├── docker-compose.yml
│   ├── Dockerfile.admin
│   ├── Dockerfile.chat
│   └── nginx.conf
├── models/
│   └── sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/
├── sql/
│   └── ry-xiaozhi-20250615.sql
├── xiaozhi-admin/target/xiaozhi-admin.jar
├── xiaozhi-chat/target/xiaozhi-chat.jar
└── xiaozhi-ui/dist/
```

### 5.2 docker-compose.yml

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: xiaozhi-mysql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci
    environment:
      MYSQL_ROOT_PASSWORD: xiaozhi123
      MYSQL_DATABASE: ry-xiaozhi
    volumes:
      - ../sql/ry-xiaozhi-20250615.sql:/docker-entrypoint-initdb.d/01-init.sql
      - mysql-data:/var/lib/mysql
    ports:
      - "3306:3306"
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p$$MYSQL_ROOT_PASSWORD"]
      interval: 10s
      timeout: 5s
      retries: 10
    networks:
      - xiaozhi-net

  redis:
    image: redis:7-alpine
    container_name: xiaozhi-redis
    ports:
      - "6379:6379"
    networks:
      - xiaozhi-net

  xiaozhi-admin:
    build:
      context: ..
      dockerfile: docker/Dockerfile.admin
    container_name: xiaozhi-admin
    environment:
      # MySQL
      SPRING_DATASOURCE_DRUID_MASTER_URL: jdbc:mysql://mysql:3306/ry-xiaozhi?useUnicode=true&characterEncoding=utf8mb4&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8
      SPRING_DATASOURCE_DRUID_MASTER_USERNAME: root
      SPRING_DATASOURCE_DRUID_MASTER_PASSWORD: xiaozhi123
      # Redis
      SPRING_DATA_REDIS_HOST: redis
      # 文件上传路径
      RUOYI_PROFILE: /opt/ruoyi/uploadPath
    ports:
      - "8080:8080"
    volumes:
      - upload-data:/opt/ruoyi/uploadPath
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    networks:
      - xiaozhi-net

  xiaozhi-chat:
    build:
      context: ..
      dockerfile: docker/Dockerfile.chat
    container_name: xiaozhi-chat
    environment:
      # ASR 模型路径
      MODEL_ASR_SENSE__VOICE_MODEL__DIR: /opt/models/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17
      # Feign 调用 admin 服务地址
      CHAT_SERVER_MANAGE__URL: http://xiaozhi-admin:8080
      # LLM 配置（按需修改）
      CHAT_CLIENT_APIKEY: 你的API密钥
      CHAT_CLIENT_URL: https://open.bigmodel.cn/api/paas
      CHAT_CLIENT_MODEL: glm-4-flash
      CHAT_CLIENT_PATH: /v4/chat/completions
      # TTS 配置（火山引擎）
      MODEL_TTS_VOLC__TTS_APPID: 你的AppID
      MODEL_TTS_VOLC__TTS_ACCESS__TOKEN: 你的AccessToken
    ports:
      - "8082:8082"
    volumes:
      - ../models/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17:/opt/models/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17:ro
    depends_on:
      - xiaozhi-admin
    networks:
      - xiaozhi-net

  nginx:
    image: nginx:alpine
    container_name: xiaozhi-nginx
    ports:
      - "80:80"
    volumes:
      - ../xiaozhi-ui/dist:/usr/share/nginx/html:ro
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
    depends_on:
      - xiaozhi-admin
    networks:
      - xiaozhi-net

volumes:
  mysql-data:
  upload-data:

networks:
  xiaozhi-net:
    driver: bridge
```

### 5.3 Dockerfile.admin

```dockerfile
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY xiaozhi-admin/target/xiaozhi-admin.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 5.4 Dockerfile.chat

```dockerfile
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY xiaozhi-chat/target/xiaozhi-chat.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 5.5 nginx.conf

```nginx
server {
    listen 80;
    server_name localhost;

    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API 代理（对应前端生产环境 VITE_APP_BASE_API = '/prod-api'）
    location /prod-api/ {
        proxy_pass http://xiaozhi-admin:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # WebSocket 代理（可选，用于浏览器语音测试页）
    location /xiaozhi/ {
        proxy_pass http://xiaozhi-chat:8082;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 3600s;
    }
}
```

## 6. 启动与验证

### 6.1 启动服务

```bash
cd docker
docker-compose up -d
```

首次启动需要等待：
1. MySQL 初始化数据库（约 30 秒）
2. Admin 服务启动（约 15 秒）
3. Chat 服务加载 ASR 模型（约 30 秒）

查看日志：
```bash
docker-compose logs -f xiaozhi-admin   # Admin 服务日志
docker-compose logs -f xiaozhi-chat    # Chat 服务日志
```

### 6.2 验证

| 服务 | 地址 | 说明 |
|------|------|------|
| 管理后台 | http://localhost | 默认账号 admin / admin123 |
| Admin API | http://localhost:8080 | REST API |
| Chat WebSocket | ws://localhost:8082/xiaozhi/v1 | 语音对话 |

### 6.3 停止服务

```bash
docker-compose down        # 停止并删除容器
docker-compose down -v     # 同时删除数据卷（会丢失数据库数据）
```

## 7. 常见问题

### MySQL 中文乱码
确保 docker-compose.yml 中 MySQL 的 `command` 参数包含 `--character-set-server=utf8mb4`。如果已有数据乱码，需要删除数据卷重新初始化：
```bash
docker-compose down -v
docker-compose up -d
```

### Chat 服务连接 Admin 失败
检查环境变量 `CHAT_SERVER_MANAGE__URL` 是否正确设置为 `http://xiaozhi-admin:8080`。注意 Spring Boot 环境变量中 `-` 需要用 `__`（双下划线）替代。

### ASR 模型加载失败
确认模型目录已正确挂载到容器内 `/opt/models/` 路径，且目录内包含 `model.int8.onnx` 和 `tokens.txt` 文件。

### 端口冲突
如果 80、8080、8082 等端口被占用，修改 docker-compose.yml 中的端口映射（冒号左边是宿主机端口）。

### ESP32 设备连接
设备的 OTA 地址改为：`http://你的Windows主机IP:8080/api/ota`
设备会自动获取 WebSocket 地址：`ws://你的Windows主机IP:8082/xiaozhi/v1`
