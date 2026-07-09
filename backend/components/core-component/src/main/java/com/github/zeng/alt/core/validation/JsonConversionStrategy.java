package com.github.zeng.alt.core.validation;


import com.github.zeng.alt.json.JacksonHelper;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;

@Getter
public abstract class JsonConversionStrategy {

    private @Autowired JacksonHelper jacksonHelper;

    public void check(String jsonString) {

    }

    public abstract @NonNull <T> T convert(String jsonString) throws Exception;
    public abstract @NonNull String getType();
}