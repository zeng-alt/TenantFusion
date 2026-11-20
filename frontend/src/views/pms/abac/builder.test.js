/* eslint test/no-import-node-test: off */
import assert from 'node:assert/strict'
import test from 'node:test'
import { normalizeOperators, parseVisualRule, ruleNodeToSpel } from './builder.js'
import { builderContext } from './model.js'

test('实际 query-builder 往返保留资源与方法参数路径及逻辑分组', () => {
  const expression = 'principal.deptId == resource.deptId and (#dto.deptId == null or #dto.deptId == principal.deptId)'
  const generated = ruleNodeToSpel(parseVisualRule(expression))
  assert.equal(generated, '(principal.deptId == resource.deptId && (#dto.deptId == null || #dto.deptId == principal.deptId))')
  assert.ok(!generated.includes('#resource'))
})

test('运算符归一化不会修改字符串或字段名', () => {
  assert.equal(normalizeOperators('principal.name == \'and or not\' and env.or == true'), 'principal.name == \'and or not\' && env.or == true')
})

test('不能完整转换时拒绝切换，避免覆写原表达式', () => {
  assert.throws(() => parseVisualRule('hasRole(\'EDITOR\')'), /原表达式已保留/)
  assert.equal(ruleNodeToSpel(parseVisualRule('')), '')
})

test('构建器按后端字段定义分别接收 globals 和 locals，无旧命名别名', () => {
  const field = (path, type) => ({ path, type, nullable: true, source: '测试', fields: [] })
  const context = builderContext({
    globals: [
      { ...field('resource', 'Object'), fields: [field('resource.deptId', 'java.lang.Long')] },
      { ...field('env', 'Object'), fields: [field('env.hour', 'Integer')] },
    ],
    methods: [{ method: 'Users.update', parameters: [{ name: 'id', index: 0, schema: field('#id', 'java.lang.Long') }], returned: field('returnObject', 'java.lang.Long') }],
    functions: [],
  }, false)
  assert.deepEqual(context.globals, { resource: { deptId: 0 }, env: { hour: 0 }, returnObject: 0 })
  assert.deepEqual(context.locals, { id: 0 })
  assert.ok(!('subject' in context))
})
