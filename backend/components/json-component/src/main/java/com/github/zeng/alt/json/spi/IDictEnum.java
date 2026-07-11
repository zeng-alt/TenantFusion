package com.github.zeng.alt.json.spi;

public interface IDictEnum {

    String getLabel();

    default String getCode() {
        return null;
    }

}
