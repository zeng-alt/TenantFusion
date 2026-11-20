<template>
  <CommonPage>
    <template #action>
      <NButton :disabled="!permission || loading" type="primary" @click="create">
        新建策略
      </NButton>
    </template>
    <NAlert v-if="error" class="mb-16" type="error" closable @close="error = ''">
      {{ error }}
    </NAlert>
    <NSpin :show="loading">
      <div class="abac-layout">
        <NCard title="权限与策略" size="small">
          <NSelect
            :value="permission" :options="permissionOptions" filterable placeholder="选择权限"
            :disabled="loading" @update:value="selectPermission"
          />
          <NEmpty v-if="!permissions.length && !loading" class="mt-20" description="当前租户没有绑定 ABAC 的方法权限" />
          <NEmpty v-else-if="!policies.length" class="mt-20" description="尚未配置规则" />
          <div v-for="policy in policies" :key="policy.id" class="policy-item mt-12" :class="{ selected: id === policy.id }">
            <NButton text class="w-full" :disabled="loading" @click="edit(policy)">
              {{ policy.preAuth ? '前置' : '后置' }} · {{ policy.id }}
            </NButton>
            <p class="policy-expression">
              {{ policy.condition }}
            </p>
            <NPopconfirm @positive-click="remove(policy)">
              <template #trigger>
                <NButton text size="tiny" type="error">
                  删除规则
                </NButton>
              </template>
              删除后将失效该权限的策略缓存，确认删除？
            </NPopconfirm>
          </div>
        </NCard>
        <div v-if="permission" class="min-w-0">
          <NCard :title="id ? '编辑策略' : '新建策略'" size="small">
            <template #header-extra>
              <NSpace>
                <NButton :disabled="loading" @click="refresh">
                  刷新缓存
                </NButton>
                <NButton :disabled="loading || !condition.trim()" type="primary" @click="save">
                  保存
                </NButton>
              </NSpace>
            </template>
            <NForm label-placement="top">
              <div class="abac-options">
                <NFormItem label="授权阶段">
                  <NSelect
                    :value="preAuth ? 'PRE' : 'POST'" :disabled="loading"
                    :options="[{ label: '前置授权', value: 'PRE' }, { label: '事务内后置授权', value: 'POST', disabled: !postSupported }]"
                    @update:value="changePhase($event === 'PRE')"
                  />
                </NFormItem>
              </div>
              <NAlert v-if="schema && !schema.methods.length" type="warning" class="mb-12">
                该权限尚未绑定授权方法，请在服务方法声明对应权限。
              </NAlert>
              <NSelect v-if="methodOptions.length > 1" v-model:value="method" class="mb-12" :options="methodOptions" @update:value="resetSample" />
              <NAlert v-if="!preAuth" type="info" class="mb-12">
                后置规则在方法返回后、事务提交前执行；拒绝会回滚写入。调用时没有活动事务将直接报错。
              </NAlert>
              <SpelEditor v-model="condition" :schema="schema" :pre-auth="preAuth" :method="method" @run="simulate" />
              <p class="mt-8 text-12 opacity-70">
                输入 principal.、resource. 或 # 查看字段补全。Ctrl/⌘ + Enter 运行试算。
              </p>
            </NForm>
          </NCard>
          <div class="abac-context mt-16">
            <NCard title="可用字段与函数" size="small">
              <NScrollbar style="max-height: 420px">
                <div v-for="suggestion in suggestions" :key="suggestion.label" class="field-item">
                  <code>{{ suggestion.label }}</code>
                  <span class="text-12 opacity-60">{{ suggestion.detail }}</span>
                </div>
              </NScrollbar>
            </NCard>
            <NCard title="模拟上下文" size="small">
              <template #header-extra>
                <NButton text @click="resetSample">
                  重建样本
                </NButton>
              </template>
              <NAlert type="info" class="mb-12">
                此处 JSON 仅用于试算。真实请求的主体、资源与环境由服务端生成，方法参数来自本次调用。
              </NAlert>
              <NInput v-model:value="context" type="textarea" :autosize="{ minRows: 12, maxRows: 24 }" aria-label="模拟上下文 JSON" />
              <NButton class="mt-12" :disabled="loading || !condition.trim()" type="success" @click="simulate">
                校验并运行
              </NButton>
              <NAlert v-if="result" class="mt-12" :type="result.allowed ? 'success' : result.code === 'POLICY_DENIED' ? 'warning' : 'error'">
                <strong>{{ result.code }}</strong>：{{ result.message }}
              </NAlert>
            </NCard>
          </div>
        </div>
        <NEmpty v-else description="请选择权限" />
      </div>
    </NSpin>
  </CommonPage>
</template>

<script setup>
import { onMounted } from 'vue'
import SpelEditor from './components/SpelEditor.vue'
import { useAbacWorkbench } from './useAbacWorkbench.js'

defineOptions({ name: 'AbacMgt' })
const {
  permissions,
  policies,
  permission,
  preAuth,
  id,
  condition,
  schema,
  method,
  context,
  result,
  loading,
  error,
  permissionOptions,
  methodOptions,
  postSupported,
  suggestions,
  init,
  selectPermission,
  changePhase,
  edit,
  create,
  simulate,
  save,
  remove,
  refresh,
  resetSample,
} = useAbacWorkbench()
onMounted(init)
</script>

<style scoped>
.abac-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
}
.abac-options {
  display: grid;
  grid-template-columns: minmax(0, 280px);
  gap: 12px;
}
.abac-context {
  display: grid;
  grid-template-columns: 1fr 1.3fr;
  gap: 16px;
}
.policy-item {
  padding: 12px;
  border: 1px solid #8884;
  border-radius: 6px;
}
.selected {
  border-color: #18a058;
}
.policy-expression {
  overflow-wrap: anywhere;
  font-family: monospace;
  font-size: 12px;
}
.field-item {
  display: flex;
  flex-direction: column;
  padding: 8px 0;
  border-bottom: 1px solid #8882;
  overflow-wrap: anywhere;
}
@media (max-width: 1200px) {
  .abac-context {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 800px) {
  .abac-layout,
  .abac-options {
    grid-template-columns: 1fr;
  }
}
</style>
