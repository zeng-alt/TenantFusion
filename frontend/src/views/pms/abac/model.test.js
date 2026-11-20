/* eslint test/no-import-node-test: off */
import assert from 'node:assert/strict'
import test from 'node:test'
import { completionOptions, sampleContext } from './model.js'

const leaf = (path, type = 'java.lang.Long', nullable = true) => ({ path, type, nullable, source: '方法参数', fields: [] })
const schema = {
  globals: [{ ...leaf('principal', 'Object'), fields: [leaf('principal.deptId')] }],
  methods: [{ method: 'Users.update', transactional: true, parameters: [
    { name: 'id', index: 0, schema: leaf('#id') },
    { name: 'p0', index: 0, schema: leaf('#p0') },
    { name: 'dto', index: 1, schema: { ...leaf('#dto', 'PatchUserDto'), fields: [leaf('#dto.deptId')] } },
  ], returned: { ...leaf('returnObject', 'UserVO'), fields: [leaf('returnObject.deptId')] } }],
  functions: ['hasRole', 'isAuthenticated'],
}

test('前置补全包含真实参数及字段，后置才补全返回对象', () => {
  const before = completionOptions(schema, true).map(item => item.label)
  assert.ok(before.includes('#dto.deptId'))
  assert.ok(before.includes('#p0'))
  assert.ok(before.includes('hasRole()'))
  assert.ok(!before.includes('returnObject.deptId'))
  assert.ok(completionOptions(schema, false).some(item => item.label === 'returnObject.deptId'))
})

test('样本使用参数名，位置别名由后端按索引绑定', () => {
  const context = sampleContext(schema, true)
  assert.deepEqual(context.variables, { id: null, dto: { deptId: null } })
  assert.ok(!('returnObject' in context))
  assert.equal(sampleContext(schema, false).returnObject.deptId, 0)
})

test('切换方法只补全所选方法参数，未加载时没有旧变量建议', () => {
  assert.deepEqual(completionOptions(null), [])
  assert.ok(!completionOptions(schema, true, 'Other.method').some(item => item.label.startsWith('#')))
  assert.ok(!completionOptions(schema, true).some(item => item.label.startsWith('subject') || item.label.startsWith('environment')))
})

test('字段补全只来自后端 schema，不从试算 JSON 注入可用字段', () => {
  const options = completionOptions(schema, true)
  assert.ok(options.every(item => !item.label.includes('password')))
  assert.ok(options.every(item => !item.label.includes('credentials')))
})
