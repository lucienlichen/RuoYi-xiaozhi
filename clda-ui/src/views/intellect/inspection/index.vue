<template>
  <div class="inspection-view">
    <!-- 未选设备提示 -->
    <div v-if="!selectedEquipment" class="empty-state">
      <el-icon :size="48" class="icon-muted"><Warning /></el-icon>
      <p>请先在左侧选择设备</p>
    </div>

    <!-- 主布局：左操作+历史 / 右预览 -->
    <div v-else class="inspection-body">
      <!-- 左侧 -->
      <aside class="inspection-left">
        <!-- 操作区 -->
        <div class="action-section">
          <div class="current-equipment">
            <span class="eq-label">当前设备</span>
            <span class="eq-name">{{ selectedEquipment.equipmentName }}</span>
          </div>
          <div class="action-buttons">
            <el-button type="primary" :loading="generating" @click="handleGenerate">
              <el-icon><Document /></el-icon> 生成排查表
            </el-button>
            <el-upload
              ref="uploadRef"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :data="{ equipmentId: selectedEquipment.id, equipmentName: selectedEquipment.equipmentName }"
              accept=".xlsx,.xls"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
            >
              <el-button type="warning" :loading="uploading">
                <el-icon><Upload /></el-icon> 上传排查表
              </el-button>
            </el-upload>
          </div>
        </div>

        <!-- 历史记录 -->
        <div class="history-section">
          <div class="history-title">历史排查记录</div>
          <div v-if="records.length === 0" class="history-empty">暂无排查记录</div>
          <div v-for="record in records" :key="record.id" class="record-card">
            <div class="record-header" @click="toggleRecord(record.id)">
              <div class="record-info">
                <span class="record-date">{{ record.inspectDate }}</span>
                <span class="record-inspector">{{ record.inspector }}</span>
              </div>
              <div class="record-summary">
                <span v-if="record.majorCount > 0" class="tag tag-major">重大{{ record.majorCount }}</span>
                <span v-if="record.otherCount > 0" class="tag tag-other">其他{{ record.otherCount }}</span>
                <span v-if="record.majorCount === 0 && record.otherCount === 0" class="tag tag-ok">无隐患</span>
              </div>
              <el-icon class="record-chevron" :class="{ expanded: expandedRecordId === record.id }" :size="14"><ArrowDown /></el-icon>
            </div>
            <!-- 展开详情 -->
            <div v-if="expandedRecordId === record.id" class="record-detail">
              <div v-if="detailLoading" class="detail-loading">
                <el-icon class="is-loading" :size="16"><Loading /></el-icon> 加载中...
              </div>
              <template v-else>
                <div v-for="r in expandedResults" :key="r.id" class="result-row">
                  <span class="result-no">{{ r.itemNo }}.</span>
                  <span class="result-content">{{ r.content || r.subCategory }}</span>
                  <span class="result-val" :class="r.result === '有' ? 'has' : 'none'">{{ r.result }}</span>
                </div>
              </template>
            </div>
          </div>
        </div>
      </aside>

      <!-- 右侧：排查表预览 -->
      <main class="inspection-right">
        <div v-if="!previewItems.length" class="preview-empty">
          <el-icon :size="48" class="icon-muted"><DocumentChecked /></el-icon>
          <p>点击"生成排查表"预览排查内容</p>
        </div>
        <div v-else class="preview-area">
          <div class="preview-header">
            <h3>起重装备隐患排查表</h3>
            <div class="preview-meta">
              <span>设备：{{ selectedEquipment.equipmentName }}</span>
              <span>排查人：{{ username }}</span>
              <span>日期：{{ today }}</span>
            </div>
          </div>
          <!-- 重大隐患 -->
          <div class="preview-category">
            <div class="category-title major">重大隐患</div>
            <table class="preview-table">
              <thead>
                <tr><th width="50">序号</th><th width="100">排查项目</th><th>排查内容</th><th width="70">结果</th></tr>
              </thead>
              <tbody>
                <tr v-for="item in majorItems" :key="item.id">
                  <td>{{ item.itemNo }}</td>
                  <td>{{ item.subCategory }}</td>
                  <td>{{ item.content }}</td>
                  <td class="result-cell">有 / 无</td>
                </tr>
              </tbody>
            </table>
          </div>
          <!-- 其他隐患 -->
          <div class="preview-category">
            <div class="category-title other">其他隐患</div>
            <table class="preview-table">
              <thead>
                <tr><th width="50">序号</th><th width="100">排查项目</th><th>排查内容</th><th width="70">结果</th></tr>
              </thead>
              <tbody>
                <tr v-for="item in otherItems" :key="item.id">
                  <td>{{ item.itemNo }}</td>
                  <td>{{ item.subCategory }}</td>
                  <td>{{ item.content }}</td>
                  <td class="result-cell">有 / 无</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="preview-actions">
            <el-button type="primary" @click="handleDownload">
              <el-icon><Download /></el-icon> 下载 Excel
            </el-button>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Warning, Document, Upload, Download, ArrowDown, Loading, DocumentChecked } from '@element-plus/icons-vue'
import { getInspectionItems, getInspectionRecords, getInspectionRecordDetail } from '@/api/intellect/inspection'
import useEquipmentStore from '@/store/modules/equipment'
import useUserStore from '@/store/modules/user'
import { getToken } from '@/utils/auth'

const equipmentStore = useEquipmentStore()
const userStore = useUserStore()

const selectedEquipment = computed(() => equipmentStore.selectedEquipment)
const username = computed(() => userStore.name)
const today = new Date().toISOString().slice(0, 10)

const generating = ref(false)
const uploading = ref(false)
const previewItems = ref([])
const records = ref([])
const expandedRecordId = ref(null)
const expandedResults = ref([])
const detailLoading = ref(false)

const majorItems = computed(() => previewItems.value.filter(i => i.category === '重大隐患'))
const otherItems = computed(() => previewItems.value.filter(i => i.category !== '重大隐患'))

const uploadUrl = computed(() => import.meta.env.VITE_APP_BASE_API + '/intellect/inspection/upload')
const uploadHeaders = computed(() => ({ Authorization: 'Bearer ' + getToken() }))

async function handleGenerate() {
  generating.value = true
  try {
    const res = await getInspectionItems()
    previewItems.value = res.data || []
  } catch {
    ElMessage.error('获取检查项失败')
  }
  generating.value = false
}

function handleDownload() {
  const equipId = selectedEquipment.value?.id
  if (!equipId) return
  const url = import.meta.env.VITE_APP_BASE_API + `/intellect/inspection/generate?equipmentId=${equipId}`
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', '')
  // Use fetch to handle auth header
  fetch(url, { headers: { Authorization: 'Bearer ' + getToken() } })
    .then(res => res.blob())
    .then(blob => {
      const blobUrl = URL.createObjectURL(blob)
      link.href = blobUrl
      link.download = `隐患排查表_${selectedEquipment.value.equipmentName}_${today}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(blobUrl)
    })
    .catch(() => ElMessage.error('下载失败'))
}

function beforeUpload(file) {
  const isExcel = /\.(xlsx|xls)$/i.test(file.name)
  if (!isExcel) {
    ElMessage.error('仅支持上传 Excel 文件')
    return false
  }
  uploading.value = true
  return true
}

function handleUploadSuccess(res) {
  uploading.value = false
  if (res.code === 200) {
    ElMessage.success('上传解析成功')
    loadRecords()
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

function handleUploadError() {
  uploading.value = false
  ElMessage.error('上传失败')
}

async function loadRecords() {
  const equipId = selectedEquipment.value?.id
  if (!equipId) return
  try {
    const res = await getInspectionRecords(equipId)
    records.value = res.data || []
  } catch {
    records.value = []
  }
}

async function toggleRecord(id) {
  if (expandedRecordId.value === id) {
    expandedRecordId.value = null
    return
  }
  expandedRecordId.value = id
  detailLoading.value = true
  try {
    const res = await getInspectionRecordDetail(id)
    expandedResults.value = res.data?.results || []
  } catch {
    expandedResults.value = []
  }
  detailLoading.value = false
}

watch(selectedEquipment, () => {
  previewItems.value = []
  records.value = []
  expandedRecordId.value = null
  if (selectedEquipment.value) {
    loadRecords()
  }
}, { immediate: true })
</script>

<style lang="scss" scoped>
.inspection-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--ds-surface);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  color: var(--ds-outline);
  font-size: 15px;
}

/* ===== 主布局 ===== */
.inspection-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

/* ===== 左侧 ===== */
.inspection-left {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--ds-surface-container-lowest);
  box-shadow: 1px 0 0 var(--ds-surface-container);
}

.action-section {
  padding: 16px;
  background: var(--ds-surface-container-low);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-buttons {
  display: flex;
  gap: 8px;

  .el-upload { display: inline-block; }
}

.current-equipment {
  display: flex;
  align-items: center;
  gap: 8px;
}

.eq-label {
  font-size: 12px;
  color: var(--ds-outline);
}

.eq-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ds-on-surface);
}

/* ===== 历史记录 ===== */
.history-section {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}

.history-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--ds-on-surface-variant);
  margin-bottom: 10px;
}

.history-empty {
  text-align: center;
  color: var(--ds-outline);
  font-size: 13px;
  padding: 24px 0;
}

.record-card {
  background: var(--ds-surface);
  border-radius: var(--ds-radius);
  margin-bottom: 8px;
  overflow: hidden;
}

.record-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.15s;
  &:hover { background: var(--ds-surface-container-low); }
}

.record-info {
  flex: 1;
  min-width: 0;
  display: flex;
  gap: 8px;
}

.record-date {
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-on-surface);
}

.record-inspector {
  font-size: 12px;
  color: var(--ds-outline);
}

.record-summary {
  display: flex;
  gap: 4px;
}

.tag {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
}

.tag-major { background: var(--ds-orange-surface); color: var(--ds-error); }
.tag-other { background: var(--ds-amber-surface); color: var(--ds-amber); }
.tag-ok { background: var(--ds-emerald-surface); color: var(--ds-success); }

.record-chevron {
  color: var(--ds-outline);
  transition: transform 0.2s;
  &.expanded { transform: rotate(180deg); }
}

.record-detail {
  background: var(--ds-surface-container-low);
  padding: 8px 12px;
  max-height: 240px;
  overflow-y: auto;
}

.detail-loading {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 0;
  color: var(--ds-outline);
  font-size: 13px;
}

.result-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 0;
  font-size: 12px;
}

.result-no {
  flex-shrink: 0;
  width: 24px;
  color: var(--ds-outline);
  font-weight: 600;
}

.result-content {
  flex: 1;
  color: var(--ds-on-surface-variant);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-val {
  flex-shrink: 0;
  font-weight: 700;
  &.has { color: var(--ds-error); }
  &.none { color: var(--ds-success); }
}

/* ===== 右侧预览 ===== */
.inspection-right {
  flex: 1;
  overflow-y: auto;
  min-width: 0;
}

.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  color: var(--ds-outline);
  font-size: 14px;
}

.preview-area {
  padding: 20px 24px;
}

.preview-header {
  text-align: center;
  margin-bottom: 20px;

  h3 {
    font-size: 18px;
    font-weight: 700;
    color: var(--ds-on-surface);
    margin: 0 0 8px;
  }
}

.preview-meta {
  display: flex;
  justify-content: center;
  gap: 24px;
  font-size: 13px;
  color: var(--ds-on-surface-variant);
}

.preview-category {
  margin-bottom: 20px;
}

.category-title {
  font-size: 15px;
  font-weight: 700;
  padding: 8px 12px;
  border-radius: 6px;
  margin-bottom: 8px;

  &.major { background: var(--ds-orange-surface); color: var(--ds-error); }
  &.other { background: var(--ds-amber-surface); color: var(--ds-amber); }
}

.preview-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;

  th, td {
    border: 1px solid var(--ds-surface-container);
    padding: 6px 10px;
    text-align: left;
  }

  th {
    background: var(--ds-surface);
    font-weight: 600;
    color: var(--ds-on-surface-variant);
  }

  td {
    color: var(--ds-on-surface);
  }

  .result-cell {
    text-align: center;
    color: var(--ds-outline);
    font-style: italic;
  }
}

.preview-actions {
  margin-top: 16px;
  text-align: center;
}

/* ===== 小屏 ===== */
@media (max-width: 820px) {
  .inspection-body { flex-direction: column; }
  .inspection-left { width: 100%; max-height: 50vh; box-shadow: none; border-bottom: 1px solid var(--ds-surface-container); }
}

.icon-muted { color: var(--ds-outline-variant); }
</style>
