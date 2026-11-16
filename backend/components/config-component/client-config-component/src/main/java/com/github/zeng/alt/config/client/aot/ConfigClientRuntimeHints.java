package com.github.zeng.alt.config.client.aot;

import com.github.zeng.alt.config.model.ConfigChangedKey;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class ConfigClientRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        new BindingReflectionHintsRegistrar().registerReflectionHints(
            hints.reflection(),
            ConfigItemDTO.class,
            ConfigChangedKey.class
        );
    }
}
