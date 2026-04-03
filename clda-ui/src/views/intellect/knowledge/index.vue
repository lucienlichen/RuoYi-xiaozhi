<template>
  <div class="knowledge-view">
    <!-- 三列布局：一本书一列 -->
    <div class="kn-body" v-loading="loading">
      <div v-for="book in books" :key="book.id" class="kn-col">
        <div class="book-card">
          <div class="book-card-header">
            <span class="book-dot" :style="{ background: bookColor(book) }" />
            <span class="book-label">{{ book.title }}</span>
            <span class="book-badge" :style="{ background: bookColorLight(book), color: bookColor(book) }">
              {{ (book.chapters || []).length }}
            </span>
          </div>
          <div class="book-card-body">
            <div
              v-for="(ch, idx) in (book.chapters || [])"
              :key="ch.id"
              class="ch-row"
              :class="{ active: selectedId === ch.id }"
              @click="openChapter(ch, book)"
            >
              <span class="ch-num" :style="{ color: bookColor(book) }">{{ idx + 1 }}</span>
              <span class="ch-title">{{ ch.title }}</span>
            </div>
            <div v-if="!loading && (!book.chapters || book.chapters.length === 0)" class="book-empty">暂无章节</div>
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
          <h3 class="drawer-title">{{ selectedChapter?.title }}</h3>
          <div class="drawer-meta">
            <el-tag v-if="selectedBook" size="small" :color="bookColorLight(selectedBook)" :style="{ color: bookColor(selectedBook), borderColor: bookColor(selectedBook) }">
              {{ selectedBook.title }}
            </el-tag>
            <span v-if="selectedChapter?.fileName" class="drawer-file">{{ selectedChapter.fileName }}</span>
            <a v-if="fileUrl" class="drawer-download" :href="fileUrl" :download="selectedChapter?.fileName" target="_blank">
              <el-icon :size="14"><Download /></el-icon> 下载原文件
            </a>
          </div>
        </div>
      </template>
      <div class="drawer-content" v-loading="chapterLoading">
        <!-- PDF 预览 -->
        <VueOfficePdf v-if="fileType === 'pdf'" :src="fileUrl" class="file-viewer" />
        <!-- DOCX 预览 -->
        <VueOfficeDocx v-else-if="fileType === 'docx'" :src="fileUrl" class="file-viewer" />
        <!-- 其他格式 -->
        <div v-else-if="selectedChapter && !chapterLoading" class="file-fallback">
          <el-icon :size="48" class="file-fallback-icon"><Document /></el-icon>
          <p v-if="fileUrl">该文件格式暂不支持在线预览</p>
          <p v-else>该章节暂无关联文件</p>
          <a v-if="fileUrl" :href="fileUrl" :download="selectedChapter?.fileName" target="_blank">
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
import { getBooksWithTree, getChapter } from '@/api/intellect/knowledge'

const DS_VARS = ['--ds-rose', '--ds-indigo', '--ds-amber']
const DS_SURFACE_VARS = ['--ds-rose-surface', '--ds-indigo-surface', '--ds-amber-surface']

function getCssVar(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

function bookColor(book) {
  const idx = books.value.indexOf(book)
  return getCssVar(DS_VARS[idx % DS_VARS.length])
}
function bookColorLight(book) {
  const idx = books.value.indexOf(book)
  return getCssVar(DS_SURFACE_VARS[idx % DS_SURFACE_VARS.length])
}

const books = ref([])
const loading = ref(false)
const selectedId = ref(null)
const selectedChapter = ref(null)
const selectedBook = ref(null)
const chapterLoading = ref(false)
const drawerVisible = ref(false)

const drawerSize = computed(() => window.innerWidth <= 820 ? '92%' : '70%')

const totalChapters = computed(() =>
  books.value.reduce((sum, b) => sum + (b.chapters?.length || 0), 0)
)

const fileUrl = computed(() => {
  if (!selectedChapter.value?.filePath) return ''
  return import.meta.env.VITE_APP_BASE_API + selectedChapter.value.filePath
})

const fileType = computed(() => {
  const name = selectedChapter.value?.fileName || ''
  const ext = name.split('.').pop().toLowerCase()
  if (ext === 'pdf') return 'pdf'
  if (ext === 'docx') return 'docx'
  return 'other'
})

async function loadBooks() {
  loading.value = true
  try {
    const res = await getBooksWithTree()
    books.value = res.data || []
  } catch {
    books.value = []
  }
  loading.value = false
}

async function openChapter(ch, book) {
  selectedId.value = ch.id
  selectedBook.value = book
  drawerVisible.value = true
  chapterLoading.value = true
  try {
    const res = await getChapter(ch.id)
    selectedChapter.value = res.data
  } catch {
    selectedChapter.value = ch
  }
  chapterLoading.value = false
}

function onDrawerClose() {
  selectedId.value = null
  selectedChapter.value = null
  selectedBook.value = null
}

onMounted(() => {
  loadBooks()
})
</script>

<style lang="scss" scoped>
.knowledge-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--ds-surface);
}


/* ===== 三列内容区 ===== */
.kn-body {
  flex: 1;
  display: flex;
  gap: var(--ds-space-3);
  padding: var(--ds-space-4);
  overflow: hidden;
  min-height: 0;
}

.kn-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ===== 书籍卡片 ===== */
.book-card {
  background: var(--ds-surface-container-lowest);
  border-radius: var(--ds-radius-lg);
  box-shadow: var(--ds-shadow-sm);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex: 1;
  min-height: 0;
}

.book-card-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--ds-surface-container-low);
}

.book-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}

.book-label {
  font-size: 14px;
  font-weight: 700;
  color: var(--ds-on-surface);
  flex: 1;
}

.book-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: var(--ds-radius-full);
  flex-shrink: 0;
}

.book-card-body {
  flex: 1;
  overflow-y: auto;
  padding: 4px 6px;
}

.book-empty {
  text-align: center;
  padding: 24px 12px;
  color: var(--ds-outline);
  font-size: 13px;
}

/* ===== 章节行 ===== */
.ch-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 9px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.12s;

  &:hover { background: var(--ds-surface-container-low); }

  &.active {
    background: var(--ds-rose-surface);
    .ch-title { color: var(--ds-rose); font-weight: 600; }
  }
}

.ch-num {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--ds-rose-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  margin-top: 1px;
}

.ch-title {
  font-size: 14px;
  color: var(--ds-on-surface-variant);
  line-height: 1.5;
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
  color: var(--ds-rose);
  text-decoration: none;
  cursor: pointer;

  &:hover { color: var(--ds-rose); filter: brightness(0.85); }
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

  &-icon { color: var(--ds-outline); }
}

/* ===== Robot / 小屏适配 ===== */
@media (max-width: 820px) {
  .kn-header { padding: 12px 14px; }
  .kn-title { font-size: 15px; }

  .kn-body {
    flex-direction: column;
    overflow-y: auto;
    padding: 10px;
  }

  .kn-col {
    flex: none;
    overflow: visible;
  }

  .book-card {
    flex: none;
    min-height: auto;
  }

  .ch-row { padding: 10px; }
  .ch-title { font-size: 15px; }
}

/* ===== Portrait 8-inch (800x1280) ===== */
@media (max-width: 820px) and (orientation: portrait) {
  .kn-body {
    flex-direction: column;
    overflow-y: auto;
    padding: 10px;
    gap: var(--ds-space-3);
  }

  .kn-col {
    flex: none;
    overflow: visible;
  }

  .book-card {
    flex: none;
    min-height: auto;
  }

  .book-label { font-size: 16px; }
  .book-badge { font-size: 13px; padding: 3px 10px; }

  .ch-row {
    padding: 12px 10px;
    min-height: 44px;
  }

  .ch-title { font-size: 16px; }
  .ch-num { width: 30px; height: 30px; font-size: 14px; }
}
</style>
