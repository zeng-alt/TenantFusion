/**********************************
 * FormBuilder 全局配置 —— 表单设计器与表单数据详情共用
 **********************************/

import { request } from '@/utils'

const treeDictionaries = [
  {
    value: 'orgTree',
    label: '组织架构树',
    options: [
      { label: '总公司', value: 'hq', children: [{ label: '研发部', value: 'rd' }, { label: '市场部', value: 'marketing' }] },
      { label: '分公司', value: 'branch', children: [{ label: '华东区', value: 'east' }, { label: '华南区', value: 'south' }] },
    ],
  },
]

export function createFormBuilderConfig() {
  return {
    locale: 'zh-CN',
    localeFallback: 'zh-CN',
    availableLocales: ['zh-CN', 'en'],
    fetchDictionary: async (code) => {
      try {
        const { data = [] } = await request.get('/dict/data/all', { params: { dictCode: code, enabled: true } })
        return (data || []).map(item => ({ label: item.dictLabel, value: item.dictValue }))
      }
      catch {
        return []
      }
    },
    fetchDictionaryPage: async ({ code, label, pageNum, pageSize }) => {
      try {
        const { data } = await request.get('/dict/type', {
          params: { dictCode: code, dictName: label, pageNo: pageNum, pageSize },
        })
        return {
          pageNum: data.pageNum ?? pageNum,
          pageSize: data.pageSize ?? pageSize,
          total: data.total ?? 0,
          data: (data.pageData || []).map(item => ({ code: item.dictCode, label: item.dictName })),
        }
      }
      catch {
        return { pageNum, pageSize, total: 0, data: [] }
      }
    },
    fetchTreeDictionary: async (value) => {
      const hit = treeDictionaries.find(item => item.value === value)
      return hit ? hit.options : []
    },
    fetchTreeDictionaryPage: async ({ value, label, pageNum, pageSize }) => {
      let list = treeDictionaries
      if (value)
        list = list.filter(item => item.value.includes(value))
      if (label)
        list = list.filter(item => item.label.includes(label))
      return {
        pageNum,
        pageSize,
        total: list.length,
        data: list.map(item => ({ value: item.value, label: item.label })),
      }
    },
  }
}
