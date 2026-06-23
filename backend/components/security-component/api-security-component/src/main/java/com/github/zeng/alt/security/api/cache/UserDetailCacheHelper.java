package com.github.zeng.alt.security.api.cache;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年09月26日 16:36
 */
public interface UserDetailCacheHelper {

	@Nullable
	public UserDetails get(@NonNull String id);

	public <T> T get(String id, Class<T> tClass);

	default void put(@NonNull String id, @NonNull UserDetails userDetails) {
		put(id, userDetails, Duration.ofMinutes(30L));
	}

	public void put(@NonNull String id, @NonNull UserDetails userDetails, Duration expireTime);

	public void remove(String username);

	public void remove(String username, String uuid);

	public void renew(String id);
}
