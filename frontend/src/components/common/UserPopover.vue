<template>
  <NPopover
    v-bind="attrs"
    :trigger="trigger"
    :placement="placement"
    :show-arrow="showArrow"
    :width="width"
    :trigger-disabled="triggerDisabled"
  >
    <template #trigger>
      <slot />
    </template>

    <div class="min-w-160">
      <NSpin :show="loading" size="small">
        <template v-if="!loading && user">
          <div class="flex items-center gap-12">
            <NAvatar round :size="36" :src="user.avatar">
              {{ avatarText }}
            </NAvatar>
            <div class="min-w-0">
              <div class="text-14 text-gray-800 font-600 leading-none dark:text-gray-100">
                {{ user.nickName || user.username }}
              </div>
              <div class="mt-4 text-12 text-gray-400">
                @{{ user.username }}
              </div>
            </div>
          </div>
          <div class="my-10 border-b border-gray-100 dark:border-gray-800" />
          <div class="text-13 space-y-8">
            <div class="flex items-center gap-8">
              <i class="i-carbon:apartment text-14 text-gray-400" />
              <span class="shrink-0 text-gray-500">部门</span>
              <span class="ml-auto text-gray-800 dark:text-gray-200">{{ user.deptName || '—' }}</span>
            </div>
            <div class="flex items-center gap-8">
              <i class="i-carbon:phone text-14 text-gray-400" />
              <span class="shrink-0 text-gray-500">电话</span>
              <span class="ml-auto text-gray-800 dark:text-gray-200">{{ user.phoneNumber || '—' }}</span>
            </div>
          </div>
        </template>
        <div v-else-if="!loading && loadError" class="py-16 text-center text-12 text-gray-400">
          {{ loadError }}
        </div>
      </NSpin>
    </div>
  </NPopover>
</template>

<script>
import { NAvatar, NPopover, NSpin } from 'naive-ui'
import { computed, ref, useAttrs, watch } from 'vue'
import { request } from '@/utils'
import { userPopoverCache } from './userPopoverCache'
</script>

<script setup>
defineOptions({ name: 'UserPopover' })

const props = defineProps({
  /** 用户ID，与 username 二选一，优先使用 userId */
  userId: { type: [String, Number], default: null },
  /** 用户名，与 userId 二选一 */
  username: { type: String, default: '' },
  /** 触发方式：hover / click / focus / manual */
  trigger: { type: String, default: 'hover' },
  placement: { type: String, default: 'top' },
  showArrow: { type: Boolean, default: true },
  width: { type: Number, default: undefined },
  triggerDisabled: { type: Boolean, default: false },
})

const attrs = useAttrs()

const user = ref(null)
const loading = ref(false)
const loadError = ref('')

const avatarText = computed(() => (user.value?.nickName || user.value?.username || '?').slice(0, 1).toUpperCase())

const cacheKey = computed(() => (props.userId ? `id:${props.userId}` : `name:${props.username}`))

async function load() {
  if (!props.userId && !props.username)
    return

  const key = cacheKey.value
  if (userPopoverCache.has(key)) {
    user.value = userPopoverCache.get(key)
    loadError.value = ''
    return
  }

  loading.value = true
  loadError.value = ''
  try {
    const res = await request.get('/admin/v1/user/info', {
      params: { userId: props.userId, username: props.username },
    })
    const data = res?.data || null
    if (!data) {
      loadError.value = '未找到用户'
    }
    else {
      user.value = data
      userPopoverCache.set(key, data)
    }
  }
  catch (error) {
    console.error(error)
    loadError.value = error?.message || '加载失败'
  }
  finally {
    loading.value = false
  }
}

watch(() => [props.userId, props.username], load, { immediate: true })
</script>
