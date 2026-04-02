<template>
  <div class="app-layout">
    <!-- Top navbar -->
    <AppNavbar />

    <!-- Body: sidebar + content -->
    <div class="app-body">
      <EquipmentSidebar />

      <div class="app-main">
        <!-- Main content area (always data upload) -->
        <div class="app-main-content">
          <AppContent :current-service="currentService" @open-structured-view="handleOpenStructuredView" />
        </div>

        <!-- AI assistant slide-up panel overlay -->
        <transition name="slide-up">
          <div v-if="activeAssistant" class="ai-panel-overlay" @click.self="closeAssistant">
            <div class="ai-panel">
              <div class="ai-panel-header">
                <div class="ai-panel-title">
                  <el-icon :size="20" :style="{ color: activeAssistant.color }">
                    <component :is="activeAssistant.icon" />
                  </el-icon>
                  <span>{{ activeAssistant.name }}</span>
                </div>
                <button class="ai-panel-close" @click="closeAssistant">
                  <el-icon :size="18"><Close /></el-icon>
                </button>
              </div>
              <div class="ai-panel-body">
                <DataServicePanel
                  v-if="activeServiceId === 'data_service_ai'"
                  :equipment-id="equipmentStore.selectedEquipment?.id"
                  :initial-file="pendingStructuredFile"
                  @close="closeAssistant"
                />
                <KnowledgeView v-else-if="activeServiceId === 'safety_maintenance_ai'" />
                <RegulationsView v-else-if="activeServiceId === 'regulations_ai'" />
                <HazardSourceView v-else-if="activeServiceId === 'typical_issue_ai'" />
                <InspectionView v-else-if="activeServiceId === 'hazard_check'" />
                <PlaceholderView
                  v-else
                  :title="activeAssistant.name"
                  :desc="activeAssistant.description"
                  :color="activeAssistant.color"
                  status="即将上线"
                />
              </div>
            </div>
          </div>
        </transition>

        <!-- Bottom AI assistant bar -->
        <div class="app-ai-bar">
          <button
            v-for="item in aiAssistants"
            :key="item.service"
            class="ai-btn"
            :class="{ active: activeServiceId === item.service }"
            :style="{ '--btn-color': item.color, '--btn-bg': item.colorSurface, '--btn-active': item.colorActive }"
            @click="toggleAssistant(item.service)"
          >
            <el-icon class="ai-btn-icon" :size="22"><component :is="item.icon" /></el-icon>
            <span class="ai-btn-label">{{ item.name }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Voice chat panel (floating) -->
    <VoiceChatPanel :username="userStore.name" :auto-connect="true" :auto-listen="false" default-mode="manual" />

  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Close } from '@element-plus/icons-vue'
import AppNavbar from './components/AppNavbar.vue'
import EquipmentSidebar from './components/EquipmentSidebar.vue'
import AppContent from './components/AppContent.vue'
import VoiceChatPanel from '@/components/VoiceChatPanel/index.vue'
import PlaceholderView from '@/views/robot/components/PlaceholderView.vue'
import KnowledgeView from '@/views/intellect/knowledge/index.vue'
import RegulationsView from '@/views/intellect/regulations/index.vue'
import HazardSourceView from '@/views/intellect/hazard-source/index.vue'
import InspectionView from '@/views/intellect/inspection/index.vue'
import DataServicePanel from '@/views/intellect/components/DataServicePanel.vue'
import { aiAssistants } from '@/config/aiAssistants'
import useUserStore from '@/store/modules/user'
import useEquipmentStore from '@/store/modules/equipment'
import { useVoiceChat } from '@/composables/useVoiceChat'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const equipmentStore = useEquipmentStore()
const { setNavigateCallback } = useVoiceChat()

const currentService = computed(() => route.query.service || '')
const activeServiceId = ref(null)
const pendingStructuredFile = ref(null)

// Handle "view structured data" from equipdata page
function handleOpenStructuredView(file) {
  pendingStructuredFile.value = file
  activeServiceId.value = 'data_service_ai'
}

const activeAssistant = computed(() => {
  if (!activeServiceId.value) return null
  return aiAssistants.find(a => a.service === activeServiceId.value) || null
})

function toggleAssistant(service) {
  activeServiceId.value = activeServiceId.value === service ? null : service
}

function closeAssistant() {
  activeServiceId.value = null
}

// Watch for assistant query param to auto-open panel
watch(() => route.query.assistant, (val) => {
  if (val && aiAssistants.some(a => a.service === val)) {
    activeServiceId.value = val
  }
}, { immediate: true })

onMounted(() => {
  setNavigateCallback((service) => {
    if (service === 'menu') {
      router.replace('/robot/menu')
    } else if (service === 'data_import') {
      router.replace('/app')
    } else {
      const isAssistant = aiAssistants.some(a => a.service === service)
      if (isAssistant) {
        activeServiceId.value = service
      } else {
        router.replace({ path: '/app', query: { service } })
      }
    }
  })
})
</script>

<style lang="scss" scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--ds-surface);
  font-family: var(--ds-font-body);
}

.app-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

.app-main-content {
  flex: 1;
  overflow-y: auto;
}

// AI assistant slide-up panel overlay
.ai-panel-overlay {
  position: absolute;
  inset: 0;
  bottom: 82px;
  background: rgba(13, 28, 46, 0.25);
  z-index: 10;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.ai-panel {
  background: var(--ds-surface-container-lowest);
  border-radius: var(--ds-radius-lg) var(--ds-radius-lg) 0 0;
  box-shadow: var(--ds-shadow-xl);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ai-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--ds-space-3) var(--ds-space-5);
  background: var(--ds-surface-container-low);
  flex-shrink: 0;
}

.ai-panel-title {
  display: flex;
  align-items: center;
  gap: var(--ds-space-2);
  font-size: 16px;
  font-weight: 600;
  color: var(--ds-on-surface);
  font-family: var(--ds-font-display);
}

.ai-panel-close {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--ds-outline);
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--ds-radius);
  transition: all 0.2s;

  &:hover {
    background: var(--ds-surface-container);
    color: var(--ds-on-surface);
  }
}

.ai-panel-body {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

// Slide-up transition
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from {
  opacity: 0;
  .ai-panel { transform: translateY(100%); }
}

.slide-up-leave-to {
  opacity: 0;
  .ai-panel { transform: translateY(100%); }
}

// Bottom AI assistant bar
.app-ai-bar {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: var(--ds-space-3);
  padding: var(--ds-space-3) var(--ds-space-4);
  background: var(--ds-surface-container-lowest);
  z-index: 11;
  position: relative;
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
}

.ai-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: var(--ds-space-12);
  border: none;
  border-radius: var(--ds-radius);
  background: var(--btn-bg, var(--ds-surface-container-low));
  color: var(--btn-color, var(--ds-on-surface-variant));
  cursor: pointer;
  transition: all 0.2s;
  padding: 0 var(--ds-space-3);
  box-shadow: var(--ds-shadow-sm);

  &:hover {
    box-shadow: var(--ds-shadow-md);
    transform: translateY(-1px);
  }

  &.active {
    background: var(--btn-active, var(--btn-color));
    color: #fff;
    box-shadow: var(--ds-shadow-md);
    transform: scale(1.02);

    .ai-btn-icon { color: #fff; }
    .ai-btn-label { color: #fff; }
  }
}

.ai-btn-icon {
  flex-shrink: 0;
  color: var(--btn-color);
  transition: color 0.2s;
}

.ai-btn-label {
  font-size: 15px;
  font-weight: 700;
  color: inherit;
  line-height: 1.2;
  text-align: center;
  white-space: nowrap;
  transition: color 0.2s;
  font-family: var(--ds-font-display);
}
</style>
