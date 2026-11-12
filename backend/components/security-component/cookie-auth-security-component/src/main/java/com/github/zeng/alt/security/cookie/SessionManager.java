package com.github.zeng.alt.security.cookie;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.storage.StorageTemplate;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * Session 管理器，通过 {@link StorageTemplate} 在缓存中存储用户会话.
 * <p>
 * 每个会话对应一个唯一 sessionId，缓存键格式为 {@code session:<sessionId>}。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@RequiredArgsConstructor
public class SessionManager {

    private static final String CACHE_PREFIX = "session:";

    private final StorageTemplate storageTemplate;
    private final long expirationSeconds;

    /**
     * 创建新会话，将用户信息存入缓存.
     *
     * @param user 安全用户
     * @return sessionId
     */
    public String createSession(SecurityUser user) {
        Assert.hasText(user.getId(), "Cookie 会话用户 ID 不能为空");
        final String sessionId = sessionPrefix(user.getTenant(), user.getId())
                + UUID.randomUUID().toString().replace("-", "");
        final String cacheKey = CACHE_PREFIX + sessionId;
        storageTemplate.opsForString().set(cacheKey, user, Duration.ofSeconds(expirationSeconds));
        return sessionId;
    }

    /**
     * 根据 sessionId 获取已登录用户信息.
     *
     * @param sessionId 会话 ID
     * @return SecurityUser 或 null（会话不存在或已过期）
     */
    public SecurityUser getSession(String sessionId) {
        final String cacheKey = CACHE_PREFIX + sessionId;
        // 旧格式未携带用户范围，无法保证按用户撤销；升级后要求重新登录。
        return Option.of(storageTemplate.opsForString().get(cacheKey, SecurityUser.class))
                .filter(user -> sessionId.startsWith(sessionPrefix(user.getTenant(), user.getId()))).getOrNull();
    }

    /**
     * 删除指定会话（登出时调用）.
     *
     * @param sessionId 会话 ID
     */
    public void removeSession(String sessionId) {
        final String cacheKey = CACHE_PREFIX + sessionId;
        storageTemplate.opsForString().delete(cacheKey);
    }

    /** 撤销指定租户用户的全部会话，不影响其他租户的同 ID 用户。 */
    public long removeUserSessions(String userId, String tenant) {
        Assert.hasText(userId, "Cookie 会话用户 ID 不能为空");
        return storageTemplate.opsForString().deleteByPattern(CACHE_PREFIX + sessionPrefix(tenant, userId) + "*");
    }

    private String sessionPrefix(String tenant, String userId) {
        final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(Option.of(tenant).getOrElse("").getBytes(StandardCharsets.UTF_8)) + "."
                + encoder.encodeToString(userId.getBytes(StandardCharsets.UTF_8)) + ".";
    }
}
