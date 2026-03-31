<template>
  <div class="regulations-view" :class="{ 'is-tablet': isTablet }">

    <!-- PC 左右分栏 / 平板列表页 -->
    <div v-show="!isTablet || !isDetailMode" class="regulations-layout">

      <!-- 左侧文档库面板 -->
      <aside class="doc-panel">
        <!-- 分类 Tabs -->
        <div class="category-tabs">
          <button
            v-for="cat in categories"
            :key="cat.id"
            class="cat-tab"
            :class="{ active: activeCategory === cat.id }"
            @click="switchCategory(cat.id)"
          >
            {{ cat.label }}
          </button>
        </div>

        <!-- 文档列表 -->
        <div class="doc-list" v-loading="listLoading">
          <div
            v-for="doc in filteredDocs"
            :key="doc.id"
            class="doc-item"
            :class="{ active: selectedDocId === doc.id }"
            @click="selectDoc(doc.id)"
          >
            <el-icon class="doc-icon" :size="16"><Document /></el-icon>
            <div class="doc-info">
              <span class="doc-name">{{ doc.title }}</span>
              <span v-if="doc.docNo" class="doc-no">{{ doc.docNo }}</span>
            </div>
            <el-icon class="doc-arrow" :size="14"><ArrowRight /></el-icon>
          </div>
          <div v-if="!listLoading && filteredDocs.length === 0" class="doc-empty">
            <p>暂无{{ currentCategoryLabel }}文档</p>
          </div>
        </div>
      </aside>

      <!-- 右侧内容区（PC 显示） -->
      <main v-if="!isTablet" class="content-panel">
        <div v-if="!selectedDoc" class="content-empty">
          <el-icon :size="48" color="#e2e8f0"><Document /></el-icon>
          <p>请从左侧列表选择法规查阅</p>
        </div>
        <template v-else>
          <div class="content-header">
            <div class="content-title-row">
              <h1 class="content-title">{{ selectedDoc.title }}</h1>
              <span v-if="selectedDoc.docNo" class="content-docno">{{ selectedDoc.docNo }}</span>
            </div>
            <span v-if="selectedDoc.publishDate" class="content-date">发布日期：{{ selectedDoc.publishDate }}</span>
          </div>
          <div class="content-body" v-loading="docLoading">
            <div class="doc-content" v-html="selectedDoc.contentHtml" />
          </div>
        </template>
      </main>
    </div>

    <!-- 平板详情页 -->
    <div v-if="isTablet && isDetailMode" class="detail-page">
      <div class="detail-header">
        <button class="back-btn" @click="isDetailMode = false">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </button>
        <span class="detail-title">{{ selectedDoc?.title }}</span>
      </div>
      <div class="detail-body" v-loading="docLoading">
        <div class="doc-content" v-html="selectedDoc?.contentHtml" />
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { Document, ArrowRight, ArrowLeft } from '@element-plus/icons-vue'
import { listRegulations, getRegulation } from '@/api/intellect/regulation'

const categories = [
  { id: 'laws', label: '法律法规' },
  { id: 'market_rules', label: '市场监管规章' },
  { id: 'tsg', label: 'TSG技术规范' },
  { id: 'standards', label: '标准' }
]

const allDocs = ref([])
const listLoading = ref(false)

function loadDocs() {
  listLoading.value = true
  listRegulations({ category: activeCategory.value, pageNum: 1, pageSize: 100 }).then(res => {
    allDocs.value = res.rows || []
    listLoading.value = false
  })
}

const activeCategory = ref('laws')
const selectedDocId = ref(null)
const selectedDoc = ref(null)
const docLoading = ref(false)
const isDetailMode = ref(false)
const isTablet = ref(false)

function checkScreenSize() {
  isTablet.value = window.innerWidth <= 820
}

onMounted(() => {
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
  loadDocs()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkScreenSize)
})

const filteredDocs = computed(() => allDocs.value)

const currentCategoryLabel = computed(() => categories.find(c => c.id === activeCategory.value)?.label || '')

function switchCategory(catId) {
  activeCategory.value = catId
  selectedDocId.value = null
  selectedDoc.value = null
  loadDocs()
}

async function selectDoc(docId) {
  selectedDocId.value = docId
  if (isTablet.value) isDetailMode.value = true
  docLoading.value = true
  const res = await getRegulation(docId)
  selectedDoc.value = res.data
  docLoading.value = false
}
</script>

<style lang="scss" scoped>
.regulations-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.regulations-layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}

/* ===== 左侧文档库面板 ===== */
.doc-panel {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e2e8f0;
  background: #f8fafc;
  overflow: hidden;
}

/* 分类 Tabs */
.category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 12px 12px 8px;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
  flex-shrink: 0;
}

.cat-tab {
  padding: 5px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;

  &:hover {
    border-color: #64748b;
    color: #334155;
  }

  &.active {
    background: #64748b;
    border-color: #64748b;
    color: #fff;
    font-weight: 600;
  }
}

/* 文档列表 */
.doc-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 8px;
}

.doc-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  margin-bottom: 2px;

  &:hover {
    background: #f1f5f9;
  }

  &.active {
    background: #e2e8f0;
    .doc-name { color: #1e293b; font-weight: 600; }
    .doc-icon { color: #64748b; }
  }
}

.doc-icon {
  flex-shrink: 0;
  color: #94a3b8;
  margin-top: 2px;
}

.doc-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.doc-name {
  font-size: 13px;
  color: #374151;
  line-height: 1.4;
  word-break: break-all;
}

.doc-no {
  font-size: 11px;
  color: #94a3b8;
}

.doc-arrow {
  flex-shrink: 0;
  color: #cbd5e1;
  margin-top: 2px;
}

.doc-empty {
  text-align: center;
  padding: 40px 16px;
  color: #94a3b8;
  font-size: 13px;
}

/* ===== 右侧内容区 ===== */
.content-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #94a3b8;
  font-size: 14px;
}

.content-header {
  flex-shrink: 0;
  padding: 20px 32px 16px;
  border-bottom: 1px solid #f1f5f9;
  background: #fff;
}

.content-title-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}

.content-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.content-docno {
  font-size: 12px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
}

.content-date {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

.content-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 32px 32px;
}

/* ===== 文档内容渲染 ===== */
.doc-content {
  :deep(h2) {
    font-size: 18px;
    font-weight: 700;
    color: #1e293b;
    margin: 0 0 16px;
    padding-bottom: 10px;
    border-bottom: 2px solid #e2e8f0;
  }

  :deep(h3) {
    font-size: 15px;
    font-weight: 600;
    color: #334155;
    margin: 20px 0 10px;
    padding-left: 10px;
    border-left: 3px solid #64748b;
  }

  :deep(h4) {
    font-size: 14px;
    font-weight: 600;
    color: #475569;
    margin: 16px 0 8px;
  }

  :deep(p) {
    font-size: 14px;
    color: #374151;
    line-height: 1.85;
    margin-bottom: 12px;

    &.doc-meta {
      font-size: 12px;
      color: #94a3b8;
      font-style: italic;
      margin-bottom: 20px;
    }
  }

  :deep(ol), :deep(ul) {
    padding-left: 22px;
    margin-bottom: 12px;

    li {
      font-size: 14px;
      color: #374151;
      line-height: 1.8;
      margin-bottom: 6px;
    }
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 16px 0;
    font-size: 13px;

    th {
      background: #f1f5f9;
      padding: 9px 12px;
      text-align: left;
      font-weight: 600;
      color: #374151;
      border: 1px solid #e2e8f0;
    }

    td {
      padding: 8px 12px;
      color: #4b5563;
      border: 1px solid #e2e8f0;
      line-height: 1.6;
    }

    tr:nth-child(even) td {
      background: #f8fafc;
    }
  }

  :deep(strong) {
    color: #1e293b;
  }
}

/* ===== 平板详情页 ===== */
.detail-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
  flex-shrink: 0;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 14px;
  cursor: pointer;
  min-width: 44px;
  min-height: 44px;
  justify-content: center;
  transition: all 0.2s;

  &:hover {
    border-color: #64748b;
    color: #334155;
  }
}

.detail-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 16px 32px;

  .doc-content {
    :deep(p), :deep(li) { font-size: 16px; }
    :deep(th), :deep(td) { font-size: 14px; }
  }
}

/* ===== 平板列表模式 ===== */
.is-tablet {
  .regulations-layout {
    flex-direction: column;
  }

  .doc-panel {
    width: 100%;
    flex: 1;
    border-right: none;
  }

  .cat-tab {
    font-size: 14px;
    padding: 8px 14px;
    min-height: 36px;
  }

  .doc-item {
    padding: 12px 12px;
    min-height: 44px;

    .doc-name { font-size: 16px; }
    .doc-no { font-size: 12px; }
  }
}
</style>
