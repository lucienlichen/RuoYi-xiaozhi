<template>
  <div class="dashboard">
    <h2 class="dashboard-title">
      <span class="title-text">CLDA 运营概览</span>
      <span class="title-date">{{ currentDate }}</span>
    </h2>

    <div class="dashboard-grid">
      <!-- Card 1: Equipment Overview -->
      <div class="dash-card">
        <div class="card-header">
          <el-icon :size="20" class="card-icon equip"><Monitor /></el-icon>
          <span class="card-label">设备统计概览</span>
        </div>
        <div class="card-body">
          <div class="big-number">{{ mock.equipment.total }}</div>
          <div class="big-label">设备总数</div>
          <div class="status-row">
            <div class="status-item" v-for="s in mock.equipment.statuses" :key="s.label">
              <span class="status-dot" :style="{ background: s.color }"></span>
              <span class="status-count">{{ s.count }}</span>
              <span class="status-label">{{ s.label }}</span>
            </div>
          </div>
          <div class="bar-track">
            <div
              v-for="s in mock.equipment.statuses" :key="'bar-'+s.label"
              class="bar-seg"
              :style="{ width: (s.count / mock.equipment.total * 100) + '%', background: s.color }"
            ></div>
          </div>
        </div>
      </div>

      <!-- Card 2: Data Processing -->
      <div class="dash-card">
        <div class="card-header">
          <el-icon :size="20" class="card-icon data"><DataAnalysis /></el-icon>
          <span class="card-label">数据处理统计</span>
        </div>
        <div class="card-body">
          <div class="metric-row">
            <div class="metric" v-for="m in mock.data.metrics" :key="m.label">
              <div class="metric-value">{{ m.value }}</div>
              <div class="metric-label">{{ m.label }}</div>
            </div>
          </div>
          <div class="trend-label">近7日上传趋势</div>
          <div class="trend-bars">
            <div v-for="(v, i) in mock.data.trend" :key="i" class="trend-col">
              <div class="trend-bar" :style="{ height: (v / mock.data.trendMax * 100) + '%' }"></div>
              <span class="trend-day">{{ mock.data.days[i] }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Card 3: AI Assistant Usage -->
      <div class="dash-card">
        <div class="card-header">
          <el-icon :size="20" class="card-icon ai"><Cpu /></el-icon>
          <span class="card-label">AI助手使用情况</span>
        </div>
        <div class="card-body">
          <div class="ai-row" v-for="a in mock.ai" :key="a.name">
            <span class="ai-name">{{ a.name }}</span>
            <div class="ai-bar-track">
              <div class="ai-bar" :style="{ width: (a.count / mock.aiMax * 100) + '%', background: a.color }"></div>
            </div>
            <span class="ai-count">{{ a.count }}</span>
          </div>
        </div>
      </div>

      <!-- Card 4: System Status -->
      <div class="dash-card">
        <div class="card-header">
          <el-icon :size="20" class="card-icon sys"><Odometer /></el-icon>
          <span class="card-label">系统运行状态</span>
        </div>
        <div class="card-body">
          <div class="sys-grid">
            <div class="sys-item" v-for="s in mock.system" :key="s.label">
              <span class="sys-dot" :class="s.status"></span>
              <div class="sys-info">
                <div class="sys-value">{{ s.value }}</div>
                <div class="sys-label">{{ s.label }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup name="AdminDashboard">
import { computed } from 'vue'
import { Monitor, DataAnalysis, Cpu, Odometer } from '@element-plus/icons-vue'

const currentDate = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
})

const mock = {
  equipment: {
    total: 128,
    statuses: [
      { label: '正常', count: 96, color: 'var(--ds-success)' },
      { label: '警告', count: 18, color: 'var(--ds-warning)' },
      { label: '故障', count: 8, color: 'var(--ds-error)' },
      { label: '停用', count: 6, color: 'var(--ds-outline)' },
    ]
  },
  data: {
    metrics: [
      { label: '今日上传', value: '47' },
      { label: 'OCR成功率', value: '94.2%' },
      { label: 'AI结构化率', value: '87.5%' },
    ],
    trend: [32, 28, 41, 35, 52, 47, 39],
    trendMax: 52,
    days: ['一', '二', '三', '四', '五', '六', '日'],
  },
  ai: [
    { name: '数据服务', count: 342, color: 'var(--ds-emerald)' },
    { name: '问题处理', count: 156, color: 'var(--ds-indigo)' },
    { name: '隐患排查', count: 89, color: 'var(--ds-amber)' },
    { name: '风险服务', count: 67, color: 'var(--ds-orange)' },
    { name: '前沿知识', count: 234, color: 'var(--ds-rose)' },
    { name: '法规标准', count: 278, color: 'var(--ds-slate)' },
  ],
  aiMax: 342,
  system: [
    { label: '在线用户', value: '12', status: 'ok' },
    { label: 'CPU使用率', value: '34%', status: 'ok' },
    { label: '内存使用率', value: '62%', status: 'ok' },
    { label: 'Redis键数', value: '1.2k', status: 'ok' },
  ]
}
</script>

<style lang="scss" scoped>
.dashboard {
  padding: var(--ds-space-6);
  background: var(--ds-surface);
  min-height: 100%;
}

.dashboard-title {
  display: flex;
  align-items: baseline;
  gap: var(--ds-space-3);
  margin: 0 0 var(--ds-space-6) 0;
}

.title-text {
  font-size: 22px;
  font-weight: 700;
  color: var(--ds-on-surface);
  font-family: var(--ds-font-display);
}

.title-date {
  font-size: 13px;
  color: var(--ds-outline);
  font-weight: 400;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--ds-space-5);
}

@media (max-width: 900px) {
  .dashboard-grid { grid-template-columns: 1fr; }
}

/* ===== Card ===== */
.dash-card {
  background: var(--ds-surface-container-lowest);
  border-radius: var(--ds-radius-lg);
  box-shadow: var(--ds-shadow-sm);
  padding: var(--ds-space-5);
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: center;
  gap: var(--ds-space-2);
  margin-bottom: var(--ds-space-5);
}

.card-icon {
  padding: 6px;
  border-radius: var(--ds-radius);
  color: var(--ds-on-primary);
  &.equip { background: var(--ds-primary); }
  &.data { background: var(--ds-emerald); }
  &.ai { background: var(--ds-indigo); }
  &.sys { background: var(--ds-slate); }
}

.card-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--ds-on-surface);
  font-family: var(--ds-font-display);
}

/* ===== Equipment Card ===== */
.big-number {
  font-size: 40px;
  font-weight: 800;
  color: var(--ds-on-surface);
  font-family: var(--ds-font-display);
  line-height: 1;
}

.big-label {
  font-size: 13px;
  color: var(--ds-outline);
  margin-bottom: var(--ds-space-4);
}

.status-row {
  display: flex;
  gap: var(--ds-space-5);
  margin-bottom: var(--ds-space-3);
}

.status-item {
  display: flex;
  align-items: center;
  gap: var(--ds-space-1);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-count {
  font-size: 15px;
  font-weight: 700;
  color: var(--ds-on-surface);
}

.status-label {
  font-size: 12px;
  color: var(--ds-on-surface-variant);
}

.bar-track {
  display: flex;
  height: 8px;
  border-radius: var(--ds-radius-full);
  overflow: hidden;
  background: var(--ds-surface-container-low);
}

.bar-seg {
  height: 100%;
  transition: width 0.3s;
}

/* ===== Data Processing Card ===== */
.metric-row {
  display: flex;
  gap: var(--ds-space-5);
  margin-bottom: var(--ds-space-5);
}

.metric {
  flex: 1;
  text-align: center;
  padding: var(--ds-space-3);
  background: var(--ds-surface-container-low);
  border-radius: var(--ds-radius);
}

.metric-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--ds-on-surface);
  font-family: var(--ds-font-display);
}

.metric-label {
  font-size: 12px;
  color: var(--ds-on-surface-variant);
  margin-top: 2px;
}

.trend-label {
  font-size: 12px;
  color: var(--ds-outline);
  margin-bottom: var(--ds-space-2);
}

.trend-bars {
  display: flex;
  align-items: flex-end;
  gap: var(--ds-space-2);
  height: 80px;
}

.trend-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
}

.trend-bar {
  width: 100%;
  background: var(--ds-primary);
  border-radius: var(--ds-radius-sm) var(--ds-radius-sm) 0 0;
  min-height: 4px;
  transition: height 0.3s;
}

.trend-day {
  font-size: 11px;
  color: var(--ds-outline);
  margin-top: 4px;
}

/* ===== AI Assistant Card ===== */
.ai-row {
  display: flex;
  align-items: center;
  gap: var(--ds-space-3);
  margin-bottom: var(--ds-space-3);
  &:last-child { margin-bottom: 0; }
}

.ai-name {
  width: 56px;
  font-size: 13px;
  color: var(--ds-on-surface-variant);
  flex-shrink: 0;
  text-align: right;
}

.ai-bar-track {
  flex: 1;
  height: 18px;
  background: var(--ds-surface-container-low);
  border-radius: var(--ds-radius-full);
  overflow: hidden;
}

.ai-bar {
  height: 100%;
  border-radius: var(--ds-radius-full);
  transition: width 0.3s;
}

.ai-count {
  width: 36px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ds-on-surface);
  text-align: right;
}

/* ===== System Status Card ===== */
.sys-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--ds-space-4);
}

.sys-item {
  display: flex;
  align-items: center;
  gap: var(--ds-space-3);
  padding: var(--ds-space-3) var(--ds-space-4);
  background: var(--ds-surface-container-low);
  border-radius: var(--ds-radius);
}

.sys-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  &.ok { background: var(--ds-success); }
  &.warn { background: var(--ds-warning); }
  &.error { background: var(--ds-error); }
}

.sys-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--ds-on-surface);
  font-family: var(--ds-font-display);
}

.sys-label {
  font-size: 12px;
  color: var(--ds-on-surface-variant);
}
</style>
