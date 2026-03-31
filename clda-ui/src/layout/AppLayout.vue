<template>
  <div class="app-layout">
    <!-- Top navbar -->
    <AppNavbar />

    <!-- Body: sidebar + content -->
    <div class="app-body">
      <EquipmentSidebar
        @add-equipment="showAddEquipment = true"
        @manage-partition="showPartitionMgr = true"
        @batch-import="showBatchImport = true"
      />

      <div class="app-main">
        <!-- Main content area (always data upload) -->
        <div class="app-main-content">
          <AppContent :current-service="currentService" />
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
                <PlaceholderView
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
            :style="{ '--btn-color': item.color, '--btn-bg': item.colorLight }"
            @click="toggleAssistant(item.service)"
          >
            <div class="ai-btn-icon">
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
            </div>
            <span class="ai-btn-label">{{ item.shortName }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Voice chat panel (floating) -->
    <VoiceChatPanel :username="userStore.name" :auto-connect="true" />

    <!-- Add equipment dialog -->
    <el-dialog v-model="showAddEquipment" title="添加设备" width="500px" append-to-body>
      <el-form :model="newEquipForm" label-width="80px">
        <el-form-item label="设备名称"><el-input v-model="newEquipForm.equipmentName" /></el-form-item>
        <el-form-item label="设备编号"><el-input v-model="newEquipForm.equipmentCode" /></el-form-item>
        <el-form-item label="所属分区">
          <el-select v-model="newEquipForm.partitionId" placeholder="选择分区">
            <el-option v-for="p in equipmentStore.partitionsWithEquip" :key="p.id" :label="p.partitionName" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddEquipment = false">取消</el-button>
        <el-button type="primary" @click="doAddEquipment">确定</el-button>
      </template>
    </el-dialog>

    <!-- Partition management dialog -->
    <el-dialog v-model="showPartitionMgr" title="分区管理" width="600px" append-to-body>
      <PartitionView />
    </el-dialog>

    <!-- Batch import dialog (placeholder) -->
    <el-dialog v-model="showBatchImport" title="批量导入" width="600px" append-to-body>
      <el-empty description="批量导入功能开发中" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import AppNavbar from './components/AppNavbar.vue'
import EquipmentSidebar from './components/EquipmentSidebar.vue'
import AppContent from './components/AppContent.vue'
import VoiceChatPanel from '@/components/VoiceChatPanel/index.vue'
import PartitionView from '@/views/intellect/partition/index.vue'
import PlaceholderView from '@/views/robot/components/PlaceholderView.vue'
import { aiAssistants } from '@/config/aiAssistants'
import { addEquipment } from '@/api/intellect/equipment'
import useUserStore from '@/store/modules/user'
import useEquipmentStore from '@/store/modules/equipment'
import { useVoiceChat } from '@/composables/useVoiceChat'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const equipmentStore = useEquipmentStore()
const { setNavigateCallback, setAutoListenOnConnect, setListenMode } = useVoiceChat()

const currentService = computed(() => route.query.service || '')
const activeServiceId = ref(null)
const showAddEquipment = ref(false)
const showPartitionMgr = ref(false)
const showBatchImport = ref(false)
const newEquipForm = ref({ equipmentName: '', equipmentCode: '', partitionId: null })

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

function switchService(service) {
  router.replace({ path: '/app', query: { service } })
}

async function doAddEquipment() {
  if (!newEquipForm.value.equipmentName) {
    ElMessage.warning('请输入设备名称')
    return
  }
  await addEquipment(newEquipForm.value)
  ElMessage.success('添加成功')
  showAddEquipment.value = false
  newEquipForm.value = { equipmentName: '', equipmentCode: '', partitionId: null }
  equipmentStore.loadEquipments()
}

// Watch for assistant query param to auto-open panel
watch(() => route.query.assistant, (val) => {
  if (val && aiAssistants.some(a => a.service === val)) {
    activeServiceId.value = val
  }
}, { immediate: true })

onMounted(() => {
  setAutoListenOnConnect(false)
  setListenMode('manual')
  setNavigateCallback((service) => {
    if (service === 'menu') {
      router.replace('/robot/menu')
    } else {
      // Check if it's an AI assistant service
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
  background: #fff;
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
  bottom: 76px; // above the AI bar
  background: rgba(0, 0, 0, 0.3);
  z-index: 10;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.ai-panel {
  background: #fff;
  border-radius: 16px 16px 0 0;
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.12);
  max-height: 75%;
  display: flex;
  flex-direction: column;
}

.ai-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0;
}

.ai-panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.ai-panel-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #94a3b8;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.2s;

  &:hover {
    background: #f1f5f9;
    color: #475569;
  }
}

.ai-panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 0;
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
  height: 76px;
  background: #fff;
  border-top: 1px solid #e2e8f0;
  z-index: 11;
  position: relative;
}

.ai-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: none;
  border-right: 1px solid #f1f5f9;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s;
  padding: 8px 4px;

  &:last-child { border-right: none; }

  &:hover {
    background: var(--btn-bg, #f5f5f5);

    .ai-btn-icon { transform: scale(1.1); }
    .ai-btn-label { color: var(--btn-color); }
  }

  &.active {
    background: var(--btn-bg, #f5f5f5);
    .ai-btn-icon {
      box-shadow: 0 0 0 2px var(--btn-color);
    }
    .ai-btn-label { color: var(--btn-color); font-weight: 700; }
  }
}

.ai-btn-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--btn-bg, rgba(100, 100, 100, 0.1));
  color: var(--btn-color);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.ai-btn-label {
  font-size: 10px;
  font-weight: 600;
  color: #475569;
  line-height: 1.2;
  text-align: center;
  transition: color 0.2s;
}
</style>
