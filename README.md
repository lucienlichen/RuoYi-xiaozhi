# CLDA-3A — 起重装备全生命周期数据 AI 智能体

> **C**rane **L**ifecycle **D**ata **A**I — 面向起重机械行业的数据采集、智能识别与知识管理平台

CLDA-3A 将工业现场的纸质报告、手写检测单、设备铭牌照片等非结构化数据，通过 **OCR + AI 结构化 + 知识图谱** 转化为可检索、可分析的数字资产。同时集成 ESP32 智能终端，提供 **语音交互 + 人脸识别** 的现场操作体验。

---

## 系统架构

```
┌─────────────┐   HTTP/REST   ┌──────────────┐   Feign    ┌──────────────┐
│   clda-ui   │ ◄───────────► │  clda-admin  │ ◄────────► │  clda-chat   │
│  Vue3+Vite  │               │   :8080      │            │   :8082      │
│  桌面端/机器人端 │             │  管理+数据处理  │            │  WebSocket   │
└─────────────┘               └──────────────┘            └──────────────┘
                                    │                           │
                              ┌─────┴─────┐              ┌─────┴─────┐
                              │ MySQL     │              │ SenseVoice│
                              │ Redis     │              │ Silero VAD│
                              │ MinIO     │              │ LLM/TTS   │
                              └───────────┘              └───────────┘
```

| 应用 | 端口 | 职责 |
|------|------|------|
| **clda-admin** | 8080 | 管理后台 + 数据处理流水线（OCR / 增强 / LLM 结构化） |
| **clda-chat** | 8082 | WebSocket 服务（ESP32 设备通信、语音对话、ASR/TTS） |
| **clda-ui** | 80 | 前端（桌面业务端 + 8 寸机器人触屏端） |

## 核心功能

### 数据采集与智能处理
- 支持照片、PDF、Word、Excel 多格式上传
- 图片自动纠偏、去噪、CLAHE 增强 → AI 超分辨率 → OCR 文字识别
- 扫描件 PDF 自动检测并逐页 OCR
- LLM 结构化提取（双策略：LLM 优先 + 正则兜底）
- 按设备 × 数据分类 × 日期三维组织数据

### 知识与法规管理
- 起重机械安全知识库（章节树 + 全文检索）
- 法规标准文库（自动 PDF 解析入库）
- 典型隐患案例库
- 64 项专项隐患排查清单

### 智能终端交互
- ESP32 设备 OTA 自动发现 + 激活绑定
- 实时语音对话（SenseVoice ASR + 火山/Edge TTS）
- 人脸识别登录（本地模型，支持离线内网）
- 语音意图识别 → 导航到对应 AI 助手

### 双端 UI
- **桌面业务端**：数据上传 + 6 个 AI 助手面板 + 设备侧边栏
- **机器人端**：8 寸竖屏触屏优化 + 语音交互面板

## 技术栈

| 层 | 技术 |
|------|------|
| 后端 | Java 21 (Azul Zulu) · Spring Boot 3.3 · Spring AI 1.0 · MyBatis Plus 3.5 |
| 通信 | Java-WebSocket 1.6（轻量级，非 Servlet） · OpenFeign |
| AI/ML | SenseVoice (ONNX Runtime) · Silero VAD · Tesseract OCR · Spring AI (OpenAI 协议) |
| 音频 | JavaCV + FFmpeg 7.1 · LWJGL + Opus |
| 存储 | MySQL 8.0 · Redis 7 · MinIO |
| 前端 | Vue 3 · Vite · Element Plus · face-api.js (本地化) |
| 部署 | Docker Compose · nginx (WebSocket 反代) |

## 快速开始

### 环境要求

- **Java 21** (Azul Zulu，需要虚拟线程支持)
- **Maven 3.8+**
- **Node.js 18+**
- **MySQL 8.0+** · **Redis 7+**

### 本地开发

```bash
# 1. 克隆
git clone https://github.com/lucienlichen/CLDA-3A.git
cd CLDA-3A

# 2. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS clda CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p clda < sql/clda-20250615.sql

# 3. 下载 ASR 模型（~1GB）
wget https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17.tar.bz2
tar xf sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17.tar.bz2

# 4. 编译 & 启动后端
mvn clean install -DskipTests
mvn spring-boot:run -pl clda-admin    # Admin :8080
mvn spring-boot:run -pl clda-chat     # Chat  :8082

# 5. 启动前端
cd clda-ui
npm install --registry=https://registry.npmmirror.com
npm run dev                            # :80
```

默认账号：`admin` / `admin123`，登录后自动跳转到业务端。

> 开发环境无需配置任何环境变量，所有配置有安全的本地默认值。Swagger UI: http://localhost:8080/swagger-ui.html

### 生产部署（Docker）

```bash
cd docker
cp .env.example .env
# 编辑 .env，设置所有 CHANGE_ME 值

cd ..
mvn clean install -DskipTests
cd clda-ui && npm install && npm run build:prod && cd ..
cd docker && docker compose up -d
```

生产环境通过 `SPRING_PROFILES_ACTIVE=prod` 自动激活安全配置：
- 启动时校验 JWT 密钥和 MinIO 凭据，缺配即拒绝启动
- Swagger / Druid 控制台禁用，文件端点需认证
- CORS 限制为指定域名，WebSocket 自适应 wss://

详细部署指南见 [docs/deployment-checklist.md](docs/deployment-checklist.md)。

## 项目结构

```
CLDA-3A/
├── clda-admin/          # Admin 服务（REST API + 数据处理流水线）
├── clda-chat/           # Chat 服务（WebSocket + 语音交互）
├── clda-common/         # 公共模块（安全、Redis、MinIO、工具类）
├── clda-system/         # 领域模型 + 业务服务（设备、数据、OCR、LLM）
├── clda-framework/      # Web 基础设施（Security、CORS、拦截器）
├── clda-feign/          # 服务间 Feign 调用
├── clda-quartz/         # 定时任务
├── clda-generator/      # 代码生成器
├── clda-enhance/        # Python 图片增强微服务
├── clda-ui/             # Vue 3 前端（桌面端 + 机器人端）
├── sherpa-onnx/         # SenseVoice ASR JNI 绑定
├── docker/              # Docker 编排 + nginx + 环境变量模板
├── sql/                 # 数据库初始化脚本
└── docs/                # 部署文档
```

## 环境变量参考

| 变量 | 必填 | 说明 |
|------|------|------|
| `SPRING_PROFILES_ACTIVE` | prod 环境 | 设为 `prod` 激活生产配置 |
| `TOKEN_SECRET` | prod | JWT 签名密钥（≥32 字符） |
| `MYSQL_ROOT_PASSWORD` | prod | 数据库密码 |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | prod | MinIO 凭据 |
| `CORS_ALLOWED_ORIGINS` | prod | 前端域名 |
| `CHAT_WS_URL` | prod | ESP32 设备 WebSocket 地址 |
| `LLM_API_KEY` / `LLM_BASE_URL` / `LLM_MODEL` | 可选 | Admin 端 LLM（数据结构化） |
| `CHAT_CLIENT_APIKEY` | 可选 | Chat 端 LLM（语音对话） |
| `VOLC_TTS_APPID` / `VOLC_TTS_TOKEN` | 可选 | 火山引擎 TTS |

完整模板见 [docker/.env.example](docker/.env.example)。

## 致谢

- [xiaozhi-esp32](https://github.com/78/xiaozhi-esp32) — ESP32 智能硬件项目
- [xiaozhi-esp32-server](https://github.com/xinnan-tech/xiaozhi-esp32-server) — 服务端参考实现
- [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue) — 管理框架基座

## License

[MIT](LICENSE)
