<template>
  <div class="app-container ai-config-page">
    <el-form ref="formRef" :model="form" label-width="140px" v-loading="loading">

      <!-- OCR 配置 -->
      <el-card class="config-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon :size="18" color="#003d99"><Document /></el-icon>
            <span>OCR识别配置</span>
          </div>
        </template>
        <el-form-item label="训练数据路径">
          <el-input v-model="form['ai.ocr.tessdataPath']" placeholder="/usr/local/share/tessdata" />
        </el-form-item>
        <el-form-item label="识别语言">
          <el-input v-model="form['ai.ocr.language']" placeholder="chi_sim+eng" />
          <div class="form-tip">多种语言用 + 连接，如 chi_sim+eng</div>
        </el-form-item>
        <el-form-item label="页面分割模式">
          <el-select v-model="form['ai.ocr.pageSegMode']" style="width: 100%">
            <el-option label="0 - 仅方向检测" value="0" />
            <el-option label="1 - 自动+方向检测" value="1" />
            <el-option label="3 - 全自动分割（推荐）" value="3" />
            <el-option label="6 - 单一文本块" value="6" />
            <el-option label="11 - 稀疏文本" value="11" />
          </el-select>
        </el-form-item>
        <el-form-item label="引擎模式">
          <el-select v-model="form['ai.ocr.engineMode']" style="width: 100%">
            <el-option label="0 - Legacy引擎" value="0" />
            <el-option label="1 - LSTM（推荐）" value="1" />
            <el-option label="2 - Legacy + LSTM" value="2" />
            <el-option label="3 - 默认" value="3" />
          </el-select>
        </el-form-item>
      </el-card>

      <!-- 图片增强配置 -->
      <el-card class="config-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon :size="18" color="#10b981"><PictureFilled /></el-icon>
            <span>图片增强配置</span>
          </div>
        </template>
        <el-form-item label="启用增强服务">
          <el-switch v-model="form['ai.enhance.enabled']" active-value="true" inactive-value="false" />
        </el-form-item>
        <el-form-item label="服务地址">
          <el-input v-model="form['ai.enhance.url']" placeholder="http://localhost:8090" />
        </el-form-item>
        <el-form-item label="超时时间(ms)">
          <el-input v-model="form['ai.enhance.timeout']" placeholder="30000" />
        </el-form-item>
      </el-card>

      <!-- LLM 大模型配置 -->
      <el-card class="config-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon :size="18" color="#6366f1"><MagicStick /></el-icon>
            <span>LLM大模型配置</span>
          </div>
        </template>
        <el-form-item label="启用LLM服务">
          <el-switch v-model="form['ai.llm.enabled']" active-value="true" inactive-value="false" />
        </el-form-item>
        <el-form-item label="API地址">
          <el-input v-model="form['ai.llm.baseUrl']" placeholder="https://api.openai.com" />
          <div class="form-tip">兼容OpenAI协议的API地址</div>
        </el-form-item>
        <el-form-item label="API密钥">
          <el-input v-model="form['ai.llm.apiKey']" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input v-model="form['ai.llm.model']" placeholder="gpt-4o-mini" />
        </el-form-item>
        <el-form-item label="启用结构化提取">
          <el-switch v-model="form['ai.structuring.enabled']" active-value="true" inactive-value="false" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" :loading="testing" @click="handleTestConnection">
            <el-icon><Connection /></el-icon>
            <span>测试连接</span>
          </el-button>
          <span v-if="testResult" class="test-result" :class="testResult.ok ? 'success' : 'error'">
            {{ testResult.msg }}
          </span>
        </el-form-item>
      </el-card>

      <!-- 保存按钮 -->
      <div class="save-bar">
        <el-button type="primary" size="large" :loading="saving" @click="handleSave">
          <el-icon><Check /></el-icon>
          <span>保存全部配置</span>
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { Document, PictureFilled, MagicStick, Connection, Check } from '@element-plus/icons-vue'
import { listAiConfig, batchUpdateAiConfig, testLlmConnection } from '@/api/intellect/aiconfig'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const testResult = ref(null)

// form: { 'ai.ocr.language': 'chi_sim+eng', ... }
const form = ref({})
// configMap: { 'ai.ocr.language': { configId, configKey, configValue, ... } }
const configMap = ref({})

onMounted(() => loadConfig())

async function loadConfig() {
  loading.value = true
  try {
    const res = await listAiConfig()
    const list = res.data || []
    const f = {}
    const m = {}
    list.forEach(c => {
      f[c.configKey] = c.configValue
      m[c.configKey] = c
    })
    form.value = f
    configMap.value = m
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const configs = Object.entries(form.value).map(([key, value]) => {
      const existing = configMap.value[key]
      return {
        configId: existing?.configId,
        configKey: key,
        configValue: String(value),
      }
    })
    await batchUpdateAiConfig(configs)
    proxy.$modal.msgSuccess('配置保存成功')
    await loadConfig()
  } catch (e) {
    proxy.$modal.msgError('保存失败: ' + (e.message || e))
  } finally {
    saving.value = false
  }
}

async function handleTestConnection() {
  testing.value = true
  testResult.value = null
  try {
    // 先保存当前LLM配置
    await handleSave()
    const res = await testLlmConnection()
    testResult.value = { ok: res.code === 200, msg: res.msg || res.data }
  } catch (e) {
    testResult.value = { ok: false, msg: '连接失败: ' + (e.message || e) }
  } finally {
    testing.value = false
  }
}
</script>

<style lang="scss" scoped>
.ai-config-page {
  max-width: 800px;
}

.config-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.save-bar {
  text-align: center;
  padding: 20px 0;
}

.test-result {
  margin-left: 12px;
  font-size: 13px;
  &.success { color: #67c23a; }
  &.error { color: #f56c6c; }
}
</style>
