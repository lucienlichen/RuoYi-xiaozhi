# 首版发布审查建议

审查日期：2026-04-02

目的：基于当前仓库状态，从交互体验、上线稳定性、安全性、工程质量和发布可行性几个维度，整理首版发布前的修改建议。

说明：本次仅整理评审结论，不涉及业务代码修改。

## 一、总体判断

当前版本已经具备可演示的产品形态，前后端主链路基本可构建，部分核心业务页面也已经具备实际功能。

但如果按“第一个正式版本发布”来要求，当前状态更接近“可内部联调/可灰度体验”，还不适合直接对外发布。主要原因不是单点 Bug，而是以下几类问题同时存在：

1. 发布配置仍明显处于开发态，存在密钥、默认凭据、绝对路径、固定密钥等问题。
2. 部分核心交互是“看起来可用”，但链路没有真正闭合。
3. 上传、异步处理、文件存储清理等后台流程还不够严谨。
4. 语音、OTA、人脸识别等依赖外部环境的功能缺少发布级配置抽象。
5. 自动化测试和回归保障明显不足。

建议结论：不建议以当前状态直接作为正式首版发布，建议至少完成本文中的 P0 和 P1 项再进入发布流程。

## 二、P0：首版发布前必须处理

### 1. 配置、密钥与环境隔离

当前仓库里仍存在明显不适合发布的开发态配置：

- `clda-chat/src/main/resources/application.yml:10-32`
  - 直接提交了模型 API Key、TTS appid 和 access-token。
  - ASR 模型目录使用了开发机绝对路径。
- `clda-admin/src/main/resources/application.yml:10-31`
  - `profile` 仍是 `D:/clda/uploadPath`
  - MinIO 默认值仍是 `minioadmin/minioadmin`
- `clda-admin/src/main/resources/application.yml:53-56`
  - 日志级别仍为 `com.clda: debug`
- `clda-admin/src/main/resources/application.yml:83-86`
  - `devtools.restart.enabled: true`
- `clda-admin/src/main/resources/application.yml:119-126`
  - token secret 仍是固定明文默认值
- `clda-system/src/main/java/com/clda/intellect/service/impl/AiConfigService.java:37-58`
  - AI 服务默认地址和默认密钥仍偏开发占位逻辑

发布建议：

- 所有第三方凭据改为环境变量或独立配置中心注入。
- 明确拆分 `dev` / `test` / `prod` 配置。
- 移除默认可用的开发凭据。
- 禁止生产包包含开发机绝对路径。
- 生产环境关闭 `debug` 日志和 `devtools`。
- token secret 改为部署期注入，并建立更换机制。

### 2. 语音、OTA 与部署地址不能硬编码

当前语音和设备接入链路对部署环境假设过强：

- `clda-system/src/main/java/com/clda/intellect/service/impl/DeviceServiceImpl.java:109-130`
  - OTA 返回的 WebSocket 地址是 `ws://<本机IP>:8082/clda/v1`
- `clda-ui/src/composables/useVoiceChat.js:367-394`
  - 前端默认连接 `ws://{location.hostname}:8082/clda/v1`
- `clda-ui/src/views/robot/login.vue:188-190`
  - 人脸模型和识别能力依赖公网 CDN
- `clda-ui/src/views/robot/login.vue:271-299`
  - 人脸模型与人脸数据拉取失败时只有降级，没有发布级兜底策略

这会导致以下问题：

- HTTPS 部署时会遇到 mixed content。
- 反向代理、网关转发、非 8082 端口场景容易失效。
- 内网、离线、客户现场网络受限时，人脸登录可能直接不可用。
- OTA 回写地址不适用于多网卡或容器/NAT 环境。

发布建议：

- WebSocket 对外地址由配置项统一控制，不要运行时拼接本机 IP。
- 前端和设备端统一走可配置的公开接入地址。
- 人脸模型和关键静态依赖本地化，避免强依赖公网 CDN。
- 明确“语音不可用/模型不可达/人脸能力不可用”时的产品降级路径。

### 3. 上传链路的成功语义与失败处理不可靠

上传设备数据时，后端先创建记录，再逐个上传文件；单文件上传失败只记日志，不回滚：

- `clda-system/src/main/java/com/clda/intellect/service/impl/EquipmentDataServiceImpl.java:48-86`

前端则直接提示：

- `clda-ui/src/views/intellect/equipdata/index.vue:498-506`
  - “上传成功，系统正在自动处理...”

这意味着：

- 用户可能看到“上传成功”，但实际上部分文件甚至全部文件都没入库。
- 数据记录可能处于 `PROCESSING`，但没有对应文件。
- 前端和后端对“成功”的定义不一致。

发布建议：

- 上传接口返回“成功文件数 / 失败文件数 / 失败原因”。
- 若全部失败，应直接返回失败，不应创建悬空记录。
- 若部分成功，应前端明确提示部分失败。
- 必要时为上传记录增加事务边界或补偿机制。

### 4. 文件存储和临时文件清理不完整

当前删除数据库记录时，没有同步清理对象存储：

- `clda-system/src/main/java/com/clda/intellect/service/impl/EquipmentDataServiceImpl.java:95-108`

扫描版 PDF 处理过程中，部分中间文件也没有完整清理：

- `clda-system/src/main/java/com/clda/intellect/service/impl/DataProcessingServiceImpl.java:211-245`

风险：

- MinIO 对象持续堆积。
- 临时目录残留大量 OCR 中间产物。
- 生产运行时间一长后，磁盘和对象存储成本失控。

发布建议：

- 删除数据记录时同步删除原文件、预处理文件、增强文件。
- PDF OCR 过程中的 `_ocr_input`、增强结果等中间文件统一清理。
- 为异步处理引入失败补偿和定时清理任务。

## 三、P1：上线前强烈建议补齐

### 5. 关键交互链路没有真正闭合

桌面端“查看结构化数据”会把文件传给 `DataServicePanel`：

- `clda-ui/src/layout/AppLayout.vue:32-37`
- `clda-ui/src/layout/AppLayout.vue:102-110`

但 `DataServicePanel` 中 `initialFile` 实际没有被消费：

- `clda-ui/src/views/intellect/components/DataServicePanel.vue:138-140`
- `clda-ui/src/views/intellect/components/DataServicePanel.vue:227-249`

这会导致：

- 用户点击“查看”，但面板未必定位到对应文件。
- 交互意图与结果不一致。

同时，同一个 assistant 在不同端的实现不一致：

- 桌面端 `hazard_check` 接的是实际页面：
  - `clda-ui/src/layout/AppLayout.vue:38-47`
- 机器人端 `hazard_check` 仍是占位页：
  - `clda-ui/src/views/robot/app.vue:13-20`

发布建议：

- “查看结构化数据”必须支持按文件直达。
- 同一功能在不同入口应保持一致结果。
- 未完成功能不要放在正式主入口中，至少要有明确的“不可用”策略，而不是混合实功能和占位页。

### 6. 数据导入页有多处逻辑偏差

当前数据导入页有几个典型问题：

- 年份模式 UI 已存在，但查询层只支持精确 `dataDate`
  - 前端：`clda-ui/src/views/intellect/equipdata/index.vue:49-59`
  - 后端：`clda-system/src/main/java/com/clda/intellect/mapper/EquipmentDataMapper.java:17-23`
- 拖拽上传逻辑没有真正把拖入文件绑定给上传组件
  - `clda-ui/src/views/intellect/equipdata/index.vue:431-437`
- “本月 X 条”统计的是日期数量，不是数据记录数量
  - `clda-ui/src/views/intellect/equipdata/index.vue:323-328`

发布建议：

- 年份模式要么真正支持按年查询，要么先移除入口。
- 拖拽上传改成真实可用的 `FileList -> FormData` 上传流程。
- 所有统计口径统一，避免日期数、文件数、记录数混淆。

### 7. 前端包体过大，分包策略失效

实际构建已通过，但构建结果存在明显发布风险：

- 静态壳层直接引入多个业务大页面
  - `clda-ui/src/layout/AppLayout.vue:81-90`
  - `clda-ui/src/views/robot/app.vue:53-58`
- 路由层又对同一批页面使用 `import.meta.glob`
  - `clda-ui/src/store/modules/permission.js:8-9`
- `vite` 构建配置允许 chunk warning 到 2000KB
  - `clda-ui/vite.config.js:29-42`

本次构建结果中，已经出现超大 chunk 和动态导入失效警告：

- `dist/static/js/index-CP2w-wjX.js` 约 1.04MB
- `dist/static/js/index-DWuxfCM5.js` 约 4.65MB

发布建议：

- 业务页不要在壳层静态引入，改为真正按路由或按面板懒加载。
- 为知识预览、法规预览、人脸识别、编辑器等重模块拆独立 chunk。
- 将首屏核心功能与后台管理能力分离加载。

### 8. 测试与回归保障明显不足

仓库中当前可见的测试文件只有：

- `clda-chat/src/test/java/com/ruoyi/xiaozhi/chat/DeviceClientTests.java`
- `clda-chat/src/test/java/com/ruoyi/xiaozhi/chat/FFMpegResampleTests.java`

核心业务链路几乎没有自动化覆盖：

- 登录与权限
- 设备激活
- 数据上传与 OCR 流水线
- 语音 WebSocket 连接与消息协议
- 法规/知识文件预览
- 设备数据删除后的对象清理

发布建议：

- 至少补一组首版回归测试：
  - 后端接口冒烟
  - 核心上传流程
  - 语音协议连通性
  - 关键页面手工回归清单
- 发布前形成固定 checklist，而不是依赖人工记忆。

## 四、P2：建议首版前顺手修正的体验问题

### 9. 首页与品牌展示不够完整

- 后台首页仍是纯文本：
  - `clda-ui/src/views/index.vue:1-13`
- 顶部标题文字存在明显错误：
  - `clda-ui/src/layout/components/AppNavbar.vue:4`

首版用户的第一印象会直接受影响。

建议：

- 首页至少补成“系统概览 / 快速入口 / 运行状态”页面。
- 修正品牌文案和命名一致性。

### 10. 公开资源暴露范围需要收紧

当前配置对跨域和静态资源开放比较宽：

- `clda-framework/src/main/java/com/clda/framework/config/ResourcesConfig.java:57-70`
  - `addAllowedOriginPattern("*")`
- `clda-framework/src/main/java/com/clda/framework/config/SecurityConfig.java:114-119`
  - `/profile/**`、`/minio/**`、`/common/minio/**` 直接匿名开放

如果后续是公网环境，这会带来不必要的暴露面。

建议：

- 根据部署域名收紧 CORS。
- 明确公开文件和受控文件边界。
- 对需要匿名访问的文件做最小化开放，不建议全量裸代理。

### 11. 测试页和实验性能力不要进入正式产物

当前仓库存在公开测试页：

- `clda-ui/public/voice-test.html:1`

这类页面在开发阶段有价值，但不适合作为正式发布产物的一部分。

建议：

- 生产构建中移除测试页。
- 若必须保留，至少做访问限制或仅在开发环境启用。

## 五、已验证项

本次审查中做过的构建验证：

- 后端编译：
  - `mvn -q -DskipTests compile`
  - 结果：通过
- 前端打包：
  - `npm run build:prod`
  - 结果：通过
  - 备注：存在多条动态导入失效警告和超大 chunk 警告

未执行项：

- 未运行完整自动化测试
- 未做真实浏览器交互回归
- 未做真实设备联调
- 未做生产环境部署验证

## 六、推荐整改顺序

建议按下面顺序推进首版发布准备：

1. 先处理发布配置、密钥、路径、部署地址问题。
2. 再修上传链路、文件清理、结构化查看直达等关键闭环问题。
3. 然后收敛未完成功能入口，避免正式版出现占位功能。
4. 最后补回归测试、分包优化和首页体验。

## 七、结论

当前版本已经具备首版产品的基础框架，但距离“可正式发布”还差一轮发布级收口。

如果只能抓重点，最优先需要完成的是：

- 配置与密钥治理
- 语音/OTA 部署地址可配置化
- 上传成功语义与失败处理修正
- 文件和中间产物清理
- 关键交互链路闭合
- 最小回归测试补齐

完成这些之后，再进入首版发布，会明显更稳。
