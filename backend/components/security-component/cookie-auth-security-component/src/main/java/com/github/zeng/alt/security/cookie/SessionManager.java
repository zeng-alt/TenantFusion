package com.github.zeng.alt.security.cookie;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.storage.StorageTemplate;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
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
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String cacheKey = CACHE_PREFIX + sessionId;
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
        String cacheKey = CACHE_PREFIX + sessionId;
        return storageTemplate.opsForString().get(cacheKey, SecurityUser.class);
    }

    /**
     * 删除指定会话（登出时调用）.
     *
     * @param sessionId 会话 ID
     */
    public void removeSession(String sessionId) {
        String cacheKey = CACHE_PREFIX + sessionId;
        storageTemplate.delete(cacheKey);
    }
}
