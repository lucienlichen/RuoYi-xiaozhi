# 人脸识别功能设计文档

## 1. 功能概述

在语音交互之前，通过浏览器摄像头识别用户身份。识别成功后，系统自动发起语音打招呼（如"李明，你好，需要什么帮助吗？"），然后进入正常语音对话模式。

## 2. 整体流程

```
页面加载
    ↓
加载人脸识别模型（face-api.js）
    ↓
打开摄像头 → 实时检测人脸 → 提取特征向量
    ↓
与已注册人脸匹配
    ↓ 匹配成功
获取人员姓名（如"李明"）
    ↓
建立 WebSocket 连接（URL 携带 username=李明）
    ↓
服务端加载个性化提示词（${username} → 李明）
    ↓
服务端发送打招呼 TTS："李明，你好，需要什么帮助吗？"
    ↓
进入正常语音对话模式（按住说话）
```

## 3. 技术选型

### 人脸识别：face-api.js

- 基于 TensorFlow.js，纯浏览器端运行，无需后端 GPU
- CDN 引入：`https://cdn.jsdelivr.net/npm/face-api.js`
- 使用模型：
  - `tinyFaceDetector` — 轻量人脸检测（约 190KB）
  - `faceLandmark68TinyNet` — 人脸关键点（约 80KB）
  - `faceRecognitionNet` — 人脸特征提取（约 6.2MB）
- 识别精度：欧氏距离 < 0.6 判定为同一人

### 人脸数据存储

| 阶段 | 方案 | 说明 |
|------|------|------|
| 演示阶段 | localStorage | 前端直接存储，零后端改动 |
| 生产阶段 | 后端 API + 数据库 | 新增 tb_face 表，管理后台注册 |

## 4. 前端改动

### 4.1 文件：`voice-test.html`

#### UI 状态机

```
FACE_SCAN → CONNECTING → READY
   ↑                       │
   └───── 断开重连 ─────────┘
```

- **FACE_SCAN**：显示摄像头画面 + 人脸检测框 + 注册按钮
- **CONNECTING**：隐藏摄像头，显示"连接中..."
- **READY**：显示麦克风按钮，正常语音交互

#### 新增 HTML 元素

```html
<!-- 人脸识别区域（FACE_SCAN 状态显示） -->
<div id="facePanel">
  <video id="video" autoplay muted></video>
  <canvas id="overlay"></canvas>
  <div id="faceStatus">正在识别...</div>
  <button id="registerBtn">注册人脸</button>
</div>
```

#### 核心 JS 逻辑

```javascript
// 1. 加载模型
await faceapi.nets.tinyFaceDetector.loadFromUri(MODEL_URL);
await faceapi.nets.faceLandmark68TinyNet.loadFromUri(MODEL_URL);
await faceapi.nets.faceRecognitionNet.loadFromUri(MODEL_URL);

// 2. 打开摄像头
const stream = await navigator.mediaDevices.getUserMedia({ video: true });
video.srcObject = stream;

// 3. 实时检测匹配
const detection = await faceapi
  .detectSingleFace(video, new faceapi.TinyFaceDetectorOptions())
  .withFaceLandmarks(true)
  .withFaceDescriptor();

// 4. 匹配已注册人脸
const faceMatcher = new faceapi.FaceMatcher(registeredFaces, 0.6);
const match = faceMatcher.findBestMatch(detection.descriptor);

// 5. 匹配成功 → 连接 WebSocket
if (match.label !== 'unknown') {
  const username = match.label;
  const wsUrl = `ws://host:8082/xiaozhi/v1?device_mac=XX&username=${encodeURIComponent(username)}`;
  connectWebSocket(wsUrl);
}
```

#### 人脸注册功能

```javascript
// 拍照 → 输入姓名 → 保存到 localStorage
function registerFace(name, descriptor) {
  const faces = JSON.parse(localStorage.getItem('registered_faces') || '[]');
  faces.push({ name, descriptor: Array.from(descriptor) });
  localStorage.setItem('registered_faces', JSON.stringify(faces));
}
```

## 5. 后端改动

### 5.1 改动文件清单

| 文件 | 改动 | 说明 |
|------|------|------|
| `WebSocketUtils.java` | 新增方法 | 从查询参数提取 `username` |
| `DeviceClient.java` | 修改接口 | info 方法增加可选 `username` 参数 |
| `ApiDeviceController.java` | 修改方法 | 透传 `username` 参数 |
| `IDeviceService.java` | 新增重载 | `detail(macAddress, username)` |
| `DeviceServiceImpl.java` | 修改方法 | 支持 username 覆盖 |
| `DeviceDetailVo.java` | 新增字段 | 增加 `username` 字段 |
| `ChatServerWebSocket.java` | 修改方法 | 握手时提取 username 并传递 |
| `ChatServerHandler.java` | 新增功能 | hello 后发送打招呼 TTS |

### 5.2 详细改动

#### 5.2.1 WebSocketUtils.java — 新增 username 提取

```java
/**
 * 获取用户名（来自人脸识别）
 */
public static String username(ClientHandshake clientHandshake) {
    UrlQuery urlQuery = UrlQuery.of(
        clientHandshake.getResourceDescriptor(), StandardCharsets.UTF_8);
    return Convert.toStr(urlQuery.get("username"), null);
}
```

#### 5.2.2 DeviceClient.java — 增加 username 参数

```java
@GetMapping("/info")
DeviceDetailVo info(@RequestParam("macAddress") String macAddress,
                    @RequestParam(value = "username", required = false) String username);
```

#### 5.2.3 ApiDeviceController.java — 透传参数

```java
@GetMapping("/info")
public AjaxResult info(@RequestParam String macAddress,
                       @RequestParam(required = false) String username) {
    return success(deviceService.detail(macAddress, username));
}
```

#### 5.2.4 DeviceServiceImpl.java — username 覆盖

```java
public DeviceDetailVo detail(String macAddress, String username) {
    Device device = deviceMapper.findByMacAddress(macAddress);
    Agent agent = agentMapper.selectById(device.getAgentId());

    // 优先使用传入的 username（来自人脸识别），否则用设备绑定的 username
    String finalUsername = StrUtil.isNotBlank(username) ? username : device.getUsername();

    result.setPrompt(buildPrompt(agent.getPrompt(), finalUsername));
    result.setUsername(finalUsername);  // 回传给 chat 服务
    return result;
}
```

#### 5.2.5 DeviceDetailVo.java — 新增字段

```java
@Data
public class DeviceDetailVo {
    // ... 现有字段 ...
    private String username;  // 新增：用于打招呼
}
```

#### 5.2.6 ChatServerWebSocket.java — 握手时传递 username

```java
@Override
public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(...) {
    String deviceId = WebSocketUtils.deviceId(request);
    String username = WebSocketUtils.username(request);  // 新增

    DeviceDetailVo deviceVo = deviceClient.info(deviceId, username);  // 传递 username
    conn.setAttachment(deviceVo);
    return builder;
}
```

#### 5.2.7 ChatServerHandler.java — hello 后发送打招呼 TTS

在 Builder 中新增 `greeting` 字段：

```java
private String greeting;

public Builder greeting(String greeting) {
    this.greeting = greeting;
    return this;
}
```

在 `sendHelloMessage()` 方法末尾添加打招呼逻辑：

```java
private void sendHelloMessage() {
    // ... 现有 hello 响应代码 ...

    // 发送打招呼语音
    if (CharSequenceUtil.isNotBlank(this.greeting)) {
        this.sendTTSMessage("start", null);
        this.sendTTSMessage("sentence_start", this.greeting);
        this.ttsProvider.submitText(this.greeting);
        // TTS 完成后会自动发送 sentence_end 和 stop
    }
}
```

在 `ChatServerWebSocket.onOpen()` 中构建 greeting：

```java
String username = deviceVo.getUsername();
String greeting = StrUtil.isNotBlank(username)
    ? username + "，你好，需要什么帮助吗？"
    : null;

conn.setAttachment(ChatServerHandler.builder(conn)
    // ... 现有配置 ...
    .greeting(greeting)
    .build());
```

## 6. 生产阶段扩展：后端人脸管理

### 6.1 数据库表

```sql
CREATE TABLE `tb_face` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '人员姓名',
  `descriptor` text NOT NULL COMMENT '人脸特征向量（JSON 数组）',
  `photo_url` varchar(256) DEFAULT NULL COMMENT '照片地址',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人脸注册表';
```

### 6.2 管理后台功能

- 人脸列表页（查看已注册人员）
- 注册人脸（上传照片 + 输入姓名，后端提取特征向量）
- 删除人脸
- API 接口：`GET /api/face/list` 返回所有注册人脸的特征向量供前端匹配

## 7. 验证步骤

1. **注册人脸**：打开页面 → 点击"注册人脸" → 对准摄像头 → 输入姓名 → 保存
2. **识别测试**：刷新页面 → 摄像头自动识别 → 显示"已识别：XXX"
3. **语音打招呼**：识别成功后自动连接 → 听到"XXX，你好，需要什么帮助吗？"
4. **语音对话**：按住麦克风说"介绍一下系统" → 验证回复包含个性化内容
5. **未注册人脸**：遮挡面部或用未注册人脸 → 应显示"未识别，请注册"

## 8. 注意事项

- face-api.js 模型首次加载约 6.5MB，建议部署时将模型文件放在 Nginx 静态目录下
- 浏览器需要 HTTPS 或 localhost 才能访问摄像头
- 人脸匹配阈值（欧氏距离 0.6）可根据实际效果调整，越小越严格
- WebSocket URL 中的中文 username 需要 `encodeURIComponent` 编码
- 服务端 Hutool 的 `UrlQuery` 已支持 UTF-8 解码，无需额外处理
