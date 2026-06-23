package com.github.zeng.alt.security.jwt;

import com.github.zeng.alt.security.api.SecurityUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT Token 提供者，负责创建和验证 JWT.
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationSeconds;
    private final JwtParser jwtParser;

    private static final String CLAIM_ID = "id";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TENANT = "tenant";
    static final String CACHE_KEY_PREFIX = "jwt:token:";

    public JwtTokenProvider(String base64Secret, long expirationSeconds) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
        this.jwtParser = Jwts.parser().verifyWith(secretKey).build();
    }

    /**
     * 为指定用户创建 JWT token，包含唯一 jti 用于缓存管理.
     *
     * @param user 安全用户
     * @return JWT token 字符串
     */
    public String createToken(SecurityUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationSeconds, ChronoUnit.SECONDS);

        List<String> roles = user.getRoles().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .subject(user.getUsername())
                .claim(CLAIM_ID, user.getId())
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TENANT, user.getTenant())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从 token 中提取 jti（唯一标识），用于缓存键.
     */
    public String getTokenId(String token) {
        try {
            return jwtParser.parseSignedClaims(token).getPayload().getId();
        } catch (JwtException e) {
            return null;
        }
    }

    /**
     * 获取 token 的缓存键.
     */
    public String getCacheKey(String token) {
        String jti = getTokenId(token);
        return jti != null ? CACHE_KEY_PREFIX + jti : null;
    }

    /**
     * 获取 token 的过期时间（秒）.
     */
    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    /**
     * 验证 token 是否有效（签名 + 过期）.
     */
    public boolean validateToken(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token 已过期: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT token 验证失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 从 token 中提取 Claims.
     */
    public Claims getClaims(String token) {
        try {
            return jwtParser.parseSignedClaims(token).getPayload();
        } catch (JwtException e) {
            log.warn("解析 JWT Claims 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 token Claims 中重建 SecurityUser.
     */
    @SuppressWarnings("unchecked")
    public SecurityUser getUserFromClaims(Claims claims) {
        String id = claims.get(CLAIM_ID, String.class);
        String username = claims.getSubject();
        String tenant = claims.get(CLAIM_TENANT, String.class);
        List<String> roles = claims.get(CLAIM_ROLES, List.class);

        Set<GrantedAuthority> authorities = roles != null
                ? roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet())
                : Set.of();

        return new SecurityUser(
                id, username, "",
                tenant, null, null,
                true, true, true, true,
                authorities, null, null
        );
    }
}
