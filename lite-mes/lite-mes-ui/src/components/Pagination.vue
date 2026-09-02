<template>
  <!-- 统一分页组件：固定布局与页大小选项，v-model:page / v-model:size / :total -->
  <div class="pagination-wrap">
    <el-pagination
      :current-page="page"
      :page-size="size"
      :total="total"
      :page-sizes="PAGE_SIZE_OPTIONS"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @update:current-page="(v) => emit('update:page', v)"
      @update:page-size="(v) => emit('update:size', v)"
    />
  </div>
</template>

<script setup>
import { PAGE_SIZE_OPTIONS, DEFAULT_PAGE_SIZE } from '@/utils/constants'

defineProps({
  page: { type: Number, default: 1 },
  size: { type: Number, default: DEFAULT_PAGE_SIZE },
  total: { type: Number, default: 0 }
})

const emit = defineEmits(['update:page', 'update:size'])
</script>

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}
</style>
