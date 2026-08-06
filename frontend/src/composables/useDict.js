import { computed, ref } from 'vue'
import { useDictStore } from '@/store'
import { request } from '@/utils'
import { dedupeAsync } from '@/utils/common'

const TAG_TYPE_MAP = {
  default: 'default',
  primary: 'primary',
  success: 'success',
  info: 'info',
  warning: 'warning',
  error: 'error',
}

const fetchDictByCode = dedupeAsync(async (code) => {
  const res = await request.get('/dict/data/all', { params: { dictCode: code } })
  return res?.data || []
}, code => code)

export function useDict(code) {
  const dictStore = useDictStore()
  const dictData = ref([])
  const loading = ref(false)

  async function fetchDict() {
    if (dictStore.dictMap[code]) {
      dictData.value = dictStore.dictMap[code]
      return
    }
    loading.value = true
    try {
      const data = await fetchDictByCode(code)
      dictStore.setDictData(code, data)
      dictData.value = data
    }
    finally {
      loading.value = false
    }
  }

  fetchDict()

  const options = computed(() =>
    dictData.value.map(item => ({
      label: item.dictLabel,
      value: item.dictValue,
      listClass: item.listClass,
    })),
  )

  function getLabel(value) {
    const item = dictData.value.find(d => String(d.dictValue) === String(value))
    return item?.dictLabel ?? value
  }

  function getTagType(value) {
    const item = dictData.value.find(d => String(d.dictValue) === String(value))
    return TAG_TYPE_MAP[item?.listClass] ?? 'default'
  }

  function getListClass(value) {
    const item = dictData.value.find(d => String(d.dictValue) === String(value))
    return item?.listClass ?? ''
  }

  return { dictData, loading, options, getLabel, getTagType, getListClass, fetchDict }
}
