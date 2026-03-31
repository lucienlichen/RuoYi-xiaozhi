<template>
  <div class="knowledge-view" :class="{ 'is-tablet': isTablet }">

    <!-- PC 左右分栏 / 平板列表页 -->
    <div v-show="!isTablet || !isDetailMode" class="knowledge-layout">

      <!-- 左侧目录面板 -->
      <aside class="catalog-panel">
        <div class="catalog-header">
          <el-icon :size="16" color="#EC4899"><Reading /></el-icon>
          <span>知识目录</span>
        </div>
        <div class="catalog-body">
          <template v-for="book in books" :key="book.id">
            <div class="book-item">
              <div class="book-title">
                <el-icon :size="14"><Collection /></el-icon>
                <span>{{ book.title }}</span>
              </div>
              <div class="chapter-tree">
                <template v-for="chapter in book.chapters" :key="chapter.id">
                  <div
                    class="tree-node level-1"
                    :class="{ active: selectedChapterId === chapter.id, expanded: expandedIds.has(chapter.id) }"
                    @click="handleNodeClick(chapter)"
                  >
                    <el-icon v-if="chapter.children && chapter.children.length" class="expand-icon" :size="12">
                      <component :is="expandedIds.has(chapter.id) ? ArrowDown : ArrowRight" />
                    </el-icon>
                    <span class="node-icon" v-else>·</span>
                    <span class="node-label">{{ chapter.title }}</span>
                  </div>
                  <template v-if="expandedIds.has(chapter.id) && chapter.children">
                    <template v-for="section in chapter.children" :key="section.id">
                      <div
                        class="tree-node level-2"
                        :class="{ active: selectedChapterId === section.id, expanded: expandedIds.has(section.id) }"
                        @click="handleNodeClick(section)"
                      >
                        <el-icon v-if="section.children && section.children.length" class="expand-icon" :size="11">
                          <component :is="expandedIds.has(section.id) ? ArrowDown : ArrowRight" />
                        </el-icon>
                        <span class="node-icon" v-else>–</span>
                        <span class="node-label">{{ section.title }}</span>
                      </div>
                      <template v-if="expandedIds.has(section.id) && section.children">
                        <div
                          v-for="sub in section.children"
                          :key="sub.id"
                          class="tree-node level-3"
                          :class="{ active: selectedChapterId === sub.id }"
                          @click="handleNodeClick(sub)"
                        >
                          <span class="node-icon">∙</span>
                          <span class="node-label">{{ sub.title }}</span>
                        </div>
                      </template>
                    </template>
                  </template>
                </template>
              </div>
            </div>
          </template>
        </div>
      </aside>

      <!-- 右侧内容区（PC 始终显示，平板隐藏） -->
      <main v-if="!isTablet" class="content-panel">
        <div v-if="!selectedChapterContent" class="content-empty">
          <el-icon :size="48" color="#e2e8f0"><Reading /></el-icon>
          <p>请从左侧目录选择章节阅读</p>
        </div>
        <template v-else>
          <div class="content-body" v-loading="contentLoading">
            <div class="chapter-content" v-html="selectedChapterContent.contentHtml" />
          </div>
          <div class="content-nav">
            <button class="nav-btn" :disabled="!prevChapter" @click="goChapter(prevChapter)">
              <el-icon><ArrowLeft /></el-icon> 上一节
            </button>
            <button class="nav-btn" :disabled="!nextChapter" @click="goChapter(nextChapter)">
              下一节 <el-icon><ArrowRight /></el-icon>
            </button>
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
        <span class="detail-title">{{ selectedChapterContent?.title }}</span>
      </div>
      <div class="detail-body" v-loading="contentLoading">
        <div class="chapter-content" v-html="selectedChapterContent?.contentHtml" />
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { Reading, Collection, ArrowDown, ArrowRight, ArrowLeft } from '@element-plus/icons-vue'
import { getBooksWithTree, getChapter } from '@/api/intellect/knowledge'

const books = ref([])
const pageLoading = ref(false)

const selectedChapterId = ref(null)
const selectedChapterContent = ref(null)
const contentLoading = ref(false)
const expandedIds = ref(new Set())
const isDetailMode = ref(false)
const isTablet = ref(false)

function checkScreenSize() {
  isTablet.value = window.innerWidth <= 820
}

onMounted(() => {
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
  pageLoading.value = true
  getBooksWithTree().then(res => {
    books.value = res.data || []
    pageLoading.value = false
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkScreenSize)
})

// Walk tree to find chapter by id
function findChapter(chapters, id) {
  for (const ch of chapters) {
    if (ch.id === id) return ch
    if (ch.children?.length) {
      const found = findChapter(ch.children, id)
      if (found) return found
    }
  }
  return null
}

// Flat list of all leaf chapters (chapters with no children) for prev/next nav
function collectLeaves(chapters, result = []) {
  for (const ch of chapters) {
    if (!ch.children || ch.children.length === 0) {
      result.push(ch)
    } else {
      collectLeaves(ch.children, result)
    }
  }
  return result
}

const flatChapters = computed(() => {
  const leaves = []
  for (const book of books.value) {
    collectLeaves(book.chapters, leaves)
  }
  return leaves
})

const currentIndex = computed(() => flatChapters.value.findIndex(c => c.id === selectedChapterId.value))
const prevChapter = computed(() => currentIndex.value > 0 ? flatChapters.value[currentIndex.value - 1] : null)
const nextChapter = computed(() => currentIndex.value < flatChapters.value.length - 1 ? flatChapters.value[currentIndex.value + 1] : null)

async function handleNodeClick(node) {
  if (node.children && node.children.length > 0) {
    // Toggle expand for parent nodes
    const ids = new Set(expandedIds.value)
    if (ids.has(node.id)) {
      ids.delete(node.id)
    } else {
      ids.add(node.id)
    }
    expandedIds.value = ids
    selectedChapterId.value = node.id
    selectedChapterContent.value = node
    if (isTablet.value) isDetailMode.value = true
  } else {
    // Leaf node: fetch full content from API
    selectedChapterId.value = node.id
    if (isTablet.value) isDetailMode.value = true
    contentLoading.value = true
    const res = await getChapter(node.id)
    selectedChapterContent.value = res.data
    contentLoading.value = false
  }
}

async function goChapter(chapter) {
  if (!chapter) return
  selectedChapterId.value = chapter.id
  expandParentsOf(chapter.id)
  contentLoading.value = true
  const res = await getChapter(chapter.id)
  selectedChapterContent.value = res.data
  contentLoading.value = false
}

function expandParentsOf(targetId) {
  function findAndExpand(chapters, parentId) {
    for (const ch of chapters) {
      if (ch.id === targetId) return true
      if (ch.children?.length) {
        if (findAndExpand(ch.children, ch.id)) {
          const ids = new Set(expandedIds.value)
          ids.add(ch.id)
          expandedIds.value = ids
          return true
        }
      }
    }
    return false
  }
  for (const book of books.value) {
    findAndExpand(book.chapters, null)
  }
}
</script>

<style lang="scss" scoped>
.knowledge-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.knowledge-layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}

/* ===== 左侧目录 ===== */
.catalog-panel {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e2e8f0;
  background: #f8fafc;
  overflow: hidden;
}

.catalog-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  font-size: 13px;
  font-weight: 700;
  color: #374151;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
  background: #fff;
}

.catalog-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.book-item {
  margin-bottom: 4px;
}

.book-title {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px 4px;
  font-size: 12px;
  font-weight: 700;
  color: #EC4899;
  letter-spacing: 0.02em;
}

.chapter-tree {
  padding: 0;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  cursor: pointer;
  font-size: 13px;
  color: #475569;
  border-radius: 6px;
  margin: 1px 6px;
  transition: background 0.15s, color 0.15s;
  line-height: 1.4;

  &:hover {
    background: #f1f5f9;
    color: #1e293b;
  }

  &.active {
    background: #fce7f3;
    color: #be185d;
    font-weight: 600;
  }

  &.level-1 {
    padding-left: 16px;
    font-weight: 500;
  }

  &.level-2 {
    padding-left: 28px;
    font-size: 13px;
  }

  &.level-3 {
    padding-left: 40px;
    font-size: 12px;
  }
}

.expand-icon {
  flex-shrink: 0;
  color: #94a3b8;
  transition: transform 0.2s;
}

.node-icon {
  flex-shrink: 0;
  width: 12px;
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
}

.node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 右侧内容 ===== */
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

.content-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
}

.content-nav {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  padding: 12px 24px;
  border-top: 1px solid #f1f5f9;
  background: #fafafa;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    border-color: #EC4899;
    color: #EC4899;
    background: #fce7f3;
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

/* ===== 章节内容渲染 ===== */
.chapter-content {
  :deep(h2) {
    font-size: 20px;
    font-weight: 700;
    color: #1e293b;
    margin: 0 0 20px;
    padding-bottom: 12px;
    border-bottom: 2px solid #fce7f3;
  }

  :deep(h3) {
    font-size: 16px;
    font-weight: 600;
    color: #374151;
    margin: 20px 0 10px;
  }

  :deep(h4) {
    font-size: 14px;
    font-weight: 600;
    color: #4b5563;
    margin: 16px 0 8px;
  }

  :deep(p) {
    font-size: 14px;
    color: #374151;
    line-height: 1.8;
    margin-bottom: 12px;
  }

  :deep(ul), :deep(ol) {
    padding-left: 20px;
    margin-bottom: 12px;

    li {
      font-size: 14px;
      color: #374151;
      line-height: 1.8;
      margin-bottom: 4px;
    }
  }

  :deep(dl) {
    dt {
      font-weight: 600;
      color: #1e293b;
      margin-top: 12px;
    }
    dd {
      margin-left: 16px;
      font-size: 14px;
      color: #4b5563;
      line-height: 1.7;
    }
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 16px 0;
    font-size: 13px;

    th {
      background: #f1f5f9;
      padding: 8px 12px;
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

  :deep(blockquote) {
    border-left: 4px solid #EC4899;
    background: #fce7f3;
    padding: 12px 16px;
    margin: 16px 0;
    border-radius: 0 8px 8px 0;

    p {
      margin: 0;
      color: #9d174d;
    }
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
  padding: 14px 16px;
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
    border-color: #EC4899;
    color: #EC4899;
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
  padding: 20px 16px;

  .chapter-content {
    :deep(h2) { font-size: 18px; }
    :deep(p), :deep(li) { font-size: 16px; }
    :deep(th), :deep(td) { font-size: 14px; }
  }
}

/* ===== 平板列表模式：目录全屏 ===== */
.is-tablet {
  .knowledge-layout {
    flex-direction: column;
  }

  .catalog-panel {
    width: 100%;
    flex: 1;
    border-right: none;
  }

  .tree-node {
    padding-top: 10px;
    padding-bottom: 10px;
    font-size: 16px;
    min-height: 44px;

    &.level-2 { font-size: 15px; }
    &.level-3 { font-size: 14px; }
  }
}
</style>
