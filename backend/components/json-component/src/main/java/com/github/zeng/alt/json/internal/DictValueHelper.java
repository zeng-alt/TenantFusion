package com.github.zeng.alt.json.internal;

import com.github.zeng.alt.api.base.BaseEnum;
import com.github.zeng.alt.json.annotation.DictFormat;
import com.github.zeng.alt.json.spi.IDictEnum;
import java.util.HashMap;
import java.util.Map;

/**
 * 两个 Jackson 版本共用的枚举字典映射。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class DictValueHelper {
    private DictValueHelper() {
    }

    /** 在映射器发现属性时预构建映射，空映射表示使用数据库字典。 */
    public static Map<String, String> enumLabels(Class<? extends Enum<?>> enumClass) {
        if (enumClass == DictFormat.NoDictEnum.class) {
            return Map.of();
        }
        final Enum<?>[] constants = enumClass.getEnumConstants();
        final Map<String, String> labels = new HashMap<>(constants.length);
        for (Enum<?> constant : constants) {
            final String code = constant instanceof IDictEnum dict
                    ? java.util.Objects.requireNonNullElse(dict.getCode(), constant.name())
                    : constant instanceof BaseEnum base ? String.valueOf(base.getCode()) : constant.name();
            final String label = constant instanceof IDictEnum dict ? dict.getLabel()
                    : constant instanceof BaseEnum base ? base.getLabel() : constant.name();
            labels.put(code, label);
        }
        return Map.copyOf(labels);
    }

}
