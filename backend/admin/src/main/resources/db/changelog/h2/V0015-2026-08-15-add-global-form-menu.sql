--liquibase formatted sql

-- 原计划在此新增全局表单菜单，但权限ID 301 与日志菜单（LogMgt）冲突导致迁移失败。
-- 已废弃，全局表单菜单迁移移至 V0016-2026-08-15-add-global-form-menu.sql（ID=304）。
