<template>
  <div class="robot-menu ds-dark">
    <!-- Header -->
    <div class="robot-menu__header">
      <div class="welcome-area">
        <h1 class="welcome-text">{{ welcomeText }}</h1>
        <div class="header-actions">
          <el-button circle :icon="SwitchButton" @click="handleLogout" title="退出登录" />
        </div>
      </div>
    </div>

    <!-- Voice Chat Panel (floating) -->
    <VoiceChatPanel :username="userStore.name" :auto-connect="true" />

    <!-- Data Import card (full-width) -->
    <div
      class="ai-card ai-card--wide"
      :style="{ '--card-color': '#3B82F6', '--card-color-light': 'rgba(59, 130, 246, 0.08)' }"
      @click="navigateToDataImport"
    >
      <div class="ai-card__icon">
        <el-icon :size="36"><Upload /></el-icon>
      </div>
      <div class="ai-card__content">
        <h3>数据导入</h3>
        <p>设备数据批量导入与管理</p>
      </div>
      <div class="ai-card__arrow">
        <el-icon><ArrowRight /></el-icon>
      </div>
    </div>

    <!-- AI Assistant Cards -->
    <div class="robot-menu__grid">
      <div
        v-for="assistant in assistants"
        :key="assistant.service"
        class="ai-card"
        :style="{ '--card-color': assistant.color, '--card-color-light': assistant.colorLight }"
        @click="navigateTo(assistant.service)"
      >
        <div class="ai-card__icon">
          <el-icon :size="36">
            <component :is="assistant.icon" />
          </el-icon>
        </div>
        <div class="ai-card__content">
          <h3>{{ assistant.name }}</h3>
          <p>{{ assistant.description }}</p>
        </div>
        <div class="ai-card__arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowRight, SwitchButton, Upload } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'
import useUserStore from '@/store/modules/user'
import VoiceChatPanel from '@/components/VoiceChatPanel/index.vue'
import { useVoiceChat } from '@/composables/useVoiceChat'
import { aiAssistants } from '@/config/aiAssistants'

const router = useRouter()
const userStore = useUserStore()

// Welcome text
const welcomeText = computed(() => {
  const name = userStore.nickName || userStore.name || '用户'
  const hour = new Date().getHours()
  let greeting = '你好'
  if (hour < 6) greeting = '夜深了'
  else if (hour < 12) greeting = '早上好'
  else if (hour < 14) greeting = '中午好'
  else if (hour < 18) greeting = '下午好'
  else greeting = '晚上好'
  return `${greeting}，${name}！请选择您需要的AI助手服务。`
})

// 6 AI Assistants (shared config)
const assistants = aiAssistants

onMounted(() => {
  if (!getToken()) {
    router.replace('/robot/login')
    return
  }
  // Ensure user info is loaded
  if (!userStore.name) {
    userStore.getInfo()
  }
})

function navigateTo(service) {
  router.push({ path: '/robot/app', query: { service } })
}

function navigateToDataImport() {
  router.push('/business')
}

const { disconnect: disconnectVoice } = useVoiceChat()

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
    disconnectVoice()
    await userStore.logOut()
    router.replace('/robot/login')
  } catch {
    // cancelled
  }
}
</script>

<style lang="scss" scoped>
.robot-menu {
  min-height: 100vh;
  background: var(--ds-surface);
  color: var(--ds-on-surface);
  padding: var(--ds-space-6);
  display: flex;
  flex-direction: column;
}

.robot-menu__header {
  padding: 16px 0 24px;
}

.welcome-area {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.welcome-text {
  font-size: 20px;
  font-weight: 600;
  color: var(--ds-on-surface);
  flex: 1;
  line-height: 1.5;
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;

  .el-button {
    background: var(--ds-surface-container);
    border-color: var(--ds-outline-variant);
    color: var(--ds-on-surface);

    &:hover {
      background: var(--ds-surface-container-high);
      border-color: var(--ds-outline-variant);
    }
  }
}

.robot-menu__grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--ds-space-4);
  align-content: start;
}

.ai-card {
  background: var(--card-color-light);
  border: 1px solid var(--ds-outline-variant);
  border-radius: var(--ds-radius-lg);
  padding: 24px 20px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: var(--card-color);
    opacity: 0.6;
    border-radius: 16px 16px 0 0;
  }

  &:hover {
    transform: translateY(-2px);
    border-color: var(--card-color);
    box-shadow: var(--ds-shadow-lg);
  }

  &:active {
    transform: translateY(0);
  }
}

.ai-card__icon {
  width: 56px;
  height: 56px;
  border-radius: var(--ds-radius-lg);
  background: var(--card-color-light);
  border: 1px solid var(--card-color);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--card-color);
}

.ai-card__content {
  flex: 1;

  h3 {
    font-size: 16px;
    font-weight: 600;
    color: var(--ds-on-surface);
    margin-bottom: 6px;
  }

  p {
    font-size: 13px;
    color: var(--ds-on-surface-variant);
    line-height: 1.5;
  }
}

.ai-card__arrow {
  position: absolute;
  top: 24px;
  right: 20px;
  color: var(--ds-outline);
  transition: color 0.3s;

  .ai-card:hover & {
    color: var(--card-color);
  }
}

// Data import card — compact horizontal layout
.ai-card.ai-card--wide {
  flex-direction: row;
  align-items: center;
  margin-bottom: 16px;
  padding: 14px 20px;
  gap: 12px;

  .ai-card__icon {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    flex-shrink: 0;
  }

  .ai-card__content {
    h3 { font-size: 15px; margin-bottom: 2px; }
    p { font-size: 12px; }
  }

  .ai-card__arrow {
    position: static;
    flex-shrink: 0;
  }

  &::before { display: none; }
}

// Portrait screen optimization (8-inch vertical: ~800x1280)
@media (max-width: 820px) and (orientation: portrait) {
  .robot-menu {
    padding: 16px;
  }

  .robot-menu__grid {
    grid-template-columns: 1fr;
    gap: var(--ds-space-3);
  }

  .ai-card {
    flex-direction: row;
    align-items: center;
    padding: 20px 16px;
    gap: 16px;
    min-height: 80px;

    &:active {
      transform: scale(0.97);
    }
  }

  .ai-card__icon {
    width: 52px;
    height: 52px;
    flex-shrink: 0;
  }

  .ai-card__content {
    h3 { font-size: 17px; }
    p { font-size: 14px; }
  }

  .ai-card__arrow {
    position: static;
    flex-shrink: 0;
  }

  .welcome-text {
    font-size: 20px;
  }

  .welcome-area {
    flex-direction: column;
    align-items: flex-start;
  }

  .ai-card.ai-card--wide {
    min-height: 64px;
    padding: 16px;

    .ai-card__icon {
      width: 44px;
      height: 44px;
    }

    .ai-card__content {
      h3 { font-size: 16px; }
      p { font-size: 13px; }
    }
  }
}

// Landscape / Desktop
@media (min-width: 1024px) {
  .robot-menu {
    padding: 32px 48px;
  }

  .robot-menu__grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .welcome-text {
    font-size: 24px;
  }
}
</style>
