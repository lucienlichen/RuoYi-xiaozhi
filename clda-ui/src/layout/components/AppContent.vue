<template>
  <div class="app-content">
    <!-- 默认: 数据上传 -->
    <EquipDataView v-if="!currentService" @open-structured-view="$emit('open-structured-view', $event)" />

    <PartitionView v-else-if="currentService === 'partition'" />
    <EquipmentView v-else-if="currentService === 'equipment'" />
  </div>
</template>

<script setup>
import { defineAsyncComponent } from 'vue'

const EquipDataView = defineAsyncComponent(() => import('@/views/intellect/equipdata/index.vue'))
const PartitionView = defineAsyncComponent(() => import('@/views/intellect/partition/index.vue'))
const EquipmentView = defineAsyncComponent(() => import('@/views/intellect/equipment/index.vue'))

defineProps({
  currentService: { type: String, default: '' }
})

defineEmits(['open-structured-view'])
</script>

<style lang="scss" scoped>
.app-content {
  height: 100%;
  overflow-y: auto;
}
</style>
