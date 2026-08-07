<template>
  <div class="form-settings-editor h-full flex flex-col">
    <!-- 头部 -->
    <div
      class="h-48 flex shrink-0 items-center gap-10 border-b border-light_border px-12 transition-colors dark:border-dark_border"
    >
      <span
        class="h-30 w-30 flex shrink-0 items-center justify-center rounded-8 auto-bg-highlight"
      >
        <i class="i-material-symbols:tune text-16 text-primary" />
      </span>
      <span class="text-13 font-600">表单设置</span>
    </div>

    <!-- 属性编辑区 -->
    <div class="min-h-0 flex-1">
      <NScrollbar class="h-full">
        <div class="flex flex-col gap-10 p-10">
          <!-- 基本信息 -->
          <NCard
            size="small"
            :bordered="false"
            class="border border-gray-200 rounded-8 auto-bg-highlight dark:border-dark_border"
          >
            <template #header>
              <span class="text-12 text-gray-600 font-600 dark:text-gray-300">基本信息</span>
            </template>
            <NForm
              label-placement="left"
              :label-width="80"
              size="small"
              class="compact-form"
            >
              <NFormItem label="表单名称">
                <NInput
                  v-model:value="model.name"
                  placeholder="请输入表单名称"
                />
              </NFormItem>
              <NFormItem label="表单编码">
                <NInput
                  v-model:value="model.code"
                  placeholder="全局唯一编码"
                />
              </NFormItem>
            </NForm>
          </NCard>

          <!-- 标签布局 -->
          <NCard
            size="small"
            :bordered="false"
            class="border border-gray-200 rounded-8 auto-bg-highlight dark:border-dark_border"
          >
            <template #header>
              <span class="text-12 text-gray-600 font-600 dark:text-gray-300">标签布局</span>
            </template>
            <NForm
              label-placement="left"
              :label-width="80"
              size="small"
              class="compact-form"
            >
              <NFormItem label="标签位置">
                <NRadioGroup v-model:value="model.labelPlacement">
                  <NRadioButton value="left">
                    左侧
                  </NRadioButton>
                  <NRadioButton value="top">
                    顶部
                  </NRadioButton>
                </NRadioGroup>
              </NFormItem>
              <NFormItem label="标签宽度">
                <div class="w-full flex items-center gap-10">
                  <NSlider
                    v-model:value="model.labelWidth"
                    :min="40"
                    :max="200"
                    class="flex-1!"
                  />
                  <NTag
                    size="small"
                    :bordered="false"
                    round
                    type="primary"
                    class="justify-center w-30!"
                  >
                    {{ model.labelWidth }}
                  </NTag>
                </div>
              </NFormItem>
              <NFormItem label="标签对齐">
                <NRadioGroup v-model:value="model.labelAlign">
                  <NRadioButton value="left">
                    左对齐
                  </NRadioButton>
                  <NRadioButton value="right">
                    右对齐
                  </NRadioButton>
                </NRadioGroup>
              </NFormItem>
            </NForm>
          </NCard>

          <!-- 表单尺寸 -->
          <NCard
            size="small"
            :bordered="false"
            class="border border-gray-200 rounded-8 auto-bg-highlight dark:border-dark_border"
          >
            <template #header>
              <span class="text-12 text-gray-600 font-600 dark:text-gray-300">表单尺寸</span>
            </template>
            <NForm
              label-placement="left"
              :label-width="80"
              size="small"
              class="compact-form"
            >
              <NFormItem label="控件大小">
                <NRadioGroup v-model:value="model.formSize">
                  <NRadioButton value="small">
                    小
                  </NRadioButton>
                  <NRadioButton value="medium">
                    中
                  </NRadioButton>
                  <NRadioButton value="large">
                    大
                  </NRadioButton>
                </NRadioGroup>
              </NFormItem>
            </NForm>
          </NCard>
        </div>
      </NScrollbar>
    </div>
  </div>
</template>

<script setup>
import {
  NCard,
  NForm,
  NFormItem,
  NInput,
  NRadioButton,
  NRadioGroup,
  NScrollbar,
  NSlider,
  NTag,
} from 'naive-ui'
import { computed } from 'vue'

defineOptions({ name: 'FormSettingsEditor' })

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue'])

const model = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})
</script>

<style scoped>
/* 紧凑表单：行间距收紧 */
.compact-form :deep(.n-form-item) {
  @apply mb-8;
}

.compact-form :deep(.n-form-item:last-child) {
  @apply mb-0;
}

/* NCard 小尺寸下内容间距收紧 */
:deep(.n-card--content__inner) {
  padding: 4px;
}
</style>
