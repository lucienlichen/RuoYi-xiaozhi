<template>
  <div class="regulations-view">
    <!-- PC三列布局 -->
    <div class="reg-body" v-loading="loading">
      <!-- 第一列：法律法规 + 市场监管规章 -->
      <div class="reg-col">
        <div v-for="cat in leftCats" :key="cat.id" class="cat-card">
          <div class="cat-card-header">
            <span class="cat-dot" :style="{ background: cat.color }" />
            <span class="cat-label">{{ cat.label }}</span>
            <span class="cat-badge" :style="{ background: cat.colorLight, color: cat.color }">{{ cat.docs.length }}</span>
          </div>
          <div class="cat-card-body">
            <div
              v-for="doc in cat.docs"
              :key="doc.id"
              class="doc-row"
              :class="{ active: selectedDocId === doc.id }"
              @click="openDoc(doc)"
            >
              <el-icon class="doc-row-icon" :size="15" :style="{ color: cat.color }"><Document /></el-icon>
              <span class="doc-row-title">{{ doc.title }}</span>
            </div>
            <div v-if="!loading && cat.docs.length === 0" class="cat-empty">暂无文档</div>
          </div>
        </div>
      </div>

      <!-- 第二列：TSG技术规范 -->
      <div class="reg-col">
        <div v-for="cat in midCats" :key="cat.id" class="cat-card cat-card--full">
          <div class="cat-card-header">
            <span class="cat-dot" :style="{ background: cat.color }" />
            <span class="cat-label">{{ cat.label }}</span>
            <span class="cat-badge" :style="{ background: cat.colorLight, color: cat.color }">{{ cat.docs.length }}</span>
          </div>
          <div class="cat-card-body">
            <div
              v-for="doc in cat.docs"
              :key="doc.id"
              class="doc-row"
              :class="{ active: selectedDocId === doc.id }"
              @click="openDoc(doc)"
            >
              <el-icon class="doc-row-icon" :size="15" :style="{ color: cat.color }"><Document /></el-icon>
              <span class="doc-row-title">{{ doc.title }}</span>
            </div>
            <div v-if="!loading && cat.docs.length === 0" class="cat-empty">暂无文档</div>
          </div>
        </div>
      </div>

      <!-- 第三列：标准 -->
      <div class="reg-col">
        <div v-for="cat in rightCats" :key="cat.id" class="cat-card cat-card--full">
          <div class="cat-card-header">
            <span class="cat-dot" :style="{ background: cat.color }" />
            <span class="cat-label">{{ cat.label }}</span>
            <span class="cat-badge" :style="{ background: cat.colorLight, color: cat.color }">{{ cat.docs.length }}</span>
          </div>
          <div class="cat-card-body">
            <div
              v-for="doc in cat.docs"
              :key="doc.id"
              class="doc-row"
              :class="{ active: selectedDocId === doc.id }"
              @click="openDoc(doc)"
            >
              <el-icon class="doc-row-icon" :size="15" :style="{ color: cat.color }"><Document /></el-icon>
              <span class="doc-row-title">{{ doc.title }}</span>
            </div>
            <div v-if="!loading && cat.docs.length === 0" class="cat-empty">暂无文档</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧抽屉：原始文件预览 -->
    <el-drawer
      v-model="drawerVisible"
      direction="rtl"
      :size="drawerSize"
      :close-on-click-modal="true"
      destroy-on-close
      @close="onDrawerClose"
    >
      <template #header>
        <div class="drawer-header">
          <h3 class="drawer-title">{{ selectedDoc?.title }}</h3>
          <div class="drawer-meta">
            <el-tag v-if="selectedDoc?.docNo" size="small" type="info">{{ selectedDoc.docNo }}</el-tag>
            <span v-if="selectedDoc?.fileName" class="drawer-file">{{ selectedDoc.fileName }}</span>
            <a class="drawer-download" :href="fileUrl" :download="selectedDoc?.fileName" target="_blank">
              <el-icon :size="14"><Download /></el-icon> 下载原文件
            </a>
          </div>
        </div>
      </template>
      <div class="drawer-content" v-loading="docLoading">
        <!-- PDF 预览 -->
        <VueOfficePdf v-if="fileType === 'pdf'" :src="fileUrl" class="file-viewer" />
        <!-- DOCX 预览 -->
        <VueOfficeDocx v-else-if="fileType === 'docx'" :src="fileUrl" class="file-viewer" />
        <!-- 其他格式：下载提示 -->
        <div v-else-if="selectedDoc && !docLoading" class="file-fallback">
          <el-icon :size="48" color="#94a3b8"><Document /></el-icon>
          <p>该文件格式（{{ fileExt }}）暂不支持在线预览</p>
          <a :href="fileUrl" :download="selectedDoc?.fileName" target="_blank">
            <el-button type="primary" icon="Download">下载文件查看</el-button>
          </a>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Document, Download } from '@element-plus/icons-vue'
import VueOfficePdf from '@vue-office/pdf'
import VueOfficeDocx from '@vue-office/docx'
import '@vue-office/docx/lib/index.css'
import { listRegulations, getRegulation } from '@/api/intellect/regulation'

const categories = [
  { id: 'laws', label: '法律法规', color: '#3b82f6', colorLight: '#dbeafe' },
  { id: 'market_rules', label: '市场监管总局规章', color: '#8b5cf6', colorLight: '#ede9fe' },
  { id: 'tsg', label: '特种设备安全技术规范TSG', color: '#f59e0b', colorLight: '#fef3c7' },
  { id: 'standards', label: '标准', color: '#10b981', colorLight: '#d1fae5' }
]

const allDocs = ref({})
const loading = ref(false)
const selectedDocId = ref(null)
const selectedDoc = ref(null)
const docLoading = ref(false)
const drawerVisible = ref(false)


const drawerSize = computed(() => window.innerWidth <= 820 ? '92%' : '70%')

const totalCount = computed(() =>
  Object.values(allDocs.value).reduce((sum, docs) => sum + docs.length, 0)
)

function withDocs(id) {
  const cat = categories.find(c => c.id === id)
  return { ...cat, docs: allDocs.value[id] || [] }
}

const leftCats = computed(() => [withDocs('laws'), withDocs('market_rules')])
const midCats = computed(() => [withDocs('tsg')])
const rightCats = computed(() => [withDocs('standards')])

/** 文件URL：filePath 以 /profile/ 开头，直接拼后端地址 */
const fileUrl = computed(() => {
  if (!selectedDoc.value?.filePath) return ''
  return import.meta.env.VITE_APP_BASE_API + selectedDoc.value.filePath
})

const fileExt = computed(() => {
  const name = selectedDoc.value?.fileName || ''
  return name.split('.').pop().toLowerCase()
})

const fileType = computed(() => {
  const ext = fileExt.value
  if (ext === 'pdf') return 'pdf'
  if (ext === 'docx') return 'docx'
  return 'other'
})

async function loadAllDocs() {
  loading.value = true
  const results = await Promise.all(
    categories.map(cat =>
      listRegulations({ category: cat.id, pageNum: 1, pageSize: 200 })
        .then(res => ({ id: cat.id, docs: res.rows || [] }))
        .catch(() => ({ id: cat.id, docs: [] }))
    )
  )
  const map = {}
  results.forEach(r => { map[r.id] = r.docs })
  allDocs.value = map
  loading.value = false
}

async function openDoc(doc) {
  selectedDocId.value = doc.id
  drawerVisible.value = true
  docLoading.value = true
  try {
    const res = await getRegulation(doc.id)
    selectedDoc.value = res.data
  } catch {
    selectedDoc.value = doc
  }
  docLoading.value = false
}

function onDrawerClose() {
  selectedDocId.value = null
  selectedDoc.value = null
}

onMounted(() => {
  loadAllDocs()
})
</script>

<style lang="scss" scoped>
.regulations-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--ds-surface);
}



/* ===== 三列内容区 ===== */
.reg-body {
  flex: 1;
  display: flex;
  gap: var(--ds-space-3);
  padding: var(--ds-space-4);
  overflow: hidden;
  min-height: 0;
}

.reg-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

/* ===== 分类卡片 ===== */
.cat-card {
  background: var(--ds-surface-container-lowest);
  border-radius: var(--ds-radius-lg);
  box-shadow: var(--ds-shadow-sm);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex: 1;
  min-height: 0;
}

.cat-card--full {
  flex: 1;
}

.cat-card-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--ds-surface-container-low);
}

.cat-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}

.cat-label {
  font-size: 14px;
  font-weight: 700;
  color: var(--ds-on-surface);
  flex: 1;
}

.cat-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: var(--ds-radius-full);
  flex-shrink: 0;
}

.cat-card-body {
  flex: 1;
  overflow-y: auto;
  padding: 4px 6px;
}

.cat-empty {
  text-align: center;
  padding: 24px 12px;
  color: var(--ds-outline);
  font-size: 13px;
}

/* ===== 文档行 ===== */
.doc-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 10px;
  border-radius: var(--ds-radius-sm);
  cursor: pointer;
  transition: background 0.12s;

  &:hover { background: var(--ds-surface-container-low); }

  &.active {
    background: var(--ds-slate-surface);
    .doc-row-title { color: var(--ds-slate); font-weight: 600; }
  }
}

.doc-row-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.doc-row-title {
  font-size: 13px;
  color: var(--ds-on-surface-variant);
  line-height: 1.45;
  word-break: break-all;
}

/* ===== 抽屉 ===== */
.drawer-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.drawer-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--ds-on-surface);
  margin: 0;
  line-height: 1.4;
}

.drawer-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.drawer-file {
  font-size: 12px;
  color: var(--ds-outline);
}

.drawer-download {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--ds-primary);
  text-decoration: none;
  cursor: pointer;

  &:hover { opacity: 0.8; }
}

.drawer-content {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.file-viewer {
  flex: 1;
  width: 100%;
  min-height: 0;
}

.file-fallback {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--ds-on-surface-variant);
  font-size: 14px;

  a { text-decoration: none; }
}

/* ===== Robot / 小屏适配 ===== */
@media (max-width: 820px) {
  .reg-header {
    padding: 12px 14px;
  }

  .reg-title { font-size: 15px; }

  .reg-body {
    flex-direction: column;
    overflow-y: auto;
    padding: 10px;
  }

  .reg-col {
    flex: none;
    overflow: visible;
  }

  .cat-card {
    flex: none;
    min-height: auto;
  }

  .doc-row { padding: 10px; }
  .doc-row-title { font-size: 15px; }
}
</style>
