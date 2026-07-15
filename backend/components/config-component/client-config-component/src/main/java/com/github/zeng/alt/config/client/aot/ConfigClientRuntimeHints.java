package com.github.zeng.alt.config.client.aot;

import com.github.zeng.alt.config.model.ConfigItemDTO;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class ConfigClientRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerType(ConfigItemDTO.class,
                MemberCategory.INTROSPECT_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS);
        hints.serialization().registerType(ConfigItemDTO.class);
    }
}
