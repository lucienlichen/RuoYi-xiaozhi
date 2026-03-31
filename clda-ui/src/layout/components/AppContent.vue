<template>
  <div class="app-content">
    <!-- 默认: 数据上传 -->
    <EquipDataView v-if="!currentService || currentService === 'partition' || currentService === 'equipment'" />

    <PartitionView v-if="currentService === 'partition'" />
    <EquipmentView v-if="currentService === 'equipment'" />
  </div>
</template>

<script setup>
import EquipDataView from '@/views/intellect/equipdata/index.vue'
import PartitionView from '@/views/intellect/partition/index.vue'
import EquipmentView from '@/views/intellect/equipment/index.vue'

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
