package com.github.zeng.alt.doc;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ComponentsProperties {

    /**
     * key -> SecurityScheme
     */
    private Map<String, SecuritySchemeProperties> securitySchemes = new LinkedHashMap<>();
}