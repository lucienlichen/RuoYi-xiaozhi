<template>
  <div class="equipment-sidebar">
    <!-- Action buttons -->
    <div class="sidebar-actions">
      <button class="action-btn-primary" @click="$emit('add-equipment')">
        <el-icon><Plus /></el-icon>
        <span>添加设备</span>
      </button>
      <div class="action-btn-grid">
        <button class="action-btn-secondary" @click="$emit('manage-partition')">
          <el-icon><Location /></el-icon>
          <span>区域管理</span>
        </button>
        <button class="action-btn-secondary" @click="$emit('batch-import')">
          <el-icon><Upload /></el-icon>
          <span>批量导入</span>
        </button>
      </div>
    </div>

    <!-- Equipment list -->
    <div class="sidebar-body">
      <template v-for="partition in equipmentStore.filteredPartitions" :key="partition.id">
        <div class="partition-group">
          <h3 class="partition-title">{{ partition.partitionName }}</h3>
          <div class="partition-equipments">
            <div
              v-for="eq in partition.equipments"
              :key="eq.id"
              class="equip-item"
              :class="{ active: equipmentStore.selectedEquipment?.id === eq.id }"
              @click="equipmentStore.selectEquipment(eq)"
            >
              <div class="equip-item-left">
                <span class="status-dot" :class="eq.status || 'NORMAL'"></span>
                <span class="equip-name">{{ eq.equipmentName }}</span>
              </div>
              <el-icon v-if="equipmentStore.selectedEquipment?.id === eq.id" class="chevron"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </template>
      <div v-if="equipmentStore.filteredPartitions.length === 0" class="sidebar-empty">
        <el-icon :size="32" color="#c0c4cc"><Box /></el-icon>
        <span>暂无设备</span>
      </div>
    </div>

    <!-- Footer -->
    <div class="sidebar-footer">
      <button class="footer-btn" @click="showHelp">
        <el-icon><QuestionFilled /></el-icon>
        <span>帮助中心</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { Plus, Location, Upload, ArrowRight, Box, QuestionFilled } from '@element-plus/icons-vue'
import useEquipmentStore from '@/store/modules/equipment'

defineEmits(['add-equipment', 'manage-partition', 'batch-import'])

const equipmentStore = useEquipmentStore()

onMounted(async () => {
  if (!equipmentStore.loaded) {
    await equipmentStore.loadEquipments()
  }
})

function showHelp() {
  // placeholder
}
</script>

<style lang="scss" scoped>
.equipment-sidebar {
  width: 256px;
  height: 100%;
  background: #f8fafc;
  border-right: 1px solid rgba(226, 232, 240, 0.6);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

/* Action buttons section */
.sidebar-actions {
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-btn-primary {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 10px 0;
  border: none;
  border-radius: 8px;
  background: #003178;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
  box-shadow: 0 1px 3px rgba(0, 49, 120, 0.2);

  &:hover { opacity: 0.9; }
}

.action-btn-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.action-btn-secondary {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);

  .el-icon { color: #1565C0; font-size: 14px; }

  &:hover {
    background: #eff6ff;
    border-color: #bfdbfe;
  }
}

/* Equipment list */
.sidebar-body {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
}

.partition-group {
  margin-bottom: 20px;
}

.partition-title {
  font-size: 10px;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  margin-bottom: 8px;
  padding: 0 8px;
}

.equip-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: rgba(148, 163, 184, 0.1);
  }

  &.active {
    background: #eff6ff;
    .equip-name { color: #1565C0; font-weight: 600; }
    .chevron { color: #1565C0; font-size: 12px; }
  }
}

.equip-item-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;

  &.NORMAL { background: #10b981; }
  &.WARNING { background: #f59e0b; }
  &.FAULT { background: #ef4444; }
  &.STOPPED { background: #94a3b8; }
}

.equip-name {
  font-size: 13px;
  color: #475569;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 0;
  color: #c0c4cc;
  font-size: 13px;
}

/* Footer */
.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(226, 232, 240, 0.6);
}

.footer-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    color: #1565C0;
    background: #eff6ff;
  }
}
</style>
