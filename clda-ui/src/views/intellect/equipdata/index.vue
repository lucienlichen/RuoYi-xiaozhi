<template>
  <div class="equipdata-page">
    <!-- Data type tab bar — always visible -->
    <nav class="data-tabs">
      <template v-for="(cat, idx) in mainCategories" :key="cat.id">
        <a
          class="data-tab"
          :class="{
            active: activeCategory === String(cat.id),
            disabled: cat.enabled === '0'
          }"
          @click="cat.enabled !== '0' && switchCategory(String(cat.id))"
        >
          {{ cat.categoryName }}
          <span v-if="cat.enabled === '0'" class="tab-badge">*</span>
        </a>
        <!-- 分隔线：维修|检验、检测|风险、监测|基础信息 -->
        <span v-if="tabSeparatorAfter.has(idx)" class="tab-divider"></span>
      </template>
      <!-- Fallback static tabs while categories load -->
      <template v-if="mainCategories.length === 0 && !categoriesLoaded">
        <a v-for="name in defaultTabNames" :key="name" class="data-tab placeholder">{{ name }}</a>
      </template>
    </nav>

    <!-- No equipment selected prompt -->
    <div v-if="!selectedEquipment" class="no-equip-state">
      <el-icon :size="48" color="#cbd5e1"><Monitor /></el-icon>
      <p>请先在左侧选择一台设备</p>
    </div>

    <!-- Main content grid (when equipment selected) -->
    <template v-else>
      <section class="content-grid">
        <!-- Left: Calendar area -->
        <div class="calendar-col" v-if="currentDateMode !== 'none'">
          <!-- Action buttons -->
          <div class="action-btns">
            <button class="btn-import-primary" @click="triggerUpload">
              <el-icon><CirclePlus /></el-icon>
              <span>导入{{ currentCategoryName }}数据</span>
            </button>
            <button class="btn-import-secondary" @click="showHistoryImport = true">
              <el-icon><Clock /></el-icon>
              <span>历史导入</span>
            </button>
          </div>

          <!-- Year picker mode -->
          <div v-if="currentDateMode === 'year'" class="year-picker-card">
            <div class="year-picker-label">选择数据年份</div>
            <el-date-picker
              v-model="selectedYear"
              type="year"
              placeholder="选择年份"
              value-format="YYYY"
              :teleported="true"
              @change="loadDataList"
              style="width: 100%"
            />
            <div class="year-picker-hint">
              <el-icon color="#003178"><Calendar /></el-icon>
              <span>{{ selectedYear || '未选择' }}年 · {{ currentCategoryName }}</span>
            </div>
          </div>

          <!-- Custom calendar -->
          <div v-else class="calendar-card">
            <div class="calendar-header">
              <div class="calendar-header-left">
                <el-icon color="#003178"><Calendar /></el-icon>
                <span class="month-count">本月 {{ monthDataCount }} 条</span>
              </div>
              <div class="calendar-nav">
                <button class="cal-nav-btn" @click="prevMonth"><el-icon><ArrowLeft /></el-icon></button>
                <button class="cal-nav-btn" @click="nextMonth"><el-icon><ArrowRight /></el-icon></button>
              </div>
            </div>
            <div class="calendar-title">
              <span>{{ calendarYear }}年 {{ calendarMonthName }}</span>
              <button class="today-link" @click="goToday">今天</button>
            </div>
            <div class="calendar-weekdays">
              <span v-for="d in weekdays" :key="d">{{ d }}</span>
            </div>
            <div class="calendar-days">
              <button
                v-for="(day, idx) in calendarDays"
                :key="idx"
                class="calendar-day"
                :class="{
                  other: day.otherMonth,
                  selected: day.selected,
                  today: day.today,
                  'has-data': day.hasData
                }"
                @click="!day.otherMonth && selectDay(day.date)"
              >
                {{ day.label }}
                <span v-if="day.dataCount > 0" class="day-count">{{ day.dataCount }}</span>
              </button>
            </div>
            <div class="calendar-legend">
              <div class="legend-item"><span class="legend-dot today"></span><span>今日</span></div>
              <div class="legend-item"><span class="legend-box">3</span><span>当日采集数</span></div>
              <div class="legend-hint">点击日期查看文件</div>
            </div>
          </div>

          <!-- Tip card -->
          <div class="tip-card">
            <div class="tip-header">
              <el-icon color="#003178"><InfoFilled /></el-icon>
              <span>{{ currentCategoryName }}提示</span>
            </div>
            <p class="tip-text">
              今日尚未完成"{{ selectedEquipment.equipmentName }}"的{{ currentCategoryName }}数据录入。请及时上传检查表扫描件或手动录入结构化数据。
            </p>
          </div>
        </div>

        <!-- Right: File content area -->
        <div class="file-col" :class="{ 'full-width': currentDateMode === 'none' }">
          <!-- Date header -->
          <div class="file-header">
            <div class="file-header-left">
              <span class="file-date">{{ currentDateLabel }}</span>
              <span class="file-count">{{ totalFileCount }} 个文件</span>
            </div>
          </div>

          <!-- Hidden upload input -->
          <el-upload
            ref="uploadRef"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :data="uploadData"
            :before-upload="beforeUpload"
            :on-success="onUploadSuccess"
            :on-error="onUploadError"
            :show-file-list="false"
            name="files"
            multiple
            accept=".jpg,.jpeg,.png,.pdf,.doc,.docx,.xls,.xlsx,.csv"
            class="hidden-upload"
          >
            <template #trigger><span ref="uploadTrigger"></span></template>
          </el-upload>

          <!-- Empty state -->
          <div v-if="dataRecords.length === 0 && !fileLoading" class="empty-state" @drop.prevent="onDrop" @dragover.prevent>
            <div class="empty-icon-wrap">
              <el-icon :size="48" color="#cbd5e1"><UploadFilled /></el-icon>
            </div>
            <h2 class="empty-title">该日期暂无数据文件</h2>
            <p class="empty-desc">
              支持：照片 · PDF · Excel · Word<br/>上传后 AI 助手将自动进行图像纠偏、去底噪及文字提取
            </p>
            <div class="empty-actions">
              <button class="btn-upload-main" @click="triggerUpload">
                <el-icon><CirclePlus /></el-icon>
                <span>+ 上传今日数据</span>
              </button>
              <button class="btn-batch-link" @click="showHistoryImport = true">
                批量导入历史文件
              </button>
            </div>
            <div class="drag-hint">
              <div class="drag-line"></div>
              <span>或者 拖拽文件到此处直接上传</span>
              <div class="drag-line"></div>
            </div>
          </div>

          <!-- File list with data -->
          <div v-else class="file-list" v-loading="fileLoading">
            <template v-for="record in dataRecords" :key="record.id">
              <div v-for="file in record.files || []" :key="file.id" class="file-item">
                <div class="file-icon clickable" @click="openDrawer(file)">
                  <el-icon v-if="file.fileType === 'image'" :size="24" color="#10b981"><Picture /></el-icon>
                  <el-icon v-else-if="file.fileType === 'pdf'" :size="24" color="#ef4444"><Document /></el-icon>
                  <el-icon v-else-if="file.fileType === 'excel'" :size="24" color="#3b82f6"><Grid /></el-icon>
                  <el-icon v-else :size="24" color="#94a3b8"><Document /></el-icon>
                </div>
                <div class="file-info">
                  <div class="file-name clickable" @click="openDrawer(file)">{{ file.fileName }}</div>
                  <div class="file-meta">{{ formatSize(file.fileSize) }} · {{ file.fileType }}</div>
                </div>
                <div class="file-stages">
                  <el-tag size="small" :type="stageTagType(file.preprocessStatus)">
                    预处理: {{ stageLabel(file.preprocessStatus) }}
                  </el-tag>
                  <el-tag size="small" :type="stageTagType(file.ocrStatus)">
                    OCR: {{ stageLabel(file.ocrStatus) }}
                  </el-tag>
                  <el-tag v-if="file.structuredData" size="small" type="success">已结构化</el-tag>
                  <el-tag v-else-if="file.ocrStatus === 'DONE'" size="small" type="warning">待结构化</el-tag>
                </div>
                <el-button
                  v-if="file.structuredData || file.ocrStatus === 'DONE'"
                  link type="primary" icon="View"
                  @click="openStructuredView(file)"
                >查看</el-button>
                <el-button link type="danger" icon="Delete" @click="handleDeleteFile(file)" />
              </div>
            </template>
          </div>
        </div>
      </section>
    </template>

    <!-- File detail drawer -->
    <el-drawer v-model="drawerVisible" :title="drawerFile?.fileName" size="520px" direction="rtl" destroy-on-close>
      <div v-if="drawerFile" class="drawer-content">
        <!-- 原始数据 -->
        <div class="drawer-section">
          <h4>原始数据</h4>
          <img v-if="drawerFile.fileType === 'image'" :src="fileUrl(drawerFile.filePath)" class="drawer-img" />
          <iframe v-else-if="drawerFile.fileType === 'pdf'" :src="fileUrl(drawerFile.filePath)" class="drawer-pdf"></iframe>
          <div v-else class="drawer-nopreview">该文件类型暂不支持预览</div>
        </div>
        <!-- 预处理结果 -->
        <div v-if="drawerFile.preprocessedPath" class="drawer-section">
          <h4>预处理结果（二值化+纠偏）</h4>
          <img :src="fileUrl(drawerFile.preprocessedPath)" class="drawer-img" />
        </div>
        <!-- 增强结果 -->
        <div v-if="drawerFile.enhancedPath" class="drawer-section">
          <h4>AI增强结果</h4>
          <img :src="fileUrl(drawerFile.enhancedPath)" class="drawer-img" />
        </div>
        <!-- 处理状态 -->
        <div class="drawer-section">
          <h4>处理状态</h4>
          <div class="drawer-status-row">
            <span>预处理:</span>
            <el-tag size="small" :type="stageTagType(drawerFile.preprocessStatus)">{{ stageLabel(drawerFile.preprocessStatus) }}</el-tag>
          </div>
          <div class="drawer-status-row">
            <span>OCR:</span>
            <el-tag size="small" :type="stageTagType(drawerFile.ocrStatus)">{{ stageLabel(drawerFile.ocrStatus) }}</el-tag>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Picture, Document, Grid, UploadFilled, CirclePlus, Clock, Monitor,
  Calendar, ArrowLeft, ArrowRight, InfoFilled
} from '@element-plus/icons-vue'
import { listCategories, listEquipData, getDataFiles, getDataDates, delDataFile, getFilesStatus } from '@/api/intellect/equipdata'
import { getToken } from '@/utils/auth'
import useEquipmentStore from '@/store/modules/equipment'

const { proxy } = getCurrentInstance()
const equipmentStore = useEquipmentStore()

const selectedEquipment = computed(() => equipmentStore.selectedEquipment)

// Categories — loaded immediately on mount, independent of equipment
const allCategories = ref([])
const categoriesLoaded = ref(false)
const activeCategory = ref('')
const showHistoryImport = ref(false)

// Fallback tab names shown while database categories load
const defaultTabNames = [
  '点巡检数据', '故障数据', '维修数据', '检验数据', '检测数据', '风险数据',
  '运行数据*', '监控数据*', '监测数据*', '基础信息', '出厂数据'
]

// Calendar state
const calendarDate = ref(new Date())
const selectedDate = ref(new Date())
const selectedYear = ref(String(new Date().getFullYear()))
const dataDates = ref([])
const dateDateCounts = ref({})

// Data & Files
const dataRecords = ref([])
const fileLoading = ref(false)

// Upload
const uploadRef = ref(null)
const uploadTrigger = ref(null)
const uploadUrl = computed(() => import.meta.env.VITE_APP_BASE_API + '/intellect/equipdata/upload')
const uploadHeaders = computed(() => ({ Authorization: 'Bearer ' + getToken() }))
const uploadData = computed(() => {
  const data = {
    equipmentId: selectedEquipment.value?.id,
    categoryId: activeCategory.value,
  }
  if (currentDateMode.value === 'day') {
    data.dataDate = formatDate(selectedDate.value)
  }
  return data
})

// Computed
const mainCategories = computed(() => allCategories.value.filter(c => String(c.parentId) === '0'))
// 分隔线位置：维修(idx=2)后、检测(idx=4)后、监测(idx=8)后
const tabSeparatorAfter = computed(() => new Set([2, 4, 8]))
const currentDateMode = computed(() => {
  const cat = mainCategories.value.find(c => String(c.id) === activeCategory.value)
  return cat ? cat.dateMode : 'day'
})
const currentCategoryName = computed(() => {
  const cat = mainCategories.value.find(c => String(c.id) === activeCategory.value)
  return cat ? cat.categoryName : '数据'
})
const dataDatesSet = computed(() => new Set(dataDates.value))
const currentDateLabel = computed(() => {
  if (currentDateMode.value === 'year') return selectedYear.value + '年'
  if (currentDateMode.value === 'none') return '全部'
  return formatDate(selectedDate.value)
})
const totalFileCount = computed(() => {
  let count = 0
  dataRecords.value.forEach(r => { count += (r.files || []).length })
  return count
})
const monthDataCount = computed(() => dataDates.value.length)

// Calendar computed
const weekdays = ['日', '一', '二', '三', '四', '五', '六']
const monthNames = ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月']
const calendarYear = computed(() => calendarDate.value.getFullYear())
const calendarMonthName = computed(() => monthNames[calendarDate.value.getMonth()])

const calendarDays = computed(() => {
  const year = calendarDate.value.getFullYear()
  const month = calendarDate.value.getMonth()
  const firstDay = new Date(year, month, 1)
  const startDay = firstDay.getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const today = new Date()
  const todayStr = formatDate(today)
  const selectedStr = formatDate(selectedDate.value)
  const days = []

  // Previous month filler
  const prevMonthDays = new Date(year, month, 0).getDate()
  for (let i = startDay - 1; i >= 0; i--) {
    days.push({ label: prevMonthDays - i, otherMonth: true, date: null })
  }

  // Current month days
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    const dateObj = new Date(year, month, d)
    days.push({
      label: d,
      otherMonth: false,
      date: dateObj,
      selected: dateStr === selectedStr,
      today: dateStr === todayStr,
      hasData: dataDatesSet.value.has(dateStr),
      dataCount: dateDateCounts.value[dateStr] || 0
    })
  }

  return days
})

// Load categories immediately — no equipment needed
onMounted(async () => {
  await loadCategories()
  if (!equipmentStore.loaded) {
    await equipmentStore.loadEquipments()
  }
})

async function loadCategories() {
  try {
    const res = await listCategories()
    allCategories.value = res.data || []
    if (mainCategories.value.length > 0) {
      activeCategory.value = String(mainCategories.value.find(c => c.enabled === '1')?.id || mainCategories.value[0].id)
    }
  } catch (e) {
    console.warn('Failed to load categories:', e)
  } finally {
    categoriesLoaded.value = true
  }
}

function switchCategory(id) {
  activeCategory.value = id
  if (selectedEquipment.value) {
    loadDataList()
    loadDataDates()
  }
}

function selectDay(date) {
  if (!date) return
  selectedDate.value = date
}

function prevMonth() {
  const d = new Date(calendarDate.value)
  d.setMonth(d.getMonth() - 1)
  calendarDate.value = d
  if (selectedEquipment.value) loadDataDates()
}

function nextMonth() {
  const d = new Date(calendarDate.value)
  d.setMonth(d.getMonth() + 1)
  calendarDate.value = d
  if (selectedEquipment.value) loadDataDates()
}

function goToday() {
  const today = new Date()
  calendarDate.value = today
  selectedDate.value = today
  if (selectedEquipment.value) loadDataDates()
}

function triggerUpload() {
  uploadTrigger.value?.click()
}

function onDrop(e) {
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    for (const file of files) {
      uploadRef.value?.submit()
    }
  }
}

// Watch for equipment selection changes
watch(selectedEquipment, () => {
  if (selectedEquipment.value) {
    loadDataList()
    loadDataDates()
  }
})
watch(selectedDate, () => { if (selectedEquipment.value) loadDataList() })
watch(selectedYear, () => { if (selectedEquipment.value) loadDataList() })
async function loadDataList() {
  if (!selectedEquipment.value || !activeCategory.value) return
  fileLoading.value = true
  const params = {
    equipmentId: selectedEquipment.value.id,
    categoryId: activeCategory.value,
    pageNum: 1,
    pageSize: 100,
  }
  if (currentDateMode.value === 'day') {
    params.dataDate = formatDate(selectedDate.value)
  }
  // year 和 none 模式不传 dataDate，查询该分类所有数据
  try {
    const res = await listEquipData(params)
    const records = res.rows || []
    for (const r of records) {
      const filesRes = await getDataFiles(r.id)
      r.files = filesRes.data || []
    }
    dataRecords.value = records
  } finally {
    fileLoading.value = false
  }
}

async function loadDataDates() {
  if (!selectedEquipment.value || !activeCategory.value) return
  const d = calendarDate.value
  const ym = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
  const res = await getDataDates({ equipmentId: selectedEquipment.value.id, categoryId: activeCategory.value, yearMonth: ym })
  dataDates.value = res.data || []
  const counts = {}
  ;(res.data || []).forEach(dateStr => { counts[dateStr] = (counts[dateStr] || 0) + 1 })
  dateDateCounts.value = counts
}

function beforeUpload(file) {
  if (!selectedEquipment.value) {
    ElMessage.warning('请先选择设备')
    return false
  }
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过50MB')
    return false
  }
  return true
}

function onUploadSuccess(res) {
  if (res.code === 200) {
    ElMessage.success('上传成功，系统正在自动处理...')
    loadDataList()
    loadDataDates()
    // Start polling for processing status
    if (res.data?.id) {
      startPolling(res.data.id)
    }
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

function onUploadError() { ElMessage.error('上传失败') }

function handleDeleteFile(file) {
  proxy.$modal.confirm('确认删除文件 "' + file.fileName + '"？').then(() => delDataFile(file.id)).then(() => {
    ElMessage.success('删除成功')
    loadDataList()
  }).catch(() => {})
}

// --- Polling for async processing status ---
const emit = defineEmits(['open-structured-view'])
let pollingTimer = null
let pollingDataIds = new Set()

function startPolling(dataId) {
  pollingDataIds.add(dataId)
  if (pollingTimer) return // already running
  pollingTimer = setInterval(async () => {
    for (const id of [...pollingDataIds]) {
      try {
        const res = await getFilesStatus(id)
        const files = res.data || []
        // Update local state
        const record = dataRecords.value.find(r => String(r.id) === String(id))
        if (record) record.files = files
        // Check if all files done
        const allDone = files.every(f => f.ocrStatus === 'DONE' || f.ocrStatus === 'FAILED' || f.fileType === 'other')
        if (allDone) {
          pollingDataIds.delete(id)
        }
      } catch (e) {
        console.warn('Polling failed for dataId:', id, e)
      }
    }
    if (pollingDataIds.size === 0) {
      clearInterval(pollingTimer)
      pollingTimer = null
    }
  }, 3000)
}

function openStructuredView(file) {
  emit('open-structured-view', file)
}

// --- File detail drawer ---
const drawerVisible = ref(false)
const drawerFile = ref(null)

function openDrawer(file) {
  drawerFile.value = file
  drawerVisible.value = true
}

function fileUrl(path) {
  if (!path) return ''
  return import.meta.env.VITE_APP_BASE_API + path
}

// Clean up on unmount
import { onUnmounted } from 'vue'
onUnmounted(() => {
  if (pollingTimer) { clearInterval(pollingTimer); pollingTimer = null }
})

function formatDate(d) {
  if (!d) return ''
  const date = new Date(d)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function formatSize(bytes) {
  if (!bytes) return '0B'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}

function stageTagType(status) {
  return { NONE: 'info', PROCESSING: '', DONE: 'success', FAILED: 'danger' }[status] || 'info'
}

function stageLabel(status) {
  return { NONE: '待处理', PROCESSING: '处理中', DONE: '已完成', FAILED: '出错' }[status] || status
}
</script>

<style lang="scss" scoped>
.equipdata-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* Tab navigation — always visible */
.data-tabs {
  display: flex;
  align-items: center;
  padding: 0 24px;
  overflow-x: auto;
  white-space: nowrap;
  scrollbar-width: none;
  flex-shrink: 0;
  background: var(--ds-surface-container-lowest);

  &::-webkit-scrollbar { display: none; }
}

.data-tab {
  position: relative;
  padding: 14px 0;
  margin-right: 28px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ds-on-surface-variant);
  cursor: pointer;
  transition: color 0.2s;
  text-decoration: none;
  flex-shrink: 0;

  &:hover { color: var(--ds-primary); }

  &.active {
    color: var(--ds-primary);
    font-weight: 700;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 2px;
      background: var(--ds-primary);
      border-radius: 1px;
    }
  }

  &.disabled {
    color: var(--ds-outline-variant);
    cursor: not-allowed;
    &:hover { color: var(--ds-outline-variant); }
  }

  &.placeholder {
    color: var(--ds-outline-variant);
    cursor: default;
  }

  .tab-badge {
    color: var(--ds-outline-variant);
    font-size: 11px;
  }
}

.tab-divider {
  width: 1px;
  height: 18px;
  background: var(--ds-outline-variant);
  margin: 0 6px;
  flex-shrink: 0;
  align-self: center;
  opacity: 0.5;
}

/* No equipment state */
.no-equip-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--ds-outline);
  font-size: 15px;
}

/* Content grid */
.content-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 24px;
  padding: 24px;
  overflow-y: auto;
  min-height: 0;
}

/* Left calendar column */
.calendar-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.action-btns {
  display: flex;
  gap: 10px;
}

.btn-import-primary {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  border: none;
  border-radius: var(--ds-radius);
  background: var(--ds-primary);
  color: var(--ds-on-primary);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: var(--ds-shadow-sm);
  transition: all 0.2s;
  white-space: nowrap;

  &:hover { opacity: 0.9; transform: translateY(-1px); }
}

.btn-import-secondary {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  border: none;
  border-radius: var(--ds-radius);
  background: var(--ds-surface-container-low);
  color: var(--ds-warning);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &:hover { background: var(--ds-surface-container); }
}

/* Calendar card */
.calendar-card {
  background: var(--ds-surface-container-low);
  border-radius: var(--ds-radius-lg);
  padding: 20px;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.calendar-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.month-count {
  font-size: 11px;
  background: var(--ds-surface-container);
  color: var(--ds-on-surface-variant);
  padding: 2px 8px;
  border-radius: var(--ds-radius-full);
}

.calendar-nav {
  display: flex;
  gap: 6px;
}

.cal-nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: var(--ds-radius-sm);
  background: var(--ds-surface-container-lowest);
  color: var(--ds-on-surface-variant);
  cursor: pointer;
  box-shadow: var(--ds-shadow-sm);
  transition: background 0.2s;

  &:hover { background: var(--ds-surface-container-low); }
}

.calendar-title {
  text-align: center;
  margin-bottom: 12px;

  span {
    font-size: 14px;
    font-weight: 700;
    color: var(--ds-on-surface);
  }
}

.today-link {
  border: none;
  background: none;
  color: var(--ds-primary);
  font-size: 11px;
  font-weight: 700;
  text-decoration: underline;
  cursor: pointer;
  margin-left: 8px;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  text-align: center;
  margin-bottom: 8px;

  span {
    font-size: 10px;
    font-weight: 700;
    color: var(--ds-outline);
  }
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.calendar-day {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--ds-radius);
  background: var(--ds-surface-container-lowest);
  color: var(--ds-on-surface);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;

  &:hover:not(.other) { background: var(--ds-surface-container-high); }

  &.other {
    color: var(--ds-outline-variant);
    background: transparent;
    cursor: default;
  }

  &.selected {
    background: var(--ds-primary);
    color: var(--ds-on-primary);
    font-weight: 700;
    box-shadow: 0 0 0 3px rgba(0, 49, 120, 0.15), 0 0 0 6px rgba(0, 49, 120, 0.06);
  }

  &.today:not(.selected) {
    border: 2px solid var(--ds-warning);
  }

  &.has-data .day-count {
    display: flex;
  }
}

.day-count {
  display: none;
  position: absolute;
  bottom: 2px;
  font-size: 7px;
  color: var(--ds-primary);
  font-weight: 700;
}

.calendar-legend {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  font-size: 10px;
  color: var(--ds-on-surface-variant);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &.today { background: var(--ds-warning); }
}

.legend-box {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  background: rgba(0, 49, 120, 0.08);
  border: 1px solid var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 7px;
  color: var(--ds-primary);
  font-weight: 700;
  transform: scale(0.8);
}

.legend-hint {
  flex: 1;
  text-align: right;
  color: var(--ds-outline);
}

/* Tip card */
.tip-card {
  background: var(--ds-surface-container-low);
  border-radius: var(--ds-radius-lg);
  padding: 16px 20px;
  border-left: 4px solid var(--ds-primary);
}

.tip-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;

  span {
    font-size: 13px;
    font-weight: 700;
    color: var(--ds-on-surface);
  }
}

.tip-text {
  font-size: 12px;
  color: var(--ds-on-surface-variant);
  line-height: 1.6;
  margin: 0;
}

/* Year picker card */
.year-picker-card {
  background: var(--ds-surface-container-low);
  border-radius: var(--ds-radius-lg);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.year-picker-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-on-surface);
}

.year-picker-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ds-on-surface-variant);
  padding-top: 4px;
}

/* Right file column */
.file-col {
  display: flex;
  flex-direction: column;
  min-height: 0;

  &.full-width {
    grid-column: 1 / -1;
  }
}

.file-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 0 4px;
}

.file-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.file-date {
  font-size: 17px;
  font-weight: 700;
  color: var(--ds-on-surface);
}

.file-count {
  font-size: 11px;
  background: var(--ds-surface-container-low);
  color: var(--ds-outline);
  padding: 2px 10px;
  border-radius: var(--ds-radius-full);
}

.hidden-upload {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}

/* Empty state */
.empty-state {
  flex: 1;
  background: var(--ds-surface-container-low);
  border-radius: var(--ds-radius-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
  min-height: 400px;
}

.empty-icon-wrap {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: var(--ds-surface-container-lowest);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  box-shadow: var(--ds-shadow-sm);
}

.empty-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--ds-on-surface);
  margin: 0 0 8px 0;
}

.empty-desc {
  font-size: 13px;
  color: var(--ds-on-surface-variant);
  margin: 0 0 28px 0;
  max-width: 320px;
  line-height: 1.6;
}

.empty-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ds-space-4);
  width: 100%;
  max-width: 280px;
}

.btn-upload-main {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--ds-space-2);
  width: 100%;
  padding: 14px 0;
  border: none;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary);
  color: var(--ds-on-primary);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: var(--ds-shadow-md);
  transition: all 0.2s;

  &:hover {
    box-shadow: var(--ds-shadow-lg);
    transform: translateY(-1px);
  }
}

.btn-batch-link {
  border: none;
  background: none;
  color: var(--ds-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;

  &:hover { text-decoration: underline; }
}

.drag-hint {
  display: flex;
  align-items: center;
  gap: var(--ds-space-3);
  margin-top: var(--ds-space-8);
  color: var(--ds-outline);

  span {
    font-size: 10px;
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 1px;
    white-space: nowrap;
  }
}

.drag-line {
  width: 48px;
  height: 1px;
  background: var(--ds-surface-container);
}

/* File list */
.file-list {
  flex: 1;
  min-height: 100px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: var(--ds-space-3);
  padding: var(--ds-space-3) var(--ds-space-4);
  border-radius: var(--ds-radius-lg);
  margin-bottom: var(--ds-space-2);
  transition: all 0.2s;
  background: var(--ds-surface-container-lowest);
  box-shadow: var(--ds-shadow-sm);

  &:hover {
    box-shadow: var(--ds-shadow-md);
  }
}

.file-icon { flex-shrink: 0; }
.file-icon.clickable, .file-name.clickable { cursor: pointer; }
.file-name.clickable:hover { color: var(--ds-primary); text-decoration: underline; }

/* Drawer styles */
.drawer-content { padding: 0 4px; }
.drawer-section {
  margin-bottom: 20px;
  h4 { font-size: 13px; font-weight: 600; color: var(--ds-on-surface); margin: 0 0 8px 0; }
}
.drawer-img { width: 100%; border-radius: 8px; border: 1px solid #e5e7eb; }
.drawer-pdf { width: 100%; height: 400px; border: none; border-radius: 8px; }
.drawer-nopreview { color: #9ca3af; font-size: 13px; padding: 20px; text-align: center; }
.drawer-status-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; font-size: 13px; }
.file-info { flex: 1; min-width: 0; }
.file-name { font-size: 13px; color: var(--ds-on-surface); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-meta { font-size: 11px; color: var(--ds-outline); margin-top: 2px; }
.file-stages {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
  flex-wrap: wrap;
}
</style>
