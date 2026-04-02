# CLDA Windows 11 Docker 完整部署指南

> 起重装备全生命周期数据AI智能体平台（CLDA）— 含 OCR、图片增强、LLM 结构化

## 1. 前置要求

### 构建环境（开发机）
- Java 21 JDK（Azul Zulu 或 OpenJDK）
- Apache Maven 3.8+
- Node.js 18+

### 部署环境（Windows 11）
- Docker Desktop for Windows（启用 WSL 2 后端）
- 至少 8GB 可用内存（推荐 16GB）
- 磁盘空间 ≥ 10GB（含模型文件和 Docker 镜像）

## 2. 部署架构

```
                    ┌─────────────┐
                    │   Nginx     │ :80
                    │  (前端+代理) │
                    └──────┬──────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
 ┌────────────────┐ ┌──────────────┐ ┌──────────────┐
 │  clda-admin    │ │  clda-chat   │ │ clda-enhance │
 │  (REST API)    │ │  (WebSocket) │ │ (图片增强)    │
 │  :8080         │ │  :8082       │ │ :8090        │
 └───────┬────────┘ └──────┬───────┘ └──────────────┘
         │                 │
         │  ┌──────────┐   │
         ├─►│  MySQL   │◄──┤
         │  │  :3306   │   │
         │  └──────────┘   │
         │  ┌──────────┐   │
         └─►│  Redis   │◄──┘
            │  :6379   │
            └──────────┘
```

### 服务说明

| 服务 | 端口 | 说明 |
|------|------|------|
| Nginx | 80 | 前端静态文件 + API/WebSocket 反向代理 |
| clda-admin | 8080 | REST API、文件上传、OCR处理、LLM结构化 |
| clda-chat | 8082 | WebSocket 语音对话、ASR/TTS |
| clda-enhance | 8090 | Python 图片超分辨率增强（Real-ESRGAN） |
| MySQL | 3306 | 主数据库（utf8mb4_unicode_ci） |
| Redis | 6379 | 缓存和配置存储 |

## 3. 构建步骤

### 3.1 构建后端

```bash
# 在项目根目录执行
mvn clean install -DskipTests
```

构建完成后生成：
- `clda-admin/target/clda-admin.jar`
- `clda-chat/target/clda-chat.jar`

### 3.2 构建前端

```bash
cd clda-ui
npm install --registry=https://registry.npmmirror.com
npm run build:prod
```

构建产物在 `clda-ui/dist/` 目录。

### 3.3 下载 ASR 模型

```bash
# 下载 SenseVoice 语音识别模型（约 1GB）
wget https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17.tar.bz2
tar xvf sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17.tar.bz2
```

将解压后的目录放到项目根目录下的 `models/` 文件夹中。

## 4. 配置变更清单

以下配置通过 Docker 环境变量覆盖，**无需修改源码文件**：

| 配置项 | 源码默认值 | Docker 覆盖值 | 说明 |
|--------|-----------|--------------|------|
| MySQL 地址 | `localhost:3306` | `mysql:3306` | 容器内网络 |
| MySQL 密码 | `root` | `clda123` | 自定义强密码 |
| Redis 地址 | `localhost` | `redis` | 容器内网络 |
| 文件上传路径 | `D:/clda/uploadPath` | `/opt/clda/uploadPath` | Linux 容器路径 |
| OCR tessdata 路径 | `/usr/local/share/tessdata` | `/usr/share/tesseract-ocr/5/tessdata` | 容器内 Tesseract 数据路径 |
| 图片增强服务地址 | `http://localhost:8090` | `http://clda-enhance:8090` | 容器内网络 |
| LLM API Key | `sk-placeholder` | 你的 API Key | 管理端 AI 配置页面可修改 |
| LLM API 地址 | `https://api.openai.com` | 你的 LLM 服务地址 | 兼容 OpenAI 协议 |
| LLM 模型 | `gpt-4o-mini` | 自定义 | 管理端可修改 |
| ASR 模型路径 | 本地路径 | `/opt/models/sherpa-onnx-...` | 容器内路径 |
| Feign 管理地址 | `http://localhost:8080` | `http://clda-admin:8080` | 容器内网络 |
| Chat LLM API Key | 空 | 你的 API Key | 语音对话 LLM |
| TTS AppID/Token | 空 | 火山引擎凭证 | 语音合成 |

### 关于原生库

以下原生库已打包在 Maven JAR 中，Docker Linux 容器会自动加载对应平台版本：
- sherpa-onnx JNI（ASR 推理）
- LWJGL + Opus（音频编解码）
- FFmpeg/JavaCV（音频处理）
- OpenCV/JavaCV（图片预处理：去噪、二值化、锐化、纠偏）

**需要额外安装的**：
- Tesseract OCR 原生库（通过 Dockerfile 安装）

## 5. Docker 文件

### 5.1 目录结构

```
project-root/
├── docker/
│   ├── docker-compose.yml
│   ├── Dockerfile.admin
│   ├── Dockerfile.chat
│   ├── Dockerfile.enhance
│   └── nginx.conf
├── models/
│   └── sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/
├── sql/
│   ├── ry-xiaozhi-20250615.sql    # 系统基础表
│   └── emds-v2-init.sql           # 业务表 + 分类 + 模板
├── clda-admin/target/clda-admin.jar
├── clda-chat/target/clda-chat.jar
├── clda-enhance/
│   ├── app.py
│   └── requirements.txt
└── clda-ui/dist/
```

### 5.2 docker-compose.yml

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: clda-mysql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    environment:
      MYSQL_ROOT_PASSWORD: clda123
      MYSQL_DATABASE: ry-xiaozhi
    volumes:
      - ../sql/ry-xiaozhi-20250615.sql:/docker-entrypoint-initdb.d/01-init.sql
      - ../sql/emds-v2-init.sql:/docker-entrypoint-initdb.d/02-emds.sql
      - mysql-data:/var/lib/mysql
    ports:
      - "3306:3306"
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p$$MYSQL_ROOT_PASSWORD"]
      interval: 10s
      timeout: 5s
      retries: 10
    networks:
      - clda-net

  redis:
    image: redis:7-alpine
    container_name: clda-redis
    ports:
      - "6379:6379"
    networks:
      - clda-net

  clda-admin:
    build:
      context: ..
      dockerfile: docker/Dockerfile.admin
    container_name: clda-admin
    environment:
      # MySQL
      SPRING_DATASOURCE_DRUID_MASTER_URL: jdbc:mysql://mysql:3306/ry-xiaozhi?useUnicode=true&characterEncoding=utf8mb4&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8
      SPRING_DATASOURCE_DRUID_MASTER_USERNAME: root
      SPRING_DATASOURCE_DRUID_MASTER_PASSWORD: clda123
      # Redis
      SPRING_DATA_REDIS_HOST: redis
      # 文件上传路径
      CLDA_PROFILE: /opt/clda/uploadPath
      # OCR 配置
      CLDA_OCR_TESSDATA__PATH: /usr/share/tesseract-ocr/5/tessdata
      CLDA_OCR_LANGUAGE: chi_sim+eng
      # 图片增强服务
      CLDA_ENHANCE_URL: http://clda-enhance:8090
      CLDA_ENHANCE_ENABLED: "true"
      # LLM（初始值，可在管理端 AI 配置页面修改）
      LLM_API_KEY: sk-placeholder
      LLM_BASE_URL: https://api.openai.com
      LLM_MODEL: gpt-4o-mini
      # JNA library path for Tesseract
      JAVA_OPTS: "-Djna.library.path=/usr/lib/x86_64-linux-gnu"
    ports:
      - "8080:8080"
    volumes:
      - upload-data:/opt/clda/uploadPath
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    networks:
      - clda-net

  clda-chat:
    build:
      context: ..
      dockerfile: docker/Dockerfile.chat
    container_name: clda-chat
    environment:
      # ASR 模型路径
      MODEL_ASR_SENSE__VOICE_MODEL__DIR: /opt/models/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17
      # Feign 调用 admin 服务
      CHAT_SERVER_MANAGE__URL: http://clda-admin:8080
      # LLM 配置（语音对话用）
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
      - clda-admin
    networks:
      - clda-net

  clda-enhance:
    build:
      context: ../clda-enhance
      dockerfile: Dockerfile
    container_name: clda-enhance
    environment:
      UPLOAD_BASE_PATH: /opt/clda/uploadPath
    ports:
      - "8090:8090"
    volumes:
      - upload-data:/opt/clda/uploadPath
    networks:
      - clda-net

  nginx:
    image: nginx:alpine
    container_name: clda-nginx
    ports:
      - "80:80"
    volumes:
      - ../clda-ui/dist:/usr/share/nginx/html:ro
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
    depends_on:
      - clda-admin
    networks:
      - clda-net

volumes:
  mysql-data:
  upload-data:

networks:
  clda-net:
    driver: bridge
```

### 5.3 Dockerfile.admin

```dockerfile
FROM eclipse-temurin:21-jre

# 安装 Tesseract OCR + 中文语言包
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      tesseract-ocr \
      tesseract-ocr-chi-sim \
      tesseract-ocr-eng \
      libtesseract-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY clda-admin/target/clda-admin.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
```

> **关键**：Admin 容器需要安装 `tesseract-ocr` 和 `tesseract-ocr-chi-sim`，因为 OCR 处理在 Admin 服务中执行。OpenCV 原生库由 JavaCV `opencv-platform` Maven 依赖自动提供。

### 5.4 Dockerfile.chat

```dockerfile
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY clda-chat/target/clda-chat.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 5.5 Dockerfile.enhance（已存在于 clda-enhance/Dockerfile）

```dockerfile
FROM python:3.11-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends libgl1 libglib2.0-0 && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .
EXPOSE 8090

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8090"]
```

### 5.6 nginx.conf

```nginx
server {
    listen 80;
    server_name localhost;

    client_max_body_size 100m;

    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /prod-api/ {
        proxy_pass http://clda-admin:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_connect_timeout 300s;
        proxy_read_timeout 300s;
    }

    # 文件上传代理（大文件支持）
    location /prod-api/common/upload {
        proxy_pass http://clda-admin:8080/common/upload;
        proxy_set_header Host $host;
        client_max_body_size 200m;
        proxy_read_timeout 600s;
    }

    # WebSocket 代理
    location /clda/ {
        proxy_pass http://clda-chat:8082;
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
2. clda-enhance 安装 Python 依赖（首次约 3-5 分钟）
3. Admin 服务启动 + Tesseract 库加载（约 20 秒）
4. Chat 服务加载 ASR 模型（约 30 秒）

查看日志：
```bash
docker-compose logs -f clda-admin     # Admin 服务日志
docker-compose logs -f clda-chat      # Chat 服务日志
docker-compose logs -f clda-enhance   # 图片增强服务日志
```

### 6.2 验证

| 服务 | 地址 | 说明 |
|------|------|------|
| 管理后台 | http://localhost | 默认账号 admin / admin123 |
| Admin API | http://localhost:8080 | REST API |
| Chat WebSocket | ws://localhost:8082/clda/v1 | 语音对话 |
| 图片增强 | http://localhost:8090/health | 应返回 `{"status":"ok"}` |

### 6.3 首次配置

1. 登录管理后台 → 起重设备管理 → **AI配置**
2. 设置 LLM API 密钥和地址（兼容 OpenAI 协议的任意服务）
3. 点击「测试连接」确认 LLM 连通性
4. OCR 和图片增强默认已启用，无需额外配置

### 6.4 功能验证

1. **OCR 测试**：起重设备管理 → 数据采集 → 选择设备和分类 → 上传图片
   - 文件状态应从「待处理」→「处理中」→「已完成」
   - 点击文件名打开右侧抽屉查看原始图和预处理结果
2. **LLM 结构化测试**：底部 AI 助手栏 → 数据服务AI助手
   - 切换分类 Tab，查看模板字段列
   - 已处理文件的结构化字段应有值
3. **图片增强测试**：http://localhost:8090/health 返回 OK 即可
   - 上传的图片会自动尝试增强，失败则降级跳过

### 6.5 停止服务

```bash
docker-compose down        # 停止并删除容器
docker-compose down -v     # 同时删除数据卷（会丢失数据库和上传文件）
```

## 7. 数据处理流水线说明

### 文件上传后自动处理流程

```
上传文件
  │
  ├─ 图片(jpg/png/bmp)
  │    ├─ [1] 预处理: 灰度化 → 高斯去噪 → 自适应二值化 → 锐化 → 自动纠偏 (OpenCV)
  │    ├─ [2] 超分辨率增强: 调用 clda-enhance 服务 (Real-ESRGAN, 可选)
  │    ├─ [3] OCR: Tesseract 识别文字 (chi_sim + eng)
  │    └─ [4] LLM 结构化: 按分类模板提取字段 (Spring AI)
  │
  ├─ PDF
  │    ├─ 原生文字 PDF: PDFBox 直接提取文字 → LLM 结构化
  │    └─ 扫描件 PDF: 逐页渲染 300DPI → 预处理 → OCR → LLM 结构化
  │
  ├─ Word (doc/docx): Apache POI 提取文字 → LLM 结构化
  └─ Excel (xls/xlsx): Apache POI 解析表格 → LLM 结构化
```

### 处理状态

| 状态 | 含义 |
|------|------|
| NONE | 待处理 |
| PROCESSING | 处理中 |
| DONE | 已完成 |
| FAILED | 处理失败（不影响其他文件） |

LLM 结构化失败不会导致整个文件标记为 FAILED，OCR 文本仍然保留。

## 8. 常见问题

### MySQL 中文乱码
确保 docker-compose.yml 中 MySQL 的 `command` 包含 `--collation-server=utf8mb4_unicode_ci`（注意是 `unicode_ci` 而非 `general_ci`）。如果已有数据乱码，需删除数据卷重新初始化：
```bash
docker-compose down -v
docker-compose up -d
```

### OCR 识别率低
- 确认 Tesseract 中文语言包已安装：进入 admin 容器 `docker exec clda-admin tesseract --list-langs`，应包含 `chi_sim`
- 在管理端 AI 配置页面检查 OCR 参数（引擎模式建议 1-LSTM，页面分割模式建议 3-全自动）
- 上传清晰扫描件效果最佳，手机拍照的倾斜/模糊图片识别率会下降

### 图片增强服务不可用
- 检查 clda-enhance 容器是否正常运行：`docker logs clda-enhance`
- Real-ESRGAN 模型首次加载需要下载权重（约 60MB），需要网络访问
- 增强失败不影响 OCR 流程，系统会自动降级使用预处理图片

### LLM 结构化不工作
- 在管理端 AI 配置页面确认 API 密钥已设置且不是 `sk-placeholder`
- 点击「测试连接」按钮验证连通性
- 检查结构化模板页面确认模板已启用

### Admin 容器启动报 Tesseract 错误
确保 Dockerfile.admin 中安装了 `tesseract-ocr` 和 `libtesseract-dev`，且环境变量 `JAVA_OPTS` 包含 `-Djna.library.path=/usr/lib/x86_64-linux-gnu`。

### Chat 服务连接 Admin 失败
检查环境变量 `CHAT_SERVER_MANAGE__URL` 是否设置为 `http://clda-admin:8080`。Spring Boot 环境变量中 `.` 用 `_` 替代，`-` 用 `__`（双下划线）替代。

### 上传大文件超时
Nginx 默认限制请求体大小。已在 nginx.conf 中设置 `client_max_body_size 100m`，如需更大文件请调整此值和 `proxy_read_timeout`。

### ESP32 设备连接
- OTA 地址：`http://你的Windows主机IP:8080/api/ota`
- 设备会自动获取 WebSocket 地址：`ws://你的Windows主机IP:8082/clda/v1`
