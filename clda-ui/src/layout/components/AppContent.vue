<template>
  <div class="app-content">
    <!-- 默认: 数据上传 -->
    <EquipDataView v-if="!currentService" />

    <PartitionView v-else-if="currentService === 'partition'" />
    <EquipmentView v-else-if="currentService === 'equipment'" />
    <KnowledgeView v-else-if="currentService === 'safety_maintenance_ai'" />
    <RegulationsView v-else-if="currentService === 'regulations_ai'" />
  </div>
</template>

<script setup>
import EquipDataView from '@/views/intellect/equipdata/index.vue'
import PartitionView from '@/views/intellect/partition/index.vue'
import EquipmentView from '@/views/intellect/equipment/index.vue'
import KnowledgeView from '@/views/intellect/knowledge/index.vue'
import RegulationsView from '@/views/intellect/regulations/index.vue'

defineProps({
  currentService: { type: String, default: '' }
})
</script>

<style lang="scss" scoped>
.app-content {
  height: 100%;
  overflow-y: auto;
}
</style>
