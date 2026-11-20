export function flattenFields(fields = []) {
  return fields.flatMap(field => [field, ...flattenFields(field.fields)])
}

export function completionOptions(schema, preAuth = true, methodName) {
  if (!schema)
    return []
  const methods = methodName ? schema.methods.filter(method => method.method === methodName) : schema.methods
  const fields = [
    ...schema.globals,
    ...methods.flatMap(method => method.parameters.map(parameter => parameter.schema)),
    ...(!preAuth ? methods.map(method => method.returned) : []),
  ]
  const unique = new Map(flattenFields(fields).map(field => [field.path, {
    label: field.path,
    type: 'property',
    detail: `${field.type} · ${field.source}${field.nullable ? ' · 可空' : ''}`,
  }]))
  for (const name of schema.functions) {
    unique.set(`${name}()`, {
      label: `${name}()`,
      type: 'function',
      detail: '授权函数',
      apply: `${name}(${name.startsWith('has') ? '\'\'' : ''})`,
    })
  }
  return [...unique.values()]
}

function sampleValue(field, input = false) {
  if (field.fields.length && !field.type.includes('Collection') && !field.type.includes('List') && !field.type.endsWith('[]')) {
    return Object.fromEntries(field.fields.map(child => [child.path.slice(field.path.length + 1), sampleValue(child, input)]))
  }
  if (field.type.includes('List') || field.type.includes('Collection') || field.type.endsWith('[]'))
    return []
  if (input && field.nullable)
    return null
  if (/Boolean|boolean/.test(field.type))
    return true
  if (/Long|Integer|Double|Float|BigDecimal|BigInteger|\bint\b|\blong\b/.test(field.type))
    return 0
  return ''
}

export function sampleContext(schema, preAuth = true, methodName) {
  const context = Object.fromEntries(schema.globals.filter(field => field.fields.length)
    .map(field => [field.path, sampleValue(field)]))
  context.authentication = { name: '示例用户', authenticated: true, roles: [], authorities: [] }
  context.variables = {}
  const method = schema.methods.find(item => item.method === methodName) || schema.methods[0]
  if (method) {
    for (const parameter of method.parameters.filter(item => !/^[pa]\d+$/.test(item.name)))
      context.variables[parameter.name] = sampleValue(parameter.schema, true)
    if (!preAuth)
      context.returnObject = sampleValue(method.returned)
  }
  return context
}

export function builderContext(schema, preAuth = true, methodName) {
  if (!schema)
    return { authentication: {}, principal: {}, locals: {}, globals: {} }
  const sample = sampleContext(schema, preAuth, methodName)
  const globals = Object.fromEntries(Object.entries(sample).filter(([name]) => !['principal', 'authentication', 'variables'].includes(name)))
  const method = schema.methods.find(item => item.method === methodName) || schema.methods[0]
  const locals = Object.fromEntries((method?.parameters || []).map(parameter => [parameter.name, sampleValue(parameter.schema)]))
  return { authentication: sample.authentication, principal: sample.principal || {}, globals, locals }
}
