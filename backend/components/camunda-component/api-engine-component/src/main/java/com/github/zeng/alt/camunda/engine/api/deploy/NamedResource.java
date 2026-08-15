package com.github.zeng.alt.camunda.engine.api.deploy;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 命名资源（BPMN/表单 XML 等）
 *
 * @author zengAlt
 */
@Data
@AllArgsConstructor
public class NamedResource implements Serializable {

    /**
     * 资源名称（如 leave.bpmn）
     */
    private String name;

    /**
     * 资源内容
     */
    private byte[] content;
}
