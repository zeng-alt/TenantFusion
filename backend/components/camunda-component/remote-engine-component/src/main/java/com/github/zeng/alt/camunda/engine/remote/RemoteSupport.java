package com.github.zeng.alt.camunda.engine.remote;

import org.camunda.community.rest.client.model.VariableValueDto;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 远程引擎通用转换工具
 *
 * @author zengAlt
 */
public final class RemoteSupport {

    private RemoteSupport() {
    }

    public static VariableValueDto variableValue(Object value) {
        VariableValueDto dto = new VariableValueDto();
        dto.setValue(value);
        return dto;
    }

    public static Map<String, VariableValueDto> toVariableMap(Map<String, Object> variables) {
        Map<String, VariableValueDto> map = new HashMap<>();
        if (variables != null) {
            variables.forEach((k, v) -> {
                if (v != null) {
                    map.put(k, variableValue(v));
                }
            });
        }
        return map;
    }

    public static Map<String, Object> fromVariableMap(Map<String, VariableValueDto> variables) {
        Map<String, Object> map = new HashMap<>();
        if (variables != null) {
            variables.forEach((k, v) -> map.put(k, v == null ? null : v.getValue()));
        }
        return map;
    }

    public static LocalDateTime toLocalDateTime(OffsetDateTime odt) {
        return odt == null ? null : odt.toLocalDateTime();
    }

    public static boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}
