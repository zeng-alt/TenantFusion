import { defineStore } from 'pinia'

export const useDictStore = defineStore('dict', {
  state: () => ({
    dictMap: {},
  }),
  actions: {
    setDictData(code, list) {
      this.dictMap[code] = list
    },
    clearDicts() {
      this.dictMap = {}
    },
  },
})
