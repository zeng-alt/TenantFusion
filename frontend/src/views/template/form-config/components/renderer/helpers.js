/**********************************
 * 表单渲染器通用工具函数
 **********************************/

import { FIELD_TYPE_META, hasOptions, isComposite } from '../../constants'

export { hasOptions, isComposite }

/** 安全解析 JSON 字符串，解析失败返回 fallback */
export function parseJson(str, fallback = null) {
  if (str == null || str === '')
    return fallback
  try {
    return JSON.parse(str)
  }
  catch {
    return fallback
  }
}

/** 解析字段附加属性 fieldProps（JSON 字符串 -> 对象） */
export function parseFieldProps(field) {
  return parseJson(field?.fieldProps, {}) || {}
}

/** 解析字段校验规则（JSON 字符串 -> 数组） */
export function parseValidationRules(field) {
  const rules = parseJson(field?.validationRules, [])
  return Array.isArray(rules) ? rules : []
}

/** 解析字段条件渲染表达式（JSON 字符串 -> 对象） */
export function parseVisibilityCondition(field) {
  return parseJson(field?.visibilityCondition, null)
}

/** 预览值对象中的字段键：优先 fieldKey，其次客户端临时 _key */
export function fieldValueKey(field) {
  return field?.fieldKey || field?._key || ''
}

/** 24 栅格列宽 -> 网格样式 */
export function gridSpanStyle(colSpan) {
  const span = Math.min(Math.max(Number(colSpan) || 24, 1), 24)
  return { gridColumn: `span ${span} / span ${span}` }
}

/** 字段类型图标配色，用于区分类型语义 */
export function typeColor(type) {
  const meta = FIELD_TYPE_META[type]
  if (!meta)
    return 'text-gray-400'
  if (meta.composite)
    return 'text-primary'
  if (['BOOLEAN', 'SELECT', 'MULTI_SELECT', 'DATE', 'DATETIME'].includes(type))
    return 'text-green-600 dark:text-green-500'
  if (['FILE', 'IMAGE', 'RICH_TEXT'].includes(type))
    return 'text-orange-500 dark:text-orange-400'
  return 'text-sky-500 dark:text-sky-400'
}
