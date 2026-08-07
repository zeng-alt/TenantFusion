/**********************************
 * 表单校验逻辑
 **********************************/

import { fieldValueKey, parseFieldProps, parseValidationRules, parseVisibilityCondition } from './helpers'

const EMAIL_RE = /^[^\s@]+@[^\s@][^\s.@]*\.[^\s@]+$/
const PHONE_RE = /^1[3-9]\d{9}$/

/** 校验单个字段，返回错误信息（空字符串表示通过） */
export function validateField(field, value) {
  if (!field)
    return ''
  const label = field.fieldLabel || field.fieldKey || '该字段'
  const errors = []
  if (field.required) {
    const empty = value === null || value === undefined || value === ''
      || (Array.isArray(value) && value.length === 0)
    if (empty)
      errors.push(`${label}为必填项`)
  }
  if (value !== null && value !== undefined && value !== '') {
    const rules = parseValidationRules(field)
    for (const rule of rules) {
      switch (rule.type) {
        case 'minLength':
          if (String(value).length < Number(rule.value))
            errors.push(`最小长度至少 ${rule.value} 个字符`)
          break
        case 'maxLength':
          if (String(value).length > Number(rule.value))
            errors.push(`最大长度不超过 ${rule.value} 个字符`)
          break
        case 'min':
          if (Number(value) < Number(rule.value))
            errors.push(`不能小于 ${rule.value}`)
          break
        case 'max':
          if (Number(value) > Number(rule.value))
            errors.push(`不能大于 ${rule.value}`)
          break
        case 'pattern':
          if (rule.value && !new RegExp(rule.value).test(String(value)))
            errors.push(rule.message || '格式不符合要求')
          break
        case 'email':
          if (!EMAIL_RE.test(String(value)))
            errors.push(rule.message || '邮箱格式不正确')
          break
        case 'phone':
          if (!PHONE_RE.test(String(value)))
            errors.push(rule.message || '手机号格式不正确')
          break
        case 'integer':
          if (!/^-?\d+$/.test(String(value)))
            errors.push(rule.message || '必须为整数')
          break
        case 'url':
          if (!/^https?:\/\/.+/.test(String(value)))
            errors.push(rule.message || 'URL 格式不正确')
          break
      }
      if (errors.length > 0)
        break
    }
  }
  return errors.join('；')
}

/** 是否满足条件渲染表达式（返回是否显示） */
export function evaluateVisibility(field, values) {
  const condition = parseVisibilityCondition(field)
  if (!condition || !Array.isArray(condition.conditions) || condition.conditions.length === 0)
    return true
  const results = condition.conditions.map((cond) => {
    const actual = values?.[cond.fieldKey]
    const target = cond.value
    switch (cond.operator) {
      case 'eq':
        return looseEq(actual, target)
      case 'neq':
        return !looseEq(actual, target)
      case 'gt':
        return Number(actual) > Number(target)
      case 'gte':
        return Number(actual) >= Number(target)
      case 'lt':
        return Number(actual) < Number(target)
      case 'lte':
        return Number(actual) <= Number(target)
      case 'contains':
        return String(actual ?? '').includes(String(target ?? ''))
      case 'notContains':
        return !String(actual ?? '').includes(String(target ?? ''))
      case 'empty':
        return isEmpty(actual)
      case 'notEmpty':
        return !isEmpty(actual)
      case 'in':
        return String(target ?? '').split(',').map(item => item.trim()).includes(String(actual ?? ''))
      case 'notIn':
        return !String(target ?? '').split(',').map(item => item.trim()).includes(String(actual ?? ''))
      default:
        return true
    }
  })
  return condition.logic === 'or' ? results.some(Boolean) : results.every(Boolean)
}

/** 递归收集所有字段错误，返回 { [fieldKey]: 错误信息 } */
export function collectErrors(fields, values, target = {}) {
  for (const field of fields || []) {
    if (!field || field.hidden)
      continue
    if (!evaluateVisibility(field, values))
      continue
    if (field.fieldType === 'LIST') {
      const rows = Array.isArray(values?.[fieldValueKey(field)]) ? values[fieldValueKey(field)] : []
      rows.forEach(row => collectErrors(field.children, row, target))
    }
    else if (field.children?.length) {
      collectErrors(field.children, values, target)
    }
    else {
      const message = validateField(field, values?.[fieldValueKey(field)])
      if (message)
        target[fieldValueKey(field) || field._key] = message
    }
  }
  return target
}

/** 依据 fieldProps 生成某字段的输入约束（预留，供展示用） */
export function fieldConstraints(field) {
  const props = parseFieldProps(field)
  return {
    maxLength: props.maxLength,
    min: props.min,
    max: props.max,
    precision: props.precision,
    step: props.step,
  }
}

/**
 * 递归校验字段结构（字段标识、字段标签必填，字段标识全局唯一），返回错误映射
 * @returns {Map<object, string[]>} field -> 错误信息数组
 */
export function collectStructureErrors(fields, target = new Map()) {
  const seenKeys = new Map()
  const check = (list) => {
    for (const field of list || []) {
      if (!field)
        continue
      const key = String(field.fieldKey || '').trim()
      const errors = []
      if (!key)
        errors.push('字段标识必填')
      if (seenKeys.has(key)) {
        errors.push(`字段标识“${key}”重复`)
        const firstErrors = target.get(seenKeys.get(key)) || []
        if (!firstErrors.includes(`字段标识“${key}”重复`)) {
          firstErrors.push(`字段标识“${key}”重复`)
          target.set(seenKeys.get(key), firstErrors)
        }
      }
      else if (key) {
        seenKeys.set(key, field)
      }
      if (!field.fieldLabel || !String(field.fieldLabel).trim())
        errors.push('字段标签必填')
      if (errors.length) {
        const existing = target.get(field) || []
        target.set(field, [...existing, ...errors])
      }
      if (field.children?.length)
        check(field.children)
    }
  }
  check(fields)
  return target
}

function looseEq(a, b) {
  if (a === b)
    return true
  if ((a === null || a === undefined || a === '') && (b === null || b === undefined || b === ''))
    return true
  return String(a) === String(b)
}

function isEmpty(value) {
  return value === null || value === undefined || value === ''
    || (Array.isArray(value) && value.length === 0)
}
