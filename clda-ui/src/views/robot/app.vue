<template>
  <div class="robot-app">
    <!-- 数据服务AI助手 -->
    <div v-if="currentService === 'data_service_ai'" class="service-panel">
      <EquipDataView />
    </div>

    <!-- 问题处理AI助手 -->
    <div v-else-if="currentService === 'typical_issue_ai'" class="service-panel">
      <PlaceholderView title="问题处理AI助手" desc="基于已上传数据，对设备故障、维修等问题进行智能处理建议与闭环跟踪。" color="#6366F1" status="规划中" />
    </div>

    <!-- 隐患排查AI助手 -->
    <div v-else-if="currentService === 'hazard_check'" class="service-panel">
      <PlaceholderView title="隐患排查AI助手" desc="依据国家特种设备安全法规，对起重机进行64项专项隐患排查。" color="#F59E0B" status="即将上线" />
    </div>

    <!-- 风险服务AI助手 -->
    <div v-else-if="currentService === 'risk_service_ai'" class="service-panel">
      <PlaceholderView title="风险服务AI助手" desc="对厂区内所有设备进行15项指标动态风险评分，汇总风险分布。" color="#F97316" status="即将上线" />
    </div>

    <!-- 前沿知识AI助手 -->
    <div v-else-if="currentService === 'safety_maintenance_ai'" class="service-panel">
      <PlaceholderView title="前沿知识AI助手" desc="基于安全维保文档的RAG问答，支持知识检索与智能问答。" color="#EC4899" status="即将上线" />
    </div>

    <!-- 法规标准AI助手 -->
    <div v-else-if="currentService === 'regulations_ai'" class="service-panel">
      <PlaceholderView title="法规标准AI助手" desc="内置起重机相关法律法规、技术规范，提供基于法规的专业问答。" color="#64748B" status="即将上线" />
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
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import EquipDataView from '@/views/intellect/equipdata/index.vue'
import PartitionView from '@/views/intellect/partition/index.vue'
import EquipmentView from '@/views/intellect/equipment/index.vue'
import PlaceholderView from './components/PlaceholderView.vue'

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
