<template>
  <div class="robot-layout ds-dark">
    <!-- Top bar -->
    <header class="robot-header">
      <div class="robot-header__left">
        <el-button text @click="goBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回菜单</span>
        </el-button>
      </div>
      <h1 class="robot-header__title">{{ currentServiceName }}</h1>
      <div class="robot-header__right">
        <span class="user-name">{{ userStore.nickName || userStore.name }}</span>
      </div>
    </header>

    <!-- Main content -->
    <main class="robot-main">
      <robot-app />
    </main>

    <!-- Bottom AI assistant bar -->
    <footer class="robot-footer">
      <div class="ai-bar">
        <button
          v-for="item in aiButtons"
          :key="item.service"
          class="ai-bar__btn"
          :class="{ active: currentService === item.service }"
          :style="{ '--btn-color': item.color }"
          @click="switchService(item.service)"
          :title="item.name"
        >
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <span class="ai-bar__label">{{ item.shortName }}</span>
        </button>
      </div>
    </footer>

    <!-- Voice chat panel (floating) -->
    <VoiceChatPanel :username="userStore.name" :auto-connect="true" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, OfficeBuilding, SetUp } from '@element-plus/icons-vue'
import RobotApp from '@/views/robot/app.vue'
import VoiceChatPanel from '@/components/VoiceChatPanel/index.vue'
import { aiAssistants } from '@/config/aiAssistants'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const currentService = computed(() => route.query.service || 'data_service_ai')

const aiButtons = [
  // 业务管理
  { name: '区域管理', shortName: '区域', service: 'partition', icon: OfficeBuilding, color: '#8B5CF6' },
  { name: '设备管理', shortName: '设备', service: 'equipment', icon: SetUp, color: '#06B6D4' },
  // AI助手（共享配置）
  ...aiAssistants,
]

const currentServiceName = computed(() => {
  const found = aiButtons.find(b => b.service === currentService.value)
  return found ? found.name : '起重装备全生命周期数据AI智能体'
})

function goBack() {
  router.push('/robot/menu')
}

function switchService(service) {
  router.replace({ path: '/robot/app', query: { service } })
}
</script>

<style lang="scss" scoped>
.robot-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--ds-surface);
  overflow: hidden;
}

.robot-header {
  height: 56px;
  background: var(--ds-surface-container-low);
  box-shadow: var(--ds-shadow-sm);
  display: flex;
  align-items: center;
  padding: 0 16px;
  flex-shrink: 0;
  z-index: 10;
}

.robot-header__left { flex: 1; }

.back-btn {
  font-size: 14px;
  color: var(--ds-on-surface-variant);
  .el-icon { margin-right: 4px; }
}

.robot-header__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--ds-on-surface);
  text-align: center;
}

.robot-header__right {
  flex: 1;
  text-align: right;
}

.user-name {
  font-size: 14px;
  color: var(--ds-on-surface-variant);
}

.robot-main {
  flex: 1;
  overflow-y: auto;
  padding: var(--ds-space-4);
}

.robot-footer {
  flex-shrink: 0;
  background: var(--ds-surface-container-low);
  box-shadow: var(--ds-shadow-sm);
  padding: 8px 16px;
  display: flex;
  align-items: center;
  z-index: 10;
}

.ai-bar {
  flex: 1;
  display: flex;
  gap: 4px;
  overflow-x: auto;
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }
}

.ai-bar__btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 10px;
  border: none;
  border-radius: var(--ds-radius);
  background: transparent;
  color: var(--ds-on-surface-variant);
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  flex-shrink: 0;

  &:hover {
    background: var(--ds-surface-container);
    color: var(--btn-color);
  }

  &.active {
    background: color-mix(in srgb, var(--btn-color) 10%, transparent);
    color: var(--btn-color);
  }
}

.ai-bar__label {
  font-size: 11px;
  line-height: 1;
}

@media (max-width: 820px) and (orientation: portrait) {
  .robot-header { height: 48px; padding: 0 12px; }
  .robot-header__title { font-size: 14px; }
  .robot-main { padding: 12px; }
  .robot-footer { padding: 6px 8px; }
  .ai-bar__btn { padding: 4px 8px; }
  .ai-bar__label { font-size: 10px; }
}
</style>
