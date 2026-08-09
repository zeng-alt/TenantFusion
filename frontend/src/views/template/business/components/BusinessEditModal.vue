<template>
  <MeModal ref="modalRef">
    <n-form
      ref="modalFormRef"
      label-placement="left"
      require-mark-placement="left"
      :label-width="100"
      :model="modalForm"
    >
      <n-grid :cols="24" :x-gap="24">
        <n-form-item-gi :span="12" path="parentId" label="上级业务">
          <NTreeSelect
            v-model:value="modalForm.parentId"
            :options="parentOptions"
            label-field="name"
            key-field="businessId"
            :disabled="parentDisabled"
            placeholder="根业务"
            clearable
            :default-expand-all="true"
          />
        </n-form-item-gi>
        <n-form-item-gi :span="12" path="name" :rule="required">
          <template #label>
            业务名称
          </template>
          <NInput v-model:value="modalForm.name" placeholder="请输入业务名称" />
        </n-form-item-gi>
        <n-form-item-gi :span="12" path="code" :rule="required">
          <template #label>
            业务编码
          </template>
          <NInputGroup>
            <NInput
              v-model:value="modalForm.code"
              :disabled="modalAction === 'edit'"
              placeholder="如：leave"
            />
            <NButton :disabled="modalAction === 'edit'" type="primary" ghost @click="generateCode">
              <template #icon>
                <i class="i-carbon:renew text-14" />
              </template>
              生成
            </NButton>
          </NInputGroup>
        </n-form-item-gi>
        <n-form-item-gi
          :span="12"
          path="sortOrder"
          label="排序"
          :rule="{
            type: 'number',
            message: '此为必填项',
            trigger: ['blur', 'change'],
          }"
        >
          <NInputNumber v-model:value="modalForm.sortOrder" placeholder="0" />
        </n-form-item-gi>
        <n-form-item-gi :span="24" path="formConfigId" label="关联配置表单">
          <NSelect
            v-model:value="modalForm.formConfigId"
            :options="formConfigOptions"
            label-field="name"
            value-field="formConfigId"
            placeholder="选择关联的配置表单"
            clearable
            filterable
          >
            <template #option="{ option }">
              <div class="flex items-center justify-between">
                <span>{{ option.name }}</span>
                <span class="text-12 font-mono opacity-50">{{ option.code }}</span>
              </div>
            </template>
          </NSelect>
        </n-form-item-gi>
        <n-form-item-gi :span="24" path="description" label="描述">
          <NInput v-model:value="modalForm.description" type="textarea" :rows="2" />
        </n-form-item-gi>
        <n-form-item-gi :span="24" path="remark" label="备注">
          <NInput v-model:value="modalForm.remark" type="textarea" :rows="2" />
        </n-form-item-gi>
      </n-grid>
    </n-form>
  </MeModal>
</template>

<script setup>
import { NButton, NInput, NInputGroup, NInputNumber, NSelect, NTreeSelect } from 'naive-ui'
import { MeModal } from '@/components'
import { useForm, useModal } from '@/composables'
import { randomKey } from '@/utils'
import formConfigApi from '../../form-config/api'
import api from '../api'

const props = defineProps({
  /** 业务树数据（用于上级选择） */
  tree: {
    type: Array,
    default: () => [],
  },
})
const emit = defineEmits(['refresh'])

const required = {
  required: true,
  message: '此为必填项',
  trigger: ['blur', 'change'],
}

const [modalFormRef, modalForm, validation] = useForm()
const [modalRef, okLoading] = useModal()

const modalAction = ref('')
const parentDisabled = ref(false)
const formConfigOptions = ref([])

/** 上级业务选择项：过滤掉自身及其后代，避免形成环 */
const parentOptions = computed(() => buildParentOptions(props.tree, modalForm.value.businessId))

function buildParentOptions(list, excludeId) {
  return (list || []).filter(item => item.businessId !== excludeId).map(item => ({
    businessId: item.businessId,
    name: item.name,
    children: item.children?.length ? buildParentOptions(item.children, excludeId) : undefined,
  }))
}

function generateCode() {
  modalForm.value.code = randomKey('biz')
  modalFormRef.value?.restoreValidation()
}

async function loadFormConfigOptions() {
  try {
    const { data } = await formConfigApi.options()
    formConfigOptions.value = data || []
  }
  catch (error) {
    console.error(error)
    $message.error('加载配置表单失败')
  }
}

function handleOpen(options = {}) {
  const { action, row = {}, title, ...rest } = options
  modalAction.value = action
  const defaults = {
    sortOrder: 0,
    parentId: null,
    formConfigId: null,
    description: '',
    remark: '',
  }
  modalForm.value = { ...defaults, ...row }
  parentDisabled.value = action === 'edit' && !!row.parentId
  loadFormConfigOptions()
  modalRef.value.open({ title: title ?? `${action === 'add' ? '新增' : '编辑'}业务`, ...rest, onOk: onSave })
}

async function onSave() {
  await validation()
  okLoading.value = true
  try {
    const payload = { ...modalForm.value }
    delete payload.children
    delete payload.formConfigName
    if (modalAction.value === 'add') {
      await api.create(payload)
    }
    else {
      await api.update(payload.businessId, payload)
    }
    okLoading.value = false
    $message.success('保存成功')
    emit('refresh', modalAction.value === 'add' ? null : modalForm.value)
  }
  catch (error) {
    console.error(error)
    okLoading.value = false
    return false
  }
}

defineExpose({
  handleOpen,
})
</script>
