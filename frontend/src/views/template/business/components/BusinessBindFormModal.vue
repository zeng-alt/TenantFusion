<template>
  <MeModal ref="modalRef">
    <n-form
      ref="modalFormRef"
      label-placement="left"
      require-mark-placement="left"
      :label-width="100"
      :model="modalForm"
    >
      <n-form-item path="name" :rule="required" label="表单名称">
        <NInput v-model:value="modalForm.name" placeholder="请输入表单名称" />
      </n-form-item>
      <n-form-item path="code" :rule="required" label="表单编码">
        <NInputGroup>
          <NInput v-model:value="modalForm.code" placeholder="如：leave-form" />
          <NButton type="primary" ghost @click="generateCode">
            <template #icon>
              <i class="i-carbon:renew text-14" />
            </template>
            生成
          </NButton>
        </NInputGroup>
      </n-form-item>
      <n-form-item path="category" label="分类">
        <NInput v-model:value="modalForm.category" placeholder="如：人事 / 财务" />
      </n-form-item>
      <n-form-item path="description" label="描述">
        <NInput v-model:value="modalForm.description" type="textarea" :rows="2" />
      </n-form-item>
      <n-form-item path="remark" label="备注">
        <NInput v-model:value="modalForm.remark" type="textarea" :rows="2" />
      </n-form-item>
    </n-form>
  </MeModal>
</template>

<script setup>
import { NButton, NInput, NInputGroup } from 'naive-ui'
import { MeModal } from '@/components'
import { useForm, useModal } from '@/composables'
import { randomKey } from '@/utils'
import api from '../api'

const emit = defineEmits(['success'])

const required = {
  required: true,
  message: '此为必填项',
  trigger: ['blur', 'change'],
}

const [modalFormRef, modalForm, validation] = useForm()
const [modalRef, okLoading] = useModal()

let businessId = null

function generateCode() {
  modalForm.value.code = randomKey('fc')
  modalFormRef.value?.restoreValidation()
}

function handleOpen(options = {}) {
  const { id, title, ...rest } = options
  businessId = id
  modalForm.value = { name: '', code: '', category: '', description: '', remark: '' }
  modalRef.value.open({
    title: title ?? '新增并关联配置表单',
    ...rest,
    onOk: onSave,
  })
}

async function onSave() {
  await validation()
  okLoading.value = true
  try {
    await api.createAndBindFormConfig(businessId, { ...modalForm.value })
    okLoading.value = false
    $message.success('创建并关联成功')
    emit('success')
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
