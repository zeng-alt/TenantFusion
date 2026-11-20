<template>
  <n-modal v-model:show="visible" preset="card" title="切换租户" style="width: 400px">
    <n-select v-model:value="selected" :options="options" filterable placeholder="请选择租户" />
    <template #footer>
      <div class="flex justify-end gap-12">
        <n-button @click="visible = false">
          取消
        </n-button>
        <n-button type="primary" :loading="loading" :disabled="!selected || selected === current" @click="select">
          确认切换
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup>
import api from '@/api'
import { useTabStore, useUserStore } from '@/store'
import { getActiveTenant, setActiveTenant } from '@/utils/active-tenant'

const router = useRouter()
const user = useUserStore()
const tabs = useTabStore()
const visible = ref(false)
const loading = ref(false)
const options = ref([])
const selected = ref(null)
const current = computed(() => getActiveTenant() || user.tenant)

async function open() {
  const { data } = await api.getTenants()
  options.value = data.map(tenant => ({ label: `${tenant.name} (${tenant.id})`, value: tenant.id }))
  selected.value = current.value
  visible.value = true
}

async function select() {
  loading.value = true
  try {
    await api.validateTenant(selected.value)
    setActiveTenant(selected.value)
    tabs.resetTabs()
    await router.replace('/')
    window.location.reload()
  }
  finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
