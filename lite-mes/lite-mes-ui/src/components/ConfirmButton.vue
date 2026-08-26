<template>
  <!-- 确认操作按钮：删除/关闭等危险操作统一走二次确认 -->
  <el-popconfirm
    :title="title"
    :confirm-button-text="confirmText"
    :cancel-button-text="cancelText"
    @confirm="emit('confirm')"
  >
    <template #reference>
      <el-button :type="type" :size="size" :disabled="disabled" :link="link">
        <slot />
      </el-button>
    </template>
  </el-popconfirm>
</template>

<script setup>
defineProps({
  title: { type: String, default: '确定执行该操作吗？' },
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '取消' },
  type: { type: String, default: 'danger' },
  size: { type: String, default: 'small' },
  disabled: { type: Boolean, default: false },
  link: { type: Boolean, default: false }
})

const emit = defineEmits(['confirm'])
</script>
