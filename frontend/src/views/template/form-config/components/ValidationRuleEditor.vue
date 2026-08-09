<template>
  <div class="validation-rule-editor w-full">
    <div
      v-for="(rule, index) in rules"
      :key="index"
      class="mb-6 flex gap-6 border border-light_border rounded-6 border-dashed px-8 py-6 transition-colors dark:border-dark_border" :class="[
        rule.type === 'pattern' ? '' : 'items-center',
      ]"
    >
      <span class="w-16 flex shrink-0 items-center justify-center text-11 text-gray-400">
        {{ index + 1 }}
      </span>
      <div class="min-w-0 flex-1">
        <div class="flex items-center gap-6">
          <NSelect
            v-model:value="rule.type"
            :options="ruleTypeOptions"
            size="small"
            placeholder="规则类型"
            class="flex-1!"
          />
          <template v-if="VALIDATION_RULE_META[rule.type]?.hasValue">
            <NInputNumber
              v-if="VALIDATION_RULE_META[rule.type].valueType === 'number'"
              v-model:value="rule.value"
              size="small"
              placeholder="数值"
              class="w-90!"
            />
            <NInput
              v-else
              v-model:value="rule.value"
              size="small"
              placeholder="值"
              class="w-150!"
            />
            <span class="text-12 text-gray-500">
              {{ VALIDATION_RULE_META[rule.type]?.unit }}
            </span>
          </template>
        </div>
        <div
          v-if="rule.type === 'pattern'"
          class="mt-6 flex items-center gap-6"
        >
          <NInput
            v-model:value="rule.message"
            size="small"
            placeholder="校验失败提示信息"
            class="flex-1!"
          />
        </div>
      </div>
      <NButton
        class="self-center!"
        size="tiny"
        type="error"
        quaternary
        circle
        @click="removeRule(index)"
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
      @click="addRule"
    >
      <template #icon>
        <i class="i-material-symbols:add text-14" />
      </template>
      添加校验规则
    </NButton>
  </div>
</template>

<script setup>
import { NButton, NInput, NInputNumber, NSelect } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { VALIDATION_RULE_META } from '../constants'

defineOptions({ name: 'ValidationRuleEditor' })

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue'])

const rules = ref(props.modelValue || [])

watch(
  () => props.modelValue,
  (val) => {
    rules.value = val || []
  },
  { deep: true },
)

watch(
  rules,
  (val) => {
    emit('update:modelValue', val)
  },
  { deep: true },
)

const ruleTypeOptions = computed(() =>
  Object.keys(VALIDATION_RULE_META).map(type => ({
    label: VALIDATION_RULE_META[type].label,
    value: type,
  })),
)

function addRule() {
  rules.value = [...rules.value, { type: 'minLength', value: 1, message: '' }]
}

function removeRule(index) {
  rules.value = rules.value.filter((_, i) => i !== index)
}
</script>
