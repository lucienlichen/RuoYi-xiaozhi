# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RuoYi-Xiaozhi is a Java backend for the [xiaozhi-esp32](https://github.com/78/xiaozhi-esp32) smart hardware project. It provides a WebSocket-based communication server with AI capabilities (ASR, TTS, LLM) built on the RuoYi-Vue management framework.

## Build & Run Commands

```bash
# Build all modules
mvn clean install

# Build skipping tests
mvn clean install -DskipTests

# Run tests
mvn clean test
mvn test -pl xiaozhi-chat -Dtest=FFMpegResampleTests    # single test class

# Start admin server (port 8080)
mvn spring-boot:run -pl xiaozhi-admin

# Start chat WebSocket server (port 8082)
mvn spring-boot:run -pl xiaozhi-chat

# Frontend (Vue 3 + Vite)
cd xiaozhi-ui
npm install --registry=https://registry.npmmirror.com
npm run dev          # dev server (port 80)
npm run build:prod   # production build
```

## Architecture

### Two Separate Applications

1. **xiaozhi-admin** — HTTP/REST admin server (Tomcat, port 8080). Manages devices, users, agents, configs via web UI.
2. **xiaozhi-chat** — Standalone WebSocket server (Java-WebSocket, port 8082, NOT Servlet-based). Handles real-time device communication, voice processing, and AI conversations.

### Module Dependency Graph

```
xiaozhi-common          ← shared utilities, security, Redis, MyBatis, JWT
  └─ xiaozhi-system     ← domain entities (Device, Agent, User), services, mappers
      └─ xiaozhi-framework  ← web infrastructure, security config, interceptors, AOP
          ├─ xiaozhi-admin   ← REST controllers, entry point: XiaozhiApplication
          ├─ xiaozhi-quartz  ← scheduled tasks (Quartz)
          └─ xiaozhi-generator ← code generation
  └─ xiaozhi-feign      ← OpenFeign clients for inter-service calls
      └─ xiaozhi-chat   ← WebSocket server, entry point: XiaozhiChatApplication
          └─ sherpa-onnx ← native JNI bindings for SenseVoice ASR model
```

### Key Packages in xiaozhi-chat

- `connect/` — `ChatServerWebSocket` (server lifecycle) and `ChatServerHandler` (per-connection message handling, the core orchestrator)
- `providers/asr/` — Speech recognition (SenseVoice via ONNX)
- `providers/tts/` — Text-to-speech (EdgeTTS, VolcEngine)
- `core/` — VAD (Silero), audio buffers, payload handling
- `opus/` — Opus codec encoder/decoder
- `tool/` — LLM function calling framework

### Key Packages in xiaozhi-admin

- `web/controller/api/` — OTA and chat API endpoints (device-facing)
- `web/controller/intellect/` — Device & agent management (admin-facing)
- `web/controller/system/` — User, role, dict, config management

## Tech Stack

- **Java 21** (Azul Zulu required — uses virtual threads)
- **Spring Boot 3.3.11** + Spring Cloud 2023.0.5
- **MyBatis Plus 3.5.10** (ORM) + Druid (connection pool)
- **Spring AI 1.0.0** (LLM integration, OpenAI-compatible protocol)
- **Java-WebSocket 1.6.0** (lightweight, non-Servlet WebSocket)
- **JavaCV + FFmpeg 7.1** (audio processing)
- **LWJGL + Opus** (audio codec)
- **ONNX Runtime** (model inference for ASR)
- **MySQL 8.0+**, **Redis** (required runtime dependencies)
- **Vue 3 + Vite + Element Plus** (frontend in `xiaozhi-ui/`)

## Configuration Files

- `xiaozhi-admin/src/main/resources/application.yml` — admin server config
- `xiaozhi-admin/src/main/resources/application-druid.yml` — database connection (master DB: `ry-xiaozhi`)
- `xiaozhi-chat/src/main/resources/application.yml` — chat server config (LLM keys, ASR model path, TTS config)
- `sql/ry-xiaozhi-20250615.sql` — database initialization script

## Important Notes

- The two applications (admin and chat) run as separate processes on different ports
- WebSocket chat server uses lightweight Java-WebSocket library, not Spring's WebSocket support
- Package naming inconsistency: admin uses `com.rouyi.xiaozhi`, chat uses `com.ruoyi.xiaozhi.chat`
- Default admin credentials: admin / admin123
- Druid monitoring console: `/druid/*` (admin/123456)
- ESP32 devices connect via OTA endpoint (`/api/ota`) to discover WebSocket address
