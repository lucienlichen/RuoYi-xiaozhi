<template>
  <div class="placeholder-panel">
    <div class="placeholder-icon" :style="{ color }">
      <el-icon :size="48"><InfoFilled /></el-icon>
    </div>
    <h2>{{ title }}</h2>
    <p>{{ desc }}</p>
    <el-tag v-if="status" type="info">{{ status }}</el-tag>
  </div>
</template>

<script setup>
import { InfoFilled } from '@element-plus/icons-vue'

defineProps({
  title: { type: String, default: '' },
  desc: { type: String, default: '' },
  color: { type: String, default: 'var(--ds-outline)' },
  status: { type: String, default: '' },
})
</script>

<style lang="scss" scoped>
.placeholder-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
  padding: 40px;

  h2 {
    font-size: 22px;
    font-weight: 600;
    color: var(--ds-on-surface);
    margin: 20px 0 12px;
  }

  p {
    font-size: 15px;
    color: var(--ds-outline);
    line-height: 1.6;
    margin-bottom: 20px;
    max-width: 480px;
  }
}

@media (max-width: 820px) and (orientation: portrait) {
  .placeholder-panel {
    padding: 24px;
    h2 { font-size: 18px; }
    p { font-size: 14px; }
  }
}
</style>
