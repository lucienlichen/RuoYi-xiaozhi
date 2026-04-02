<template>
  <div class="equipment-sidebar">
    <!-- Header -->
    <div class="sidebar-header">
      <div class="sidebar-header-row">
        <span class="sidebar-label">设备列表</span>
        <span class="sidebar-count">{{ totalCount }}</span>
      </div>
      <!-- Search -->
      <input
        v-model="searchKeyword"
        class="sidebar-search"
        placeholder="搜索设备..."
        @input="onSearch"
      />
    </div>

    <!-- Equipment list -->
    <div class="sidebar-body">
      <template v-for="partition in equipmentStore.filteredPartitions" :key="partition.id">
        <div class="partition-group">
          <button class="partition-header" @click="togglePartition(partition.id)">
            <div class="partition-header-left">
              <el-icon class="partition-chevron" :size="14">
                <component :is="expandedPartitions.has(partition.id) ? ArrowDown : ArrowRight" />
              </el-icon>
              <span class="partition-name">{{ partition.partitionName }}</span>
            </div>
            <span class="partition-count">{{ partition.equipments?.length || 0 }}</span>
          </button>
          <div v-if="expandedPartitions.has(partition.id)" class="partition-equipments">
            <div
              v-for="eq in partition.equipments"
              :key="eq.id"
              class="equip-item"
              :class="{ active: equipmentStore.selectedEquipment?.id === eq.id }"
              @click="equipmentStore.selectEquipment(eq)"
            >
              <span class="status-dot" :class="eq.status || 'NORMAL'"></span>
              <span class="equip-name">{{ eq.equipmentName }}</span>
            </div>
          </div>
        </div>
      </template>
      <div v-if="equipmentStore.filteredPartitions.length === 0" class="sidebar-empty">
        <el-icon :size="32" color="#c0c4cc"><Box /></el-icon>
        <span>暂无设备</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowRight, ArrowDown, Box } from '@element-plus/icons-vue'
import useEquipmentStore from '@/store/modules/equipment'


const equipmentStore = useEquipmentStore()
const searchKeyword = ref('')
const expandedPartitions = ref(new Set())

const totalCount = computed(() =>
  equipmentStore.filteredPartitions.reduce((sum, p) => sum + (p.equipments?.length || 0), 0)
)

function togglePartition(id) {
  const s = new Set(expandedPartitions.value)
  if (s.has(id)) { s.delete(id) } else { s.add(id) }
  expandedPartitions.value = s
}

function onSearch() {
  equipmentStore.equipSearch = searchKeyword.value
}

onMounted(async () => {
  if (!equipmentStore.loaded) {
    await equipmentStore.loadEquipments()
  }
  // Auto-expand all partitions
  equipmentStore.filteredPartitions.forEach(p => {
    expandedPartitions.value.add(p.id)
  })
})
</script>

<style lang="scss" scoped>
.equipment-sidebar {
  width: 256px;
  height: 100%;
  background: var(--ds-surface-container-lowest);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

/* ===== Header ===== */
.sidebar-header {
  padding: var(--ds-space-3);
  background: var(--ds-surface-container-low);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sidebar-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--ds-on-surface-variant);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  font-family: var(--ds-font-display);
}

.sidebar-count {
  font-size: 12px;
  color: var(--ds-outline);
}

.sidebar-search {
  width: 100%;
  padding: var(--ds-space-2) var(--ds-space-3);
  background: var(--ds-surface-container-low);
  border: none;
  border-radius: var(--ds-radius);
  font-size: 14px;
  color: var(--ds-on-surface);
  outline: none;
  transition: all 0.2s;

  &::placeholder { color: var(--ds-outline); }

  &:focus {
    background: var(--ds-surface-container-lowest);
    border-bottom: 2px solid var(--ds-surface-tint);
  }
}

/* ===== Equipment list ===== */
.sidebar-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: #e2e8f0; border-radius: 10px; }
  &::-webkit-scrollbar-thumb:hover { background: #cbd5e1; }
}

.partition-group {
  margin-bottom: 2px;
}

/* Collapsible partition header */
.partition-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: var(--ds-surface-container-low);
  border: none;
  border-radius: var(--ds-radius);
  cursor: pointer;
  transition: background 0.2s;

  &:hover { background: var(--ds-surface-container); }
}

.partition-header-left {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.partition-chevron {
  flex-shrink: 0;
  color: var(--ds-outline);
}

.partition-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ds-on-surface);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.partition-count {
  font-size: 12px;
  color: var(--ds-outline);
  flex-shrink: 0;
}

/* Device items */
.partition-equipments {
  padding: 4px 0 4px 4px;
}

.equip-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px 6px 28px;
  border-radius: var(--ds-radius);
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 1px;

  &:hover {
    background: var(--ds-surface-container-low);
  }

  &.active {
    background: rgba(0, 49, 120, 0.08);
    .equip-name { color: var(--ds-primary); font-weight: 600; }
  }
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;

  &.NORMAL { background: #10b981; }
  &.WARNING { background: #f59e0b; }
  &.FAULT { background: #ef4444; }
  &.STOPPED { background: var(--ds-outline); }
}

.equip-name {
  font-size: 14px;
  color: var(--ds-on-surface-variant);
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
</style>
