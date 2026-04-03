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
      <div class="robot-header__center">
        <h1 class="robot-header__title">{{ currentServiceName }}</h1>
        <span v-if="equipmentName" class="robot-header__equip">{{ equipmentName }}</span>
      </div>
      <div class="robot-header__right">
        <span class="user-name">{{ userStore.nickName || userStore.name }}</span>
        <el-dropdown trigger="click" @command="handleSettingsCommand">
          <el-button text class="settings-btn">
            <el-icon :size="20"><Setting /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="partition">
                <el-icon><OfficeBuilding /></el-icon>区域管理
              </el-dropdown-item>
              <el-dropdown-item command="equipment">
                <el-icon><SetUp /></el-icon>设备管理
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- Main content -->
    <main class="robot-main">
      <robot-app />
    </main>

    <!-- Bottom AI assistant bar (3x2 grid, AI assistants only) -->
    <footer class="robot-footer">
      <div class="ai-bar">
        <button
          v-for="item in aiAssistants"
          :key="item.service"
          class="ai-bar__btn"
          :class="{ active: currentService === item.service }"
          :style="{ '--btn-color': item.color }"
          @click="switchService(item.service)"
          :title="item.name"
        >
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
          <span class="ai-bar__label">{{ item.name }}</span>
        </button>
      </div>
    </footer>

    <!-- Voice chat panel (floating) -->
    <VoiceChatPanel :username="userStore.name" :auto-connect="true" />
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, OfficeBuilding, SetUp, Setting } from '@element-plus/icons-vue'
import VoiceChatPanel from '@/components/VoiceChatPanel/index.vue'

const RobotApp = defineAsyncComponent(() => import('@/views/robot/app.vue'))
import { aiAssistants } from '@/config/aiAssistants'
import useUserStore from '@/store/modules/user'
import useEquipmentStore from '@/store/modules/equipment'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const equipmentStore = useEquipmentStore()

const currentService = computed(() => route.query.service || 'data_service_ai')

const equipmentName = computed(() => equipmentStore.selectedEquipment?.equipmentName || '')

const currentServiceName = computed(() => {
  const found = aiAssistants.find(b => b.service === currentService.value)
  return found ? found.name : '起重装备全生命周期数据AI智能体'
})

function goBack() {
  router.push('/robot/menu')
}

function switchService(service) {
  router.replace({ path: '/robot/app', query: { service } })
}

function handleSettingsCommand(command) {
  switchService(command)
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
  height: 64px;
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

.robot-header__center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.robot-header__title {
  font-size: 18px;
  font-weight: 600;
  color: var(--ds-on-surface);
  text-align: center;
  line-height: 1.3;
}

.robot-header__equip {
  font-size: 12px;
  color: var(--ds-on-surface-variant);
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.robot-header__right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.user-name {
  font-size: 14px;
  color: var(--ds-on-surface-variant);
}

.settings-btn {
  color: var(--ds-on-surface-variant);
  padding: 6px;
  &:hover { color: var(--ds-on-surface); }
}

.robot-main {
  flex: 1;
  overflow-y: auto;
  padding: var(--ds-space-4);
}

.robot-footer {
  flex-shrink: 0;
  background: var(--ds-surface-container-low);
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.15);
  padding: 10px 16px;
  z-index: 10;
}

.ai-bar {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 8px;
}

.ai-bar__btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 52px;
  padding: 8px 6px;
  border: 1px solid transparent;
  border-radius: var(--ds-radius);
  background: var(--ds-surface-container);
  color: var(--ds-on-surface-variant);
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--ds-surface-container-high);
    color: var(--btn-color);
    border-color: color-mix(in srgb, var(--btn-color) 20%, transparent);
  }

  &:active {
    transform: scale(0.97);
  }

  &.active {
    background: color-mix(in srgb, var(--btn-color) 12%, transparent);
    backdrop-filter: blur(var(--ds-glass-blur));
    border-color: color-mix(in srgb, var(--btn-color) 30%, transparent);
    color: var(--btn-color);
    box-shadow: 0 0 12px color-mix(in srgb, var(--btn-color) 15%, transparent);
  }
}

.ai-bar__label {
  font-size: 12px;
  line-height: 1.2;
  text-align: center;
}

@media (max-width: 820px) and (orientation: portrait) {
  .robot-header { height: 56px; padding: 0 12px; }
  .robot-header__title { font-size: 16px; }
  .robot-header__equip { max-width: 180px; font-size: 11px; }
  .robot-main { padding: 10px; }
  .robot-footer { padding: 8px 10px; }
  .ai-bar { gap: 6px; }
  .ai-bar__btn { min-height: 48px; padding: 6px 4px; }
  .ai-bar__label { font-size: 11px; }
  .user-name { display: none; }
}
</style>
