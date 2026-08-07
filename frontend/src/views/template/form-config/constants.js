/**********************************
 * 配置表单设计器常量与工具函数
 **********************************/

/** 字段类型元数据 */
export const FIELD_TYPE_META = {
  STRING: { label: '单行文本', icon: 'i-material-symbols:short-text', group: '基础', composite: false },
  TEXTAREA: { label: '多行文本', icon: 'i-material-symbols:notes', group: '基础', composite: false },
  NUMBER: { label: '数字', icon: 'i-material-symbols:pin', group: '基础', composite: false },
  BOOLEAN: { label: '开关', icon: 'i-material-symbols:toggle-on', group: '基础', composite: false },
  DATE: { label: '日期', icon: 'i-material-symbols:calendar-month', group: '选择', composite: false },
  DATETIME: { label: '日期时间', icon: 'i-material-symbols:schedule', group: '选择', composite: false },
  SELECT: { label: '下拉单选', icon: 'i-material-symbols:arrow-drop-down-circle', group: '选择', composite: false },
  MULTI_SELECT: { label: '下拉多选', icon: 'i-material-symbols:checklist', group: '选择', composite: false },
  FILE: { label: '文件上传', icon: 'i-material-symbols:attach-file', group: '高级', composite: false },
  IMAGE: { label: '图片上传', icon: 'i-material-symbols:image', group: '高级', composite: false },
  RICH_TEXT: { label: '富文本', icon: 'i-material-symbols:format-color-text', group: '高级', composite: false },
  LIST: { label: '列表', icon: 'i-material-symbols:view-list', group: '复合', composite: true },
  OBJECT: { label: '对象分组', icon: 'i-material-symbols:category', group: '复合', composite: true },
}

/** 基础字段类型（不含复合类型） */
export const BASIC_FIELD_TYPES = Object.keys(FIELD_TYPE_META).filter(t => !FIELD_TYPE_META[t].composite)

/** 复合字段类型（可嵌套子字段） */
export const COMPOSITE_FIELD_TYPES = Object.keys(FIELD_TYPE_META).filter(t => FIELD_TYPE_META[t].composite)

/** 条件渲染操作符 */
export const CONDITION_OPERATORS = {
  eq: '等于',
  neq: '不等于',
  gt: '大于',
  gte: '大于等于',
  lt: '小于',
  lte: '小于等于',
  contains: '包含',
  notContains: '不包含',
  empty: '为空',
  notEmpty: '不为空',
  in: '包含于',
  notIn: '不包含于',
}

/** 校验规则类型元数据 */
export const VALIDATION_RULE_META = {
  minLength: { label: '最小长度', hasValue: true, valueType: 'number', unit: '个字符' },
  maxLength: { label: '最大长度', hasValue: true, valueType: 'number', unit: '个字符' },
  min: { label: '最小值', hasValue: true, valueType: 'number', unit: '' },
  max: { label: '最大值', hasValue: true, valueType: 'number', unit: '' },
  pattern: { label: '正则匹配', hasValue: true, valueType: 'text', unit: '' },
  email: { label: '邮箱格式', hasValue: false, valueType: 'none', unit: '' },
  phone: { label: '手机号格式', hasValue: false, valueType: 'none', unit: '' },
  integer: { label: '整数', hasValue: false, valueType: 'none', unit: '' },
  url: { label: 'URL 格式', hasValue: false, valueType: 'none', unit: '' },
}

/** 创建一个默认字段对象 */
export function createField(type, index = 0) {
  const field = {
    fieldId: undefined,
    parentFieldId: null,
    fieldKey: '',
    fieldLabel: '',
    fieldType: type,
    defaultValue: null,
    placeholder: '',
    helpText: '',
    sortOrder: index,
    colSpan: 24,
    required: false,
    readonly: false,
    hidden: false,
    validationRules: null,
    visibilityCondition: null,
    fieldProps: null,
    options: type === 'SELECT' || type === 'MULTI_SELECT' ? [] : null,
    children: FIELD_TYPE_META[type]?.composite ? [] : null,
  }
  return field
}

/** 根据字段类型判断是否可嵌套子字段 */
export function isComposite(type) {
  return FIELD_TYPE_META[type]?.composite === true
}

/** 根据字段类型判断是否有下拉选项 */
export function hasOptions(type) {
  return type === 'SELECT' || type === 'MULTI_SELECT'
}
