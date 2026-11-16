package com.github.zeng.alt.config;

import com.github.zeng.alt.config.model.ConfigItemDTO;
import io.vavr.control.Either;
import java.util.List;

/**
 * 已发布配置的数据源；服务端提供此接口后，同进程客户端优先使用本地数据源。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface ConfigSourceTemplate {
    Either<String, List<ConfigItemDTO>> getAll(String appCode);
}
