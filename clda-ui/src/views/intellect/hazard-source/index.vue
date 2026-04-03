<template>
  <div class="hazard-view">
    <!-- 顶部Tab：三大类 -->
    <div class="hazard-tabs">
      <button
        v-for="cat in topCategories"
        :key="cat.id"
        class="hazard-tab"
        :class="{ active: activeCatId === cat.id }"
        @click="switchTopCategory(cat)"
      >
        <el-icon :size="18"><component :is="cat.icon || 'Warning'" /></el-icon>
        <span>{{ cat.name }}</span>
      </button>
    </div>

    <!-- 左右分栏 -->
    <div class="hazard-body">
      <!-- 左侧：子分类导航 -->
      <aside class="hazard-sidebar">
        <template v-for="sub in currentSubCategories" :key="sub.id">
          <div class="sub-group">
            <button
              class="sub-header"
              :class="{ expanded: expandedSubs.has(sub.id), active: activeSubId === sub.id && !sub.children?.length }"
              @click="toggleSub(sub)"
            >
              <el-icon v-if="sub.children?.length" class="sub-chevron" :size="12">
                <component :is="expandedSubs.has(sub.id) ? 'ArrowDown' : 'ArrowRight'" />
              </el-icon>
              <span class="sub-code">{{ sub.code }}</span>
              <span class="sub-name">{{ sub.name }}</span>
            </button>
            <div v-if="expandedSubs.has(sub.id) && sub.children?.length" class="sub-children">
              <button
                v-for="child in sub.children"
                :key="child.id"
                class="sub-child"
                :class="{ active: activeSubId === child.id }"
                @click="selectSubCategory(child)"
              >
                <span class="sub-code">{{ child.code }}</span>
                <span class="sub-name">{{ child.name }}</span>
              </button>
            </div>
          </div>
        </template>
      </aside>

      <!-- 右侧：内容区 -->
      <main class="hazard-main">
        <!-- 未选择状态 -->
        <div v-if="!activeSubId" class="main-empty">
          <el-icon :size="48" class="empty-icon"><Warning /></el-icon>
          <p>请从左侧选择危险源分类进行排查</p>
        </div>

        <!-- 条目列表 + 分析结果 -->
        <template v-else>
          <!-- 条目列表 -->
          <div v-if="!analysisTarget" class="item-list" v-loading="itemsLoading">
            <div class="item-list-header">
              <span>{{ activeSubName }} — 危险源清单</span>
              <span class="item-count">{{ items.length }} 项</span>
            </div>
            <div
              v-for="item in items"
              :key="item.id"
              class="item-card"
              @click="startAnalysis(item)"
            >
              <span class="item-no">{{ item.itemNo }})</span>
              <span class="item-desc">{{ item.description }}</span>
              <el-icon class="item-arrow" :size="14"><ArrowRight /></el-icon>
            </div>
            <div v-if="!itemsLoading && items.length === 0" class="item-empty">
              暂无危险源数据
            </div>
          </div>

          <!-- AI分析结果 -->
          <div v-else class="analysis-panel">
            <button class="analysis-back" @click="analysisTarget = null">
              <el-icon><ArrowLeft /></el-icon> 返回列表
            </button>

            <!-- Step 1: Loading -->
            <div v-if="analysisStep >= 0" class="analysis-loading" :class="{ done: analysisStep >= 1 }">
              <div class="loading-dots" v-if="analysisStep === 0">
                <span></span><span></span><span></span>
              </div>
              <span v-if="analysisStep === 0">正在分析危险源...</span>
              <el-icon v-if="analysisStep >= 1" class="icon-success" :size="18"><CircleCheck /></el-icon>
              <span v-if="analysisStep >= 1">分析完成</span>
            </div>

            <!-- Step 2: 危险源描述 -->
            <transition name="fade-slide">
              <div v-if="analysisStep >= 1" class="analysis-section">
                <div class="section-icon"><el-icon :size="18" class="icon-primary"><Search /></el-icon></div>
                <div class="section-content">
                  <h4 class="section-title">危险源描述</h4>
                  <p class="section-text">{{ analysisTarget.description }}</p>
                </div>
              </div>
            </transition>

            <!-- Step 3: 产生原因 -->
            <transition name="fade-slide">
              <div v-if="analysisStep >= 2" class="analysis-section">
                <div class="section-icon"><el-icon :size="18" class="icon-warning"><WarningFilled /></el-icon></div>
                <div class="section-content">
                  <h4 class="section-title">产生原因</h4>
                  <div v-if="causeGroups.length === 0" class="section-text">该危险源为环境因素，无特定产生原因条款</div>
                  <div v-for="group in causeGroups" :key="group.stage" class="cause-group">
                    <div class="cause-stage">
                      <span class="cause-stage-name">{{ group.stage }}</span>
                      <span class="cause-stage-count">{{ group.items.length }}项</span>
                    </div>
                    <div v-for="cause in group.items" :key="cause.code" class="cause-item">
                      <span class="cause-code">{{ cause.code }}</span>
                      <span class="cause-desc">{{ cause.description }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </transition>

            <!-- Step 4: 危险事件 -->
            <transition name="fade-slide">
              <div v-if="analysisStep >= 3" class="analysis-section">
                <div class="section-icon"><el-icon :size="18" class="icon-error"><Bell /></el-icon></div>
                <div class="section-content">
                  <h4 class="section-title">可能导致的危险事件</h4>
                  <div class="event-list">
                    <div v-for="evt in events" :key="evt.code" class="event-tag">
                      <el-icon :size="16"><component :is="evt.icon || 'Warning'" /></el-icon>
                      <span>{{ evt.name }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </transition>
          </div>
        </template>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  Warning, WarningFilled, ArrowRight, ArrowLeft, ArrowDown,
  Search, Bell, CircleCheck
} from '@element-plus/icons-vue'
import { getHazardSourceTree, getHazardSourceItems, getHazardSourceCauses, getHazardSourceEvents } from '@/api/intellect/hazardSource'

const treeData = ref([])
const activeCatId = ref(null)
const activeSubId = ref(null)
const expandedSubs = ref(new Set())
const items = ref([])
const itemsLoading = ref(false)

// Analysis state
const analysisTarget = ref(null)
const analysisStep = ref(-1)
const causes = ref([])
const events = ref([])

const topCategories = computed(() => treeData.value)

const currentSubCategories = computed(() => {
  const cat = treeData.value.find(c => c.id === activeCatId.value)
  return cat?.children || []
})

const activeSubName = computed(() => {
  for (const sub of currentSubCategories.value) {
    if (sub.id === activeSubId.value) return sub.name
    if (sub.children) {
      const child = sub.children.find(c => c.id === activeSubId.value)
      if (child) return child.name
    }
  }
  return ''
})

const causeGroups = computed(() => {
  const groups = {}
  for (const c of causes.value) {
    const stage = c.stage || '其他'
    if (!groups[stage]) groups[stage] = []
    groups[stage].push(c)
  }
  return Object.entries(groups).map(([stage, items]) => ({ stage, items }))
})

function switchTopCategory(cat) {
  activeCatId.value = cat.id
  activeSubId.value = null
  analysisTarget.value = null
  items.value = []
  expandedSubs.value = new Set()
}

function toggleSub(sub) {
  if (sub.children?.length) {
    const s = new Set(expandedSubs.value)
    if (s.has(sub.id)) s.delete(sub.id); else s.add(sub.id)
    expandedSubs.value = s
  } else {
    selectSubCategory(sub)
  }
}

async function selectSubCategory(sub) {
  activeSubId.value = sub.id
  analysisTarget.value = null
  itemsLoading.value = true
  try {
    const res = await getHazardSourceItems(sub.id)
    items.value = res.data || []
  } catch {
    items.value = []
  }
  itemsLoading.value = false
}

async function startAnalysis(item) {
  analysisTarget.value = item
  analysisStep.value = 0
  causes.value = []
  events.value = []

  // Fetch data in parallel
  const [causesRes, eventsRes] = await Promise.all([
    item.causeCodes ? getHazardSourceCauses(item.causeCodes) : Promise.resolve({ data: [] }),
    item.eventCodes ? getHazardSourceEvents(item.eventCodes) : Promise.resolve({ data: [] })
  ])

  // Step animation
  setTimeout(() => {
    analysisStep.value = 1
  }, 300)

  setTimeout(() => {
    causes.value = causesRes.data || []
    analysisStep.value = 2
  }, 600)

  setTimeout(() => {
    events.value = eventsRes.data || []
    analysisStep.value = 3
  }, 900)
}

onMounted(async () => {
  const res = await getHazardSourceTree()
  treeData.value = res.data || []
  if (treeData.value.length > 0) {
    activeCatId.value = treeData.value[0].id
  }
})
</script>

<style lang="scss" scoped>
.hazard-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--ds-surface);
}

/* ===== 顶部Tab ===== */
.hazard-tabs {
  display: flex;
  gap: var(--ds-space-1);
  padding: var(--ds-space-2) var(--ds-space-2);
  background: var(--ds-surface-container-lowest);
  flex-shrink: 0;
}

.hazard-tab {
  display: flex;
  align-items: center;
  gap: var(--ds-space-1);
  padding: 10px 20px;
  border: none;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-surface-container-low);
  color: var(--ds-on-surface-variant);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover { background: var(--ds-surface-container-high); color: var(--ds-on-surface); }

  &.active {
    background: var(--ds-primary);
    color: var(--ds-on-primary);
  }
}

/* ===== 左右分栏 ===== */
.hazard-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

/* ===== 左侧子分类导航 ===== */
.hazard-sidebar {
  width: 240px;
  flex-shrink: 0;
  overflow-y: auto;
  background: var(--ds-surface-container-lowest);
  padding: var(--ds-space-1);
}

.sub-group {
  margin-bottom: 2px;
}

.sub-header {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  transition: background 0.15s;
  text-align: left;

  &:hover { background: var(--ds-surface-container-low); }
  &.active { background: var(--ds-amber-surface); .sub-name { color: var(--ds-amber); font-weight: 600; } }
  &.expanded { .sub-chevron { color: var(--ds-on-surface); } }
}

.sub-chevron { flex-shrink: 0; color: var(--ds-outline); }

.sub-code {
  font-size: 11px;
  font-weight: 700;
  color: var(--ds-outline);
  min-width: 24px;
}

.sub-name {
  font-size: 13px;
  color: var(--ds-on-surface-variant);
  font-weight: 500;
}

.sub-children {
  padding-left: 16px;
}

.sub-child {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s;

  &:hover { background: var(--ds-surface-container-low); }
  &.active { background: var(--ds-amber-surface); .sub-name { color: var(--ds-amber); font-weight: 600; } }
}

/* ===== 右侧内容区 ===== */
.hazard-main {
  flex: 1;
  overflow-y: auto;
  min-width: 0;
}

.main-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: var(--ds-space-2);
  color: var(--ds-outline);
  font-size: 14px;
}

/* ===== 条目列表 ===== */
.item-list {
  padding: var(--ds-space-2);
}

.item-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--ds-space-2);
  font-size: 15px;
  font-weight: 700;
  color: var(--ds-on-surface);
}

.item-count {
  font-size: 12px;
  font-weight: 600;
  color: var(--ds-outline);
  background: var(--ds-surface-container-low);
  padding: 2px var(--ds-space-1);
  border-radius: var(--ds-radius-full);
}

.item-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: var(--ds-space-2);
  background: var(--ds-surface-container-lowest);
  border-radius: var(--ds-radius);
  margin-bottom: 6px;
  cursor: pointer;
  box-shadow: var(--ds-shadow-sm);
  transition: all 0.15s;

  &:hover {
    box-shadow: var(--ds-shadow-md);
  }
}

.item-no {
  font-size: 13px;
  font-weight: 700;
  color: var(--ds-amber);
  flex-shrink: 0;
  min-width: 24px;
}

.item-desc {
  font-size: 14px;
  color: var(--ds-on-surface);
  line-height: 1.5;
  flex: 1;
}

.item-arrow {
  flex-shrink: 0;
  color: var(--ds-outline-variant);
  margin-top: 3px;
}

.item-empty {
  text-align: center;
  padding: 40px;
  color: var(--ds-outline);
  font-size: 13px;
}

/* ===== AI分析面板 ===== */
.analysis-panel {
  padding: var(--ds-space-2);
}

.analysis-back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: none;
  border-radius: var(--ds-radius);
  background: var(--ds-surface-container-low);
  color: var(--ds-on-surface-variant);
  font-size: 13px;
  cursor: pointer;
  margin-bottom: var(--ds-space-2);
  transition: all 0.2s;

  &:hover { color: var(--ds-amber); }
}

.analysis-loading {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px var(--ds-space-2);
  background: var(--ds-surface-container-lowest);
  border-radius: var(--ds-radius-lg);
  margin-bottom: var(--ds-space-2);
  font-size: 14px;
  color: var(--ds-on-surface-variant);
  box-shadow: var(--ds-shadow-sm);
  transition: all 0.3s;

  &.done {
    background: var(--ds-emerald-surface);
    color: var(--ds-success);
  }
}

.loading-dots {
  display: flex;
  gap: 4px;

  span {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--ds-outline);
    animation: dot-pulse 1.2s infinite ease-in-out;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes dot-pulse {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* ===== 分析结果区块 ===== */
.analysis-section {
  display: flex;
  gap: 14px;
  padding: 16px;
  background: var(--ds-surface-container-lowest);
  box-shadow: var(--ds-shadow-sm);
  border-radius: var(--ds-radius-lg);
  margin-bottom: 10px;
}

.section-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--ds-surface);
  display: flex;
  align-items: center;
  justify-content: center;
}

.section-content {
  flex: 1;
  min-width: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--ds-on-surface);
  margin: 0 0 8px;
}

.section-text {
  font-size: 14px;
  color: var(--ds-on-surface-variant);
  line-height: 1.6;
}

/* 原因分组 */
.cause-group {
  margin-bottom: 10px;
}

.cause-stage {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.cause-stage-name {
  font-size: 12px;
  font-weight: 700;
  color: var(--ds-amber);
  background: var(--ds-amber-surface);
  padding: 1px 8px;
  border-radius: 4px;
}

.cause-stage-count {
  font-size: 11px;
  color: var(--ds-outline);
}

.cause-item {
  display: flex;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
  line-height: 1.5;
}

.cause-code {
  flex-shrink: 0;
  color: var(--ds-outline);
  font-family: monospace;
  font-size: 11px;
  min-width: 52px;
}

.cause-desc {
  color: var(--ds-on-surface-variant);
}

/* 事件标签 */
.event-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.event-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: var(--ds-orange-surface);
  box-shadow: var(--ds-shadow-sm);
  border-radius: var(--ds-radius);
  color: var(--ds-error);
  font-size: 13px;
  font-weight: 500;
}

/* ===== 动画 ===== */
.fade-slide-enter-active {
  transition: all 0.35s ease-out;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

/* ===== 小屏适配 ===== */
@media (max-width: 820px) {
  .hazard-body { flex-direction: column; }
  .hazard-sidebar { width: 100%; max-height: 200px; border-right: none; box-shadow: inset 0 -1px 0 var(--ds-surface-container); }
  .item-desc { font-size: 15px; }
}

/* Utility icon classes */
.empty-icon { color: var(--ds-surface-container); }
.icon-success { color: var(--ds-success); }
.icon-primary { color: var(--ds-primary); }
.icon-warning { color: var(--ds-warning); }
.icon-error { color: var(--ds-error); }
</style>
