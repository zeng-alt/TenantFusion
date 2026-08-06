<template>
  <div class="h-full w-full">
    <NSpin v-if="loading" class="h-full flex items-center justify-center" />
    <FormDesigner
      v-else
      :template="template"
      @close="handleClose"
      @saved="handleSaved"
    />
  </div>
</template>

<script setup>
import { NSpin } from 'naive-ui'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FormDesigner from './components/FormDesigner.vue'

defineOptions({ name: 'FormDesignPage' })

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const template = ref(null)

onMounted(async () => {
  const id = route.query.id
  if (!id) {
    loading.value = false
    return
  }
  try {
    template.value = {
      formTemplateId: route.query.formTemplateId,
      versionId: route.query.id,
      code: route.query.code,
      name: route.query.name,
    }
  }
  catch (error) {
    console.error(error)
    $message.error('加载表单模板失败')
  }
  finally {
    loading.value = false
  }
})

function handleClose() {
  closeTabOrBack()
}

function handleSaved() {
  closeTabOrBack()
}

function closeTabOrBack() {
  const openedByScript = !!window.opener
  if (openedByScript) {
    window.close()
  }
  else {
    router.back()
  }
}
</script>
