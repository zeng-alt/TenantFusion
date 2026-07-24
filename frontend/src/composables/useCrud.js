/**********************************
 * @Author: Ronnie Zhang
 * @LastEditor: Ronnie Zhang
 * @LastEditTime: 2023/12/12 09:03:00
 * @Email: zclzone@outlook.com
 * Copyright © 2023 Ronnie Zhang(大脸怪) | https://isme.top
 **********************************/

import { cloneDeep } from 'lodash-es'
import { useForm, useModal } from '.'

const ACTIONS = {
  view: '查看',
  edit: '编辑',
  add: '新增',
}

export function useCrud({ name, initForm = {}, doCreate, doDelete, doUpdate, doSort, refresh }) {
  const modalAction = ref('')
  const [modalRef, okLoading] = useModal()
  const [modalFormRef, modalForm, validation] = useForm(initForm)

  /** 排序 */
  async function handleSort({ data, oldIndex, newIndex, rowKey, sortKey }) {
    if (!doSort || oldIndex === newIndex)
      return

    const list = [...data]

    // 保存原排序值
    const start = Math.min(oldIndex, newIndex)
    const end = Math.max(oldIndex, newIndex)

    const sorts = list
      .slice(start, end + 1)
      .map(item => item[sortKey])

    // 移动元素
    const [movedItem] = list.splice(oldIndex, 1)
    list.splice(newIndex, 0, movedItem)

    // 重新分配排序值
    const changedItems = list
      .slice(start, end + 1)
      .map((item, index) => ({
        id: item[rowKey],
        sort: sorts[index],
      }))

    await doSort(changedItems)
  }

  /** 新增 */
  function handleAdd(row = {}, title) {
    handleOpen({ action: 'add', title, row: Object.assign({}, cloneDeep(initForm), cloneDeep(row)) })
  }

  /** 修改 */
  function handleEdit(row, title) {
    handleOpen({ action: 'edit', title, row })
  }

  /** 查看 */
  function handleView(row, title) {
    handleOpen({ action: 'view', title, row })
  }

  /** 打开modal */
  function handleOpen(options = {}) {
    const { action, row, title, onOk } = options
    modalAction.value = action
    modalForm.value = { ...row }
    modalRef.value?.open({
      ...options,
      async onOk() {
        if (typeof onOk === 'function') {
          return await onOk()
        }
        else {
          return await handleSave()
        }
      },
      title: title ?? (ACTIONS[modalAction.value] || '') + name,
    })
  }

  /** 保存 */
  async function handleSave(action) {
    if (!action && !['edit', 'add'].includes(modalAction.value)) {
      return false
    }
    await validation()
    const actions = {
      add: {
        api: () => doCreate(modalForm.value),
        cb: () => $message.success('新增成功'),
      },
      edit: {
        api: () => doUpdate(modalForm.value),
        cb: () => $message.success('保存成功'),
      },
    }

    action = action || actions[modalAction.value]

    try {
      okLoading.value = true
      const data = await action.api()
      action.cb()
      okLoading.value = false
      data && refresh(data)
    }
    catch (error) {
      console.error(error)
      okLoading.value = false
      return false
    }
  }

  /** 启用/停用切换 */
  async function handleEnable(row, idField = 'id', fieldName = 'enabled') {
    row[`${fieldName}Loading`] = true
    try {
      await doUpdate({ [idField]: row[idField], [fieldName]: !row[fieldName] })
      $message.success('操作成功')
      row[`${fieldName}Loading`] = false
      refresh && refresh(true)
    }
    catch (error) {
      console.error(error)
      row[`${fieldName}Loading`] = false
    }
  }

  /** 删除 */
  function handleDelete(ids, confirmOptions) {
    if (!ids && ids !== 0)
      return
    const d = $dialog.warning({
      content: '确定删除？',
      title: '提示',
      positiveText: '确定',
      negativeText: '取消',
      async onPositiveClick() {
        try {
          d.loading = true
          const data = await doDelete(Array.isArray(ids) ? ids.join(',') : ids)
          $message.success('删除成功')
          d.loading = false
          refresh(data, true)
        }
        catch (error) {
          console.error(error)
          d.loading = false
        }
      },
      ...confirmOptions,
    })
  }

  return {
    modalRef,
    modalFormRef,
    modalAction,
    modalForm,
    okLoading,
    validation,
    handleAdd,
    handleDelete,
    handleEdit,
    handleEnable,
    handleView,
    handleOpen,
    handleSave,
    handleSort,
  }
}
