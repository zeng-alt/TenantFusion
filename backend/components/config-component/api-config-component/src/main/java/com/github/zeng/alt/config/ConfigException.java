package com.github.zeng.alt.config;

import com.github.zeng.alt.api.exception.BaseException;

/**
 * 配置组件的异常路径，交由项目统一异常渲染处理。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class ConfigException extends BaseException {
    public ConfigException(String message) {
        super(message);
    }
}
