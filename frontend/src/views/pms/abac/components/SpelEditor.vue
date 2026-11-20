<template>
  <div aria-label="ABAC 表达式编辑器" @keydown.capture="shortcut">
    <NSpace class="mb-12" align="center">
      <NRadioGroup :value="mode" size="small" @update:value="setMode">
        <NRadioButton value="code">
          代码编辑
        </NRadioButton>
        <NRadioButton value="visual">
          可视化规则树
        </NRadioButton>
      </NRadioGroup>
      <NText depth="3">
        字段来自后端定义，运行与保存均由服务端校验。
      </NText>
    </NSpace>
    <NAlert v-if="conversionError" class="mb-12" type="warning" closable @close="conversionError = ''">
      {{ conversionError }}
    </NAlert>
    <QuerySpelEditor
      v-if="mode === 'code'" class="abac-query-builder" :model-value="modelValue" :authentication="bindings.authentication"
      :principal="bindings.principal" :locals="bindings.locals" :globals="bindings.globals"
      :methods="methods" :height="260" :theme="theme" size="small" locale="zh"
      @update:model-value="emit('update:modelValue', $event)"
    />
    <RuleTree
      v-else v-model="rule" class="abac-query-builder" :authentication="bindings.authentication" :principal="bindings.principal"
      :locals="bindings.locals" :globals="bindings.globals" :methods="methods" :theme="theme"
      size="small" locale="zh" @change="changeRule"
    />
  </div>
</template>

<script setup>
import { useDark } from '@vueuse/core'
import { SpelEditor as QuerySpelEditor, RuleTree } from '@zeng-alt/vue-spel-query-builder'
import { computed, ref, watch } from 'vue'
import { parseVisualRule, ruleNodeToSpel } from '../builder.js'
import { builderContext } from '../model.js'
import '@zeng-alt/vue-spel-query-builder/style.css'

const props = defineProps({
  modelValue: { type: String, default: '' },
  schema: { type: Object, default: null },
  preAuth: { type: Boolean, default: true },
  method: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'run'])
const mode = ref('code')
const rule = ref(parseVisualRule(''))
const conversionError = ref('')
const dark = useDark()
const theme = computed(() => dark.value ? 'dark' : 'light')
const bindings = computed(() => builderContext(props.schema, props.preAuth, props.method))
const methods = computed(() => (props.schema?.functions || []).map(name => ({
  name,
  argumentCount: name.startsWith('has') ? 1 : 0,
  params: name.startsWith('has') ? [{ name: 'code', type: 'string' }] : [],
  returnType: 'boolean',
  description: '服务端授权函数',
})))

function setMode(value) {
  conversionError.value = ''
  if (value === 'visual') {
    try {
      rule.value = parseVisualRule(props.modelValue)
    }
    catch (error) {
      conversionError.value = error.message
      return
    }
  }
  mode.value = value
}

function changeRule(value) {
  emit('update:modelValue', ruleNodeToSpel(value))
}

function shortcut(event) {
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    event.preventDefault()
    emit('run')
  }
}

watch(() => props.modelValue, (value) => {
  if (mode.value === 'visual' && value !== ruleNodeToSpel(rule.value)) {
    try {
      rule.value = parseVisualRule(value)
    }
    catch (error) {
      conversionError.value = error.message
      mode.value = 'code'
    }
  }
})
</script>
