package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.security.abac.AbacMethodContext;
import com.github.zeng.alt.security.abac.AbacResourceResolver;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 用户资源解析器：从方法参数 #id 读取数据库用户并映射为标量属性（含 id），
 * 在 {@code @AbacAuthorize.resources} 声明使用。不从请求体复制授权属性。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Component
@RequiredArgsConstructor
public class UserResourceResolver implements AbacResourceResolver {
    private final UserRepository users;
    private final AbacUserMapper mapper;

    @Override
    public Map<String, Object> resolve(AbacMethodContext context) {
        final Long userId = Try.of(() -> Long.valueOf(String.valueOf(context.paramByName().get("id"))))
                .getOrElseThrow(() -> new IllegalStateException("资源标识无效"));
        return users.findById(userId)
                .map(user -> mapper.toDto(user).attributes())
                .getOrElseThrow(() -> new IllegalStateException("目标用户不存在"));
    }
}