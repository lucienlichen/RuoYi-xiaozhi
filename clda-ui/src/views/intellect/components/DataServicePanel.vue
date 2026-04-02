<template>
  <div class="data-service-panel">
    <!-- Category tabs -->
    <div class="panel-tabs">
      <a
        v-for="cat in enabledCategories"
        :key="cat.id"
        class="panel-tab"
        :class="{ active: activeTab === String(cat.id) }"
        @click="activeTab = String(cat.id)"
      >
        {{ cat.categoryName }}
      </a>
    </div>

    <!-- File table with dynamic template columns -->
    <div class="panel-content" v-loading="loading">
      <el-table :data="tableData" stripe size="small" :empty-text="'当前分类暂无数据文件'">
        <el-table-column label="文件名" prop="fileName" min-width="140" show-overflow-tooltip fixed />
        <!-- Dynamic template field columns -->
        <el-table-column
          v-for="field in templateFields"
          :key="field.key"
          :label="field.label"
          min-width="100"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span v-if="row._structured && row._structured[field.key] != null">{{ row._structured[field.key] }}</span>
            <span v-else class="cell-empty">—</span>
          </template>
        </el-table-column>
        <!-- Process artifact columns -->
        <el-table-column label="原始数据" width="72" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openArtifact(row, 'original')">查看</el-button>
          </template>
        </el-table-column>
        <el-table-column label="预处理" width="72" align="center">
          <template #default="{ row }">
            <el-button v-if="row.preprocessedPath" link type="success" size="small" @click.stop="openArtifact(row, 'preprocessed')">查看</el-button>
            <el-tag v-else size="small" :type="stageTagType(row.preprocessStatus)">{{ stageLabel(row.preprocessStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="OCR" width="72" align="center">
          <template #default="{ row }">
            <el-button v-if="row.ocrStatus === 'DONE'" link type="success" size="small" @click.stop="openArtifact(row, 'ocr')">查看</el-button>
            <el-tag v-else size="small" :type="stageTagType(row.ocrStatus)">{{ stageLabel(row.ocrStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结构化" width="72" align="center">
          <template #default="{ row }">
            <el-button v-if="row.structuredData" link type="success" size="small" @click.stop="openArtifact(row, 'structured')">查看</el-button>
            <el-tag v-else-if="row.ocrStatus === 'DONE'" size="small" type="warning">待处理</el-tag>
            <span v-else class="cell-empty">-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Pagination -->
    <div v-if="totalRecords > pageSize" class="panel-pagination">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="totalRecords"
        layout="prev, pager, next"
        small
      />
    </div>

    <!-- Detail Drawer -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="560px" direction="rtl" destroy-on-close>
      <div v-if="drawerFile" class="drawer-body">
        <template v-if="drawerMode === 'original'">
          <div class="drawer-section">
            <h4>原始数据</h4>
            <img v-if="drawerFile.fileType === 'image'" :src="fileUrl(drawerFile.filePath)" class="drawer-img" />
            <iframe v-else-if="drawerFile.fileType === 'pdf'" :src="fileUrl(drawerFile.filePath)" class="drawer-iframe"></iframe>
            <div v-else class="drawer-nopreview">该类型暂不支持预览</div>
          </div>
        </template>

        <template v-if="drawerMode === 'preprocessed'">
          <div v-if="drawerFile.preprocessedPath" class="drawer-section">
            <h4>预处理结果（二值化+纠偏）</h4>
            <img :src="fileUrl(drawerFile.preprocessedPath)" class="drawer-img" />
          </div>
        </template>

        <template v-if="drawerMode === 'ocr'">
          <div v-if="drawerFile.ocrText" class="drawer-section">
            <h4>OCR识别文本</h4>
            <pre class="drawer-text">{{ drawerFile.ocrText }}</pre>
          </div>
          <div v-else class="drawer-nopreview">暂无OCR结果</div>
        </template>

        <template v-if="drawerMode === 'structured'">
          <div v-if="parsedDrawerStructured" class="drawer-section">
            <h4>结构化数据</h4>
            <table class="structured-table">
              <thead><tr><th>字段</th><th>值</th></tr></thead>
              <tbody>
                <tr v-for="field in templateFields" :key="field.key">
                  <td class="field-key">{{ field.label }}</td>
                  <td>{{ parsedDrawerStructured[field.key] ?? '—' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="drawer-nopreview">暂无结构化数据</div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { listCategories, listEquipData, getDataFiles, getStructuredData } from '@/api/intellect/equipdata'
import { listTemplates } from '@/api/intellect/structuring'

const props = defineProps({
  equipmentId: { type: [Number, String], default: null },
  initialFile: { type: Object, default: null }
})

defineEmits(['close'])

const allCategories = ref([])
const allTemplates = ref([]) // all structuring templates
const activeTab = ref('')
const files = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 10
const totalRecords = ref(0)

// Drawer
const drawerVisible = ref(false)
const drawerFile = ref(null)
const drawerMode = ref('original')
const drawerTitle = ref('')

const enabledCategories = computed(() =>
  allCategories.value.filter(c => String(c.parentId) === '0' && c.enabled === '1')
)

// Active category's code (e.g. 'inspection', 'fault')
const activeCategoryCode = computed(() => {
  const cat = allCategories.value.find(c => String(c.id) === activeTab.value)
  return cat?.categoryCode || ''
})

// Template fields for current category
const templateFields = computed(() => {
  const tpl = allTemplates.value.find(t => t.categoryCode === activeCategoryCode.value)
  if (!tpl || !tpl.fieldSchema) return []
  try {
    return typeof tpl.fieldSchema === 'string' ? JSON.parse(tpl.fieldSchema) : tpl.fieldSchema
  } catch { return [] }
})

// Table data: files with parsed structured data attached
const tableData = computed(() => {
  return files.value.map(f => {
    let parsed = null
    if (f.structuredData) {
      try {
        parsed = typeof f.structuredData === 'string' ? JSON.parse(f.structuredData) : f.structuredData
      } catch { /* ignore */ }
    }
    return { ...f, _structured: parsed }
  })
})

// Drawer structured data parsed
const parsedDrawerStructured = computed(() => {
  if (!drawerFile.value?.structuredData) return null
  try {
    return typeof drawerFile.value.structuredData === 'string'
      ? JSON.parse(drawerFile.value.structuredData)
      : drawerFile.value.structuredData
  } catch { return null }
})

onMounted(async () => {
  await loadCategoriesData()
  await loadAllTemplates()
})

async function loadCategoriesData() {
  try {
    const res = await listCategories()
    allCategories.value = res.data || []
    if (enabledCategories.value.length > 0) {
      activeTab.value = String(enabledCategories.value[0].id)
    }
  } catch (e) { console.warn(e) }
}

async function loadAllTemplates() {
  try {
    const res = await listTemplates({ pageNum: 1, pageSize: 100 })
    allTemplates.value = res.rows || []
  } catch (e) { console.warn(e) }
}

watch(activeTab, () => { pageNum.value = 1; loadFiles() })
watch(pageNum, () => loadFiles())
watch(() => props.equipmentId, () => { if (props.equipmentId) loadFiles() })

async function loadFiles() {
  if (!props.equipmentId || !activeTab.value) return
  loading.value = true
  try {
    const res = await listEquipData({
      equipmentId: props.equipmentId,
      categoryId: activeTab.value,
      pageNum: pageNum.value,
      pageSize
    })
    const records = res.rows || []
    totalRecords.value = res.total || 0
    const allFiles = []
    for (const r of records) {
      const filesRes = await getDataFiles(r.id)
      allFiles.push(...(filesRes.data || []))
    }
    files.value = allFiles
  } finally { loading.value = false }
}

function openArtifact(row, mode) {
  drawerFile.value = row
  drawerMode.value = mode
  const labels = { original: '原始数据', preprocessed: '预处理结果', ocr: 'OCR结果', structured: '结构化数据' }
  drawerTitle.value = (labels[mode] || '详情') + ' - ' + row.fileName
  drawerVisible.value = true
  if (mode === 'structured' || mode === 'ocr') loadStructuredDetail(row.id)
}

async function loadStructuredDetail(fileId) {
  try {
    const res = await getStructuredData(fileId)
    if (res.data) drawerFile.value = { ...drawerFile.value, ...res.data }
  } catch (e) { console.warn(e) }
}

function stageTagType(s) {
  return { NONE: 'info', PROCESSING: '', DONE: 'success', FAILED: 'danger' }[s] || 'info'
}
function stageLabel(s) {
  return { NONE: '待处理', PROCESSING: '处理中', DONE: '已完成', FAILED: '出错' }[s] || s
}
function fileUrl(path) {
  return path ? import.meta.env.VITE_APP_BASE_API + path : ''
}
</script>

<style lang="scss" scoped>
.data-service-panel { display: flex; flex-direction: column; height: 100%; background: var(--ds-surface-container-lowest); }

.panel-tabs {
  display: flex; padding: 0 20px; overflow-x: auto; border-bottom: 1px solid #e5e7eb; flex-shrink: 0;
  &::-webkit-scrollbar { display: none; }
}
.panel-tab {
  padding: 10px 16px; font-size: 12px; font-weight: 500; color: #6b7280; cursor: pointer;
  white-space: nowrap; border-bottom: 2px solid transparent; transition: all 0.2s;
  &:hover { color: #006c49; }
  &.active { color: #006c49; font-weight: 700; border-bottom-color: #006c49; }
}

.panel-content { flex: 1; overflow: auto; padding: 12px 16px; }
.cell-empty { color: #d1d5db; }
.panel-pagination { padding: 12px 20px; display: flex; justify-content: center; border-top: 1px solid #e5e7eb; }

/* Drawer */
.drawer-body { padding: 0 4px; }
.drawer-section { margin-bottom: 20px; h4 { font-size: 13px; font-weight: 600; margin: 0 0 8px; } }
.drawer-img { width: 100%; border-radius: 8px; border: 1px solid #e5e7eb; }
.drawer-iframe { width: 100%; height: 400px; border: none; border-radius: 8px; }
.drawer-nopreview { color: #9ca3af; text-align: center; padding: 20px; }
.drawer-text {
  white-space: pre-wrap; word-break: break-all; font-size: 12px; line-height: 1.6;
  color: #374151; background: #f9fafb; padding: 12px; border-radius: 8px;
  max-height: 300px; overflow-y: auto; margin: 0;
}
.structured-table {
  width: 100%; border-collapse: collapse; font-size: 13px;
  th { text-align: left; padding: 8px 12px; background: #f3f4f6; font-weight: 600; font-size: 12px; }
  td { padding: 8px 12px; border-bottom: 1px solid #e5e7eb; }
  .field-key { font-weight: 500; width: 35%; white-space: nowrap; }
}
</style>
