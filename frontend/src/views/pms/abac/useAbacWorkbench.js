import { computed, reactive, toRefs } from 'vue'
import api from './api.js'
import { completionOptions, sampleContext } from './model.js'

export function useAbacWorkbench() {
  const state = reactive({
    permissions: [],
    policies: [],
    permission: '',
    preAuth: true,
    id: null,
    condition: '',
    schema: null,
    method: '',
    context: '{}',
    result: null,
    loading: false,
    error: '',
  })
  const permissionOptions = computed(() => state.permissions.map(item => ({ label: `${item.name} · ${item.code}`, value: item.code })))
  const methodOptions = computed(() => state.schema?.methods.map(item => ({ label: item.method, value: item.method })) || [])
  const postSupported = computed(() => state.schema?.methods.length,
  )
  const suggestions = computed(() => completionOptions(state.schema, state.preAuth, state.method))

  async function execute(action) {
    state.loading = true
    state.error = ''
    try {
      await action()
    }
    catch (error) {
      state.error = error.detail || error.message || '请求失败'
    }
    finally {
      state.loading = false
    }
  }

  function resetSample() {
    if (state.schema)
      state.context = JSON.stringify(sampleContext(state.schema, state.preAuth, state.method), null, 2)
    state.result = null
  }

  async function metadata() {
    const { data } = await api.metadata({ permission: state.permission })
    state.schema = data
    state.method = data.methods[0]?.method || ''
    resetSample()
  }

  async function selectPermission(permission) {
    await execute(async () => {
      state.permission = permission
      const { data } = await api.policies(permission)
      state.policies = data
      const first = data[0]
      state.id = first?.id || null
      state.preAuth = first?.preAuth ?? true
      state.condition = first?.condition || ''
      await metadata()
    })
  }

  async function init() {
    await execute(async () => {
      state.permissions = (await api.permissions()).data
    })
    if (state.permissions.length)
      await selectPermission(state.permissions.find(item => item.code === 'user:update')?.code || state.permissions[0].code)
  }

  function changePhase(preAuth) {
    state.preAuth = preAuth
    state.result = null
    resetSample()
  }

  async function edit(policy) {
    state.id = policy.id
    state.condition = policy.condition
    state.preAuth = policy.preAuth
    await execute(metadata)
  }

  function create() {
    state.id = null
    state.condition = ''
    state.result = null
  }

  function command() {
    return { permission: state.permission, preAuth: state.preAuth, condition: state.condition }
  }

  async function simulate() {
    await execute(async () => {
      const context = JSON.parse(state.context)
      state.result = (await api.simulate({ ...command(), method: state.method, context })).data
    })
  }

  async function save() {
    await execute(async () => {
      const { data } = state.id ? await api.update(state.id, command()) : await api.create(command())
      state.id = data.id
      state.policies = (await api.policies(state.permission)).data
      $message.success('策略已保存，缓存刷新通知已提交')
    })
  }

  async function remove(policy) {
    await execute(async () => {
      await api.delete(policy.id)
      state.policies = (await api.policies(state.permission)).data
      if (state.id === policy.id)
        create()
      $message.success('策略已删除')
    })
  }

  async function refresh() {
    await execute(async () => {
      await api.refresh(state.permission)
      $message.success('当前实例缓存已刷新')
    })
  }

  return { ...toRefs(state), permissionOptions, methodOptions, postSupported, suggestions, init, selectPermission, changePhase, edit, create, simulate, save, remove, refresh, resetSample }
}
