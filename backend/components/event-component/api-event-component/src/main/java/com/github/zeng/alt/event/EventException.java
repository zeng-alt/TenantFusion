package com.github.zeng.alt.event;

import com.github.zeng.alt.api.exception.BaseException;

/**
 * 事件组件的配置、编解码及传输异常。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class EventException extends BaseException {
    public EventException(String message) {
        super(message);
    }
    public EventException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
