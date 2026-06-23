package com.github.zeng.alt.security.api.cache;


import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GuavaUserDetailCacheHelper implements UserDetailCacheHelper {

    // key = username:uuid
    private final Map<String, CacheValue> cache = new ConcurrentHashMap<>();

    // username -> uuids
    private final SetMultimap<String, String> userIndex =
            Multimaps.synchronizedSetMultimap(HashMultimap.create());

    // ---------------- inner ----------------

    private static class CacheValue {
        final UserDetails value;
        volatile long expireAt;

        CacheValue(UserDetails value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }

    // ---------------- get ----------------

    @Override
    @Nullable
    public UserDetails get(@NonNull String id) {
        CacheValue v = cache.get(id);

        if (v == null) {
            return null;
        }

        if (System.currentTimeMillis() > v.expireAt) {
            cache.remove(id);
            return null;
        }

        return v.value;
    }

    @Override
    public <T> T get(String id, Class<T> tClass) {
        UserDetails userDetails = get(id);
        if (userDetails == null) {
            return null;
        }

        return tClass.isInstance(userDetails) ? tClass.cast(userDetails) : null;
    }

    // ---------------- put ----------------

    public void put(@NonNull String username,
                    @NonNull String uuid,
                    @NonNull UserDetails userDetails,
                    Duration expireTime) {

        String key = buildKey(username, uuid);
        long expireAt = System.currentTimeMillis() + expireTime.toMillis();

        cache.put(key, new CacheValue(userDetails, expireAt));
        userIndex.put(username, uuid);
    }

    // 兼容接口默认方法（不建议单独用）
    @Override
    public void put(@NonNull String id,
                    @NonNull UserDetails userDetails,
                    Duration expireTime) {

        throw new UnsupportedOperationException(
                "Use put(username, uuid, userDetails, expireTime) instead");
    }

    // ---------------- remove ----------------

    @Override
    public void remove(String username) {

        Set<String> uuids = userIndex.removeAll(username);

        if (uuids != null) {
            for (String uuid : uuids) {
                cache.remove(buildKey(username, uuid));
            }
        }
    }

    @Override
    public void remove(String username, String uuid) {

        cache.remove(buildKey(username, uuid));
        userIndex.remove(username, uuid);
    }

    // ---------------- renew ----------------

    @Override
    public void renew(String id) {
        CacheValue v = cache.get(id);
        if (v != null) {
            v.expireAt = System.currentTimeMillis() + Duration.ofMinutes(30).toMillis();
        }
    }

    // ---------------- helper ----------------

    private String buildKey(String username, String uuid) {
        return username + ":" + uuid;
    }
}