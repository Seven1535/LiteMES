<template>
  <!-- 状态标签：统一文案与颜色，页面禁止自行硬编码状态样式 -->
  <el-tag :type="config?.type || 'info'" size="small" :effect="effect" disable-transitions>
    {{ config?.text || status }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'
import { STATUS_MAP } from '@/utils/constants'

const props = defineProps({
  status: { type: String, required: true },
  effect: { type: String, default: 'light' }
})

const config = computed(() => STATUS_MAP[props.status])
</script>
