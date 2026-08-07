/**********************************
 * 字段类型 -> 渲染组件 注册表
 **********************************/

import BooleanWidget from './BooleanWidget.vue'
import DateWidget from './DateWidget.vue'
import NumberWidget from './NumberWidget.vue'
import RichTextWidget from './RichTextWidget.vue'
import SelectWidget from './SelectWidget.vue'
import TextareaWidget from './TextareaWidget.vue'
import TextWidget from './TextWidget.vue'
import UploadWidget from './UploadWidget.vue'

/** 叶子字段类型组件映射 */
export const FIELD_TYPE_WIDGETS = {
  STRING: TextWidget,
  TEXTAREA: TextareaWidget,
  NUMBER: NumberWidget,
  BOOLEAN: BooleanWidget,
  DATE: DateWidget,
  DATETIME: DateWidget,
  SELECT: SelectWidget,
  MULTI_SELECT: SelectWidget,
  FILE: UploadWidget,
  IMAGE: UploadWidget,
  RICH_TEXT: RichTextWidget,
}

/** 根据字段类型获取渲染组件 */
export function getFieldWidget(type) {
  return FIELD_TYPE_WIDGETS[type] || TextWidget
}
