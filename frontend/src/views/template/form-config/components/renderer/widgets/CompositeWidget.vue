<template>
  <div
    class="group relative min-h-0 rounded-6 transition-all duration-150"
    :class="[
      selectedField === field
        ? 'bg-primary/5 ring-2 ring-primary/40'
        : mode === 'design' ? 'hover:bg-primary/5 hover:ring-1 hover:ring-primary/30' : '',
      isInvalid ? 'bg-red-50/60 ring-2 ring-red-500/60 dark:bg-red-500/10' : '',
    ]"
    @click.stop="handleSelect"
  >
    <!-- 复合字段分组头 -->
    <div
      class="flex items-center gap-6 border-b border-light_border rounded-t-6 px-6 py-6 transition-colors dark:border-dark_border"
      :class="mode === 'design' ? 'cursor-pointer' : ''"
    >
      <i class="text-15 text-primary" :class="[FIELD_TYPE_META[field.fieldType]?.icon]" />
      <span class="min-w-0 truncate text-12 font-600">
        {{ field.fieldLabel || '未命名字段' }}
      </span>
      <span v-if="field.required" class="text-12 text-red-500">*</span>
      <NTag
        size="tiny"
        :bordered="false"
        round
        type="primary"
        class="ml-2 shrink-0"
      >
        {{ FIELD_TYPE_META[field.fieldType]?.label }}
      </NTag>
      <NTooltip
        v-if="isInvalid && mode === 'design'"
        :show-arrow="false"
        content-style="white-space: normal; word-break: break-word; max-width: 260px"
      >
        <template #trigger>
          <span class="ml-2 flex shrink-0 items-center gap-2 text-11 text-red-500">
            <i class="i-material-symbols:error-outline text-12" />
            <span class="whitespace-nowrap">{{ structureErrorMessages.length }} 项未完善</span>
          </span>
        </template>
        {{ structureErrorMessages.join('；') }}
      </NTooltip>
      <div class="flex-1" />
      <span
        v-if="mode === 'design'"
        class="text-10 text-gray-400"
      >
        {{ childCount }} 个子字段
      </span>
    </div>

    <!-- 内容区 -->
    <div class="p-6">
      <!-- 设计态：渲染一次子字段 -->
      <FormRenderer
        v-if="mode === 'design'"
        :fields="field.children"
        :mode="mode"
        :selected-field="selectedField"
        :values="values"
        :disabled="disabled"
        :label-placement="labelPlacement"
        :label-width="labelWidth"
        :label-align="labelAlign"
        :size="size"
        :structure-errors="structureErrors"
        :empty-description="`选中后从左侧字段库添加字段到${FIELD_TYPE_META[field.fieldType]?.label}内部`"
        @select="emit('select', $event)"
        @delete="emit('delete', $event)"
        @move="(child, direction) => emit('move', child, direction)"
        @update:field-value="payload => emit('update:fieldValue', payload)"
      />

      <!-- 预览态：LIST 按行渲染，OBJECT 渲染子字段 -->
      <template v-else>
        <template v-if="field.fieldType === 'LIST'">
          <div
            v-for="(row, rowIndex) in rows"
            :key="rowIndex"
            class="mb-8 border border-light_border rounded-4 border-dashed p-8 transition-colors dark:border-dark_border"
          >
            <div class="mb-6 flex items-center gap-4">
              <i class="i-material-symbols:table-rows text-13 text-gray-400" />
              <span class="text-11 text-gray-400">第 {{ rowIndex + 1 }} 行</span>
              <div class="flex-1" />
              <NButton
                size="tiny"
                quaternary
                circle
                type="error"
                @click.stop="emit('removeRow', field, rowIndex)"
              >
                <template #icon>
                  <i class="i-material-symbols:delete-outline text-12" />
                </template>
              </NButton>
            </div>
            <FormRenderer
              :fields="field.children"
              :mode="mode"
              :selected-field="selectedField"
              :values="row"
              :disabled="disabled"
              :label-placement="labelPlacement"
              :label-width="labelWidth"
              :label-align="labelAlign"
              :size="size"
              :structure-errors="structureErrors"
              @select="emit('select', $event)"
              @delete="emit('delete', $event)"
              @move="(child, direction) => emit('move', child, direction)"
              @add-row="(f) => emit('addRow', f)"
              @remove-row="(f, i) => emit('removeRow', f, i)"
              @update:field-value="payload => emit('update:fieldValue', payload)"
            />
          </div>
          <NEmpty
            v-if="!rows.length"
            size="small"
            description="暂无列表数据"
          />
          <NButton
            size="small"
            type="primary"
            dashed
            block
            @click="emit('addRow', field)"
          >
            <template #icon>
              <i class="i-material-symbols:add text-13" />
            </template>
            新增一行
          </NButton>
        </template>
        <FormRenderer
          v-else
          :fields="field.children"
          :mode="mode"
          :selected-field="selectedField"
          :values="values"
          :disabled="disabled"
          :label-placement="labelPlacement"
          :label-width="labelWidth"
          :label-align="labelAlign"
          :size="size"
          :structure-errors="structureErrors"
          :empty-description="`选中后从左侧字段库添加字段到${FIELD_TYPE_META[field.fieldType]?.label}内部`"
          @select="emit('select', $event)"
          @delete="emit('delete', $event)"
          @move="(child, direction) => emit('move', child, direction)"
          @add-row="(f) => emit('addRow', f)"
          @remove-row="(f, i) => emit('removeRow', f, i)"
          @update:field-value="payload => emit('update:fieldValue', payload)"
        />
      </template>
    </div>

    <!-- 设计态悬浮操作条 -->
    <div
      v-if="mode === 'design'"
      class="absolute right-6 top-6 z-10 flex items-center gap-2 rounded-4 bg-white/95 px-4 py-2 opacity-0 shadow-sm transition-opacity dark:bg-[#2a2a2f] group-hover:opacity-100"
    >
      <NTooltip>
        <template #trigger>
          <NButton
            size="tiny"
            quaternary
            circle
            :disabled="index === 0"
            @click.stop="emit('move', field, -1)"
          >
            <template #icon>
              <i class="i-material-symbols:arrow-upward text-12" />
            </template>
          </NButton>
        </template>
        上移
      </NTooltip>
      <NTooltip>
        <template #trigger>
          <NButton
            size="tiny"
            quaternary
            circle
            :disabled="index === total - 1"
            @click.stop="emit('move', field, 1)"
          >
            <template #icon>
              <i class="i-material-symbols:arrow-downward text-12" />
            </template>
          </NButton>
        </template>
        下移
      </NTooltip>
      <NTooltip>
        <template #trigger>
          <NButton
            size="tiny"
            quaternary
            type="error"
            circle
            @click.stop="emit('delete', field)"
          >
            <template #icon>
              <i class="i-material-symbols:delete-outline text-12" />
            </template>
          </NButton>
        </template>
        删除
      </NTooltip>
    </div>
  </div>
</template>

<script setup>
import { NButton, NEmpty, NTag, NTooltip } from 'naive-ui'
import { computed } from 'vue'
import { FIELD_TYPE_META } from '../../../constants'
import FormRenderer from '../FormRenderer.vue'
import { fieldValueKey } from '../helpers'

defineOptions({ name: 'CompositeWidget' })

const props = defineProps({
  field: { type: Object, required: true },
  mode: { type: String, default: 'design' },
  selectedField: { type: Object, default: null },
  values: { type: Object, default: null },
  disabled: { type: Boolean, default: false },
  index: { type: Number, default: 0 },
  total: { type: Number, default: 1 },
  /** 表单级标签配置 */
  labelPlacement: { type: String, default: 'left' },
  labelWidth: { type: Number, default: 90 },
  labelAlign: { type: String, default: 'right' },
  /** 表单级控件尺寸 */
  size: { type: String, default: 'medium' },
  /** 字段结构校验错误映射：field -> 错误数组 */
  structureErrors: { type: Object, default: () => new Map() },
})

const emit = defineEmits(['select', 'delete', 'move', 'addRow', 'removeRow', 'update:fieldValue'])

/** 字段结构是否校验失败（字段标识/字段标签必填、字段标识重复） */
const isInvalid = computed(() => !!props.structureErrors?.get(props.field))

/** 字段结构校验错误内容 */
const structureErrorMessages = computed(() => props.structureErrors?.get(props.field) || [])

/** 预览态 LIST 行数据 */
const rows = computed(() => {
  const value = props.values?.[fieldValueKey(props.field)]
  return Array.isArray(value) ? value : []
})

const childCount = computed(() => {
  const count = (list) => {
    let n = 0
    for (const f of list || []) {
      n += 1
      if (f.children?.length)
        n += count(f.children)
    }
    return n
  }
  return count(props.field.children)
})

function handleSelect() {
  if (props.mode === 'design')
    emit('select', props.field)
}
</script>
