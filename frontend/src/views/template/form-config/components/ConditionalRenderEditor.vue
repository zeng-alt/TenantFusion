<template>
  <div class="conditional-editor w-full">
    <div class="mb-6 flex items-center gap-6">
      <span class="text-12 text-gray-500">当</span>
      <NRadioGroup
        v-model:value="model.logic"
        size="small"
      >
        <NRadioButton value="and">
          全部满足
        </NRadioButton>
        <NRadioButton value="or">
          任一满足
        </NRadioButton>
      </NRadioGroup>
      <span class="text-12 text-gray-500">时显示该字段</span>
    </div>

    <div
      v-for="(cond, index) in model.conditions"
      :key="index"
      class="mb-6 flex flex-wrap items-center gap-6 border border-light_border rounded-6 border-dashed px-8 py-6 transition-colors dark:border-dark_border"
    >
      <span class="w-16 shrink-0 text-center text-11 text-gray-400">
        {{ index + 1 }}
      </span>
      <NSelect
        v-model:value="cond.fieldKey"
        :options="fieldKeyOptions"
        size="small"
        placeholder="选择字段"
        class="w-150!"
        filterable
      />
      <NSelect
        v-model:value="cond.operator"
        :options="operatorOptions"
        size="small"
        class="w-110!"
      />
      <NInput
        v-if="needsValue(cond.operator)"
        v-model:value="cond.value"
        size="small"
        placeholder="比较值"
        class="w-130!"
      />
      <NButton
        size="tiny"
        type="error"
        quaternary
        circle
        @click="removeCondition(index)"
      >
        <template #icon>
          <i class="i-material-symbols:delete-outline text-14" />
        </template>
      </NButton>
    </div>

    <NButton
      size="small"
      type="primary"
      dashed
      block
      @click="addCondition"
    >
      <template #icon>
        <i class="i-material-symbols:add text-14" />
      </template>
      添加条件
    </NButton>
  </div>
</template>

<script setup>
import { NButton, NInput, NRadioButton, NRadioGroup, NSelect } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { CONDITION_OPERATORS } from '../constants'

defineOptions({ name: 'ConditionalRenderEditor' })

const props = defineProps({
  /** 条件表达式对象 { logic, conditions } 或 null */
  modelValue: {
    type: Object,
    default: null,
  },
  /** 可被引用的其他字段列表 */
  fields: {
    type: Array,
    default: () => [],
  },
  /** 当前字段自身的 key（条件引用时排除自身） */
  currentFieldKey: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue'])

const model = ref({
  logic: 'and',
  conditions: [],
  ...(props.modelValue || {}),
})

watch(
  () => props.modelValue,
  (val) => {
    model.value = { logic: 'and', conditions: [], ...(val || {}) }
  },
  { deep: true },
)

watch(
  model,
  (val) => {
    emit('update:modelValue', val)
  },
  { deep: true },
)

const operatorOptions = computed(() =>
  Object.keys(CONDITION_OPERATORS).map(op => ({
    label: CONDITION_OPERATORS[op],
    value: op,
  })),
)

const fieldKeyOptions = computed(() => {
  const current = props.fields || []
  return current
    .filter(f => f.fieldKey && f.fieldKey !== props.currentFieldKey)
    .map(f => ({
      label: f.fieldLabel || f.fieldKey,
      value: f.fieldKey,
    }))
})

function needsValue(operator) {
  return !['empty', 'notEmpty'].includes(operator)
}

function addCondition() {
  model.value.conditions = [...model.value.conditions, {
    fieldKey: fieldKeyOptions.value[0]?.value || '',
    operator: 'eq',
    value: '',
  }]
}

function removeCondition(index) {
  model.value.conditions = model.value.conditions.filter((_, i) => i !== index)
}
</script>
