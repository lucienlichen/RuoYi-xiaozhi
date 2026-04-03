<template>
  <div class="robot-app">
    <!-- 数据服务AI助手 -->
    <div v-if="currentService === 'data_service_ai'" class="service-panel">
      <EquipDataView />
    </div>

    <!-- 问题处理AI助手 -->
    <div v-else-if="currentService === 'typical_issue_ai'" class="service-panel">
      <HazardSourceView />
    </div>

    <!-- 隐患排查AI助手 -->
    <div v-else-if="currentService === 'hazard_check'" class="service-panel">
      <InspectionView />
    </div>

    <!-- 风险服务AI助手 -->
    <div v-else-if="currentService === 'risk_service_ai'" class="service-panel">
      <PlaceholderView title="风险服务AI助手" desc="对厂区内所有设备进行15项指标动态风险评分，汇总风险分布。" color="#F97316" status="即将上线" />
    </div>

    <!-- 前沿知识AI助手 -->
    <div v-else-if="currentService === 'safety_maintenance_ai'" class="service-panel">
      <KnowledgeView />
    </div>

    <!-- 法规标准AI助手 -->
    <div v-else-if="currentService === 'regulations_ai'" class="service-panel">
      <RegulationsView />
    </div>

    <!-- 设备区域管理 -->
    <div v-else-if="currentService === 'partition'" class="service-panel">
      <PartitionView />
    </div>

    <!-- 设备管理 -->
    <div v-else-if="currentService === 'equipment'" class="service-panel">
      <EquipmentView />
    </div>

    <!-- 默认 -->
    <div v-else class="service-panel">
      <PlaceholderView title="请选择一个服务" desc="点击底部的按钮或使用语音命令选择服务。" color="#909399" />
    </div>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent } from 'vue'
import { useRoute } from 'vue-router'
import PlaceholderView from './components/PlaceholderView.vue'

// 按需加载业务视图
const EquipDataView = defineAsyncComponent(() => import('@/views/intellect/equipdata/index.vue'))
const PartitionView = defineAsyncComponent(() => import('@/views/intellect/partition/index.vue'))
const EquipmentView = defineAsyncComponent(() => import('@/views/intellect/equipment/index.vue'))
const KnowledgeView = defineAsyncComponent(() => import('@/views/intellect/knowledge/index.vue'))
const RegulationsView = defineAsyncComponent(() => import('@/views/intellect/regulations/index.vue'))
const HazardSourceView = defineAsyncComponent(() => import('@/views/intellect/hazard-source/index.vue'))
const InspectionView = defineAsyncComponent(() => import('@/views/intellect/inspection/index.vue'))

const route = useRoute()
const currentService = computed(() => route.query.service || '')
</script>

<style lang="scss" scoped>
.robot-app {
  min-height: 100%;
}

.service-panel {
  height: 100%;
}
</style>
