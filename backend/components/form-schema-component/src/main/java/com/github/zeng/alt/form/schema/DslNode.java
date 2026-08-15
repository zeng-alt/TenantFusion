package com.github.zeng.alt.form.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * FormKit 动态表单 DSL 节点（与前端 {@code src/types/dsl.ts} 对齐的语义化描述）。
 * <p>
 * 树形结构：根节点必须是容器（category=container），字段只能挂在容器 / 布局内。
 * 仅抽取服务端校验需要的字段，其余属性（渲染 / 布局 / 事件）不关心。
 *
 * @param id          稳定唯一 id
 * @param type        组件类型标识
 * @param category    语义分层：field / container / layout / static
 * @param name        字段名（提交到后端的数据 key；非字段节点可为空）
 * @param label       字段标签
 * @param visibleIf   条件显示（可移植表达式 AST，JSON 结构）
 * @param validation  校验规则
 * @param children    子节点（容器 / 布局）
 * @author zengAlt
 */
public record DslNode(
        String id,
        String type,
        String category,
        String name,
        String label,
        JsonNode visibleIf,
        List<DslValidationRule> validation,
        List<DslNode> children
) {

    /** 是否字段节点 */
    public boolean isField() {
        return "field".equals(category);
    }

    /** 是否有子节点 */
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}
