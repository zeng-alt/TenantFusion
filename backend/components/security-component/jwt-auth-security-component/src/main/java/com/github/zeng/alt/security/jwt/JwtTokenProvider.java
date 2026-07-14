package com.github.zeng.alt.security.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.github.zeng.alt.security.api.SecurityUser;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.log.LogMessage;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
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
@CommonsLog
public class JwtTokenProvider {

    private static final String CLAIM_ID = "id";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TENANT = "tenant";
    private static final String CLAIM_PURPOSE = "purpose";
    private static final String CLAIM_PURPOSE_REFRESH = "refresh";
    static final String CACHE_KEY_PREFIX = "jwt:token:";
    static final String REFRESH_CACHE_KEY_PREFIX = "jwt:refresh:";

    private final long expirationSeconds;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public JwtTokenProvider(String base64Secret, long expirationSeconds) {

        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);

        SecretKey secretKey =
                new SecretKeySpec(keyBytes, "HmacSHA256");
        this.expirationSeconds = expirationSeconds;
        this.jwtEncoder =
                new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    public String createToken(SecurityUser user) {

        Instant now = Instant.now();

        List<String> roles = user.getRoles().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .subject(user.getUsername())
                .claim(CLAIM_ID, user.getId())
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TENANT, user.getTenant())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    public String getTokenId(String token) {
        Jwt decode = jwtDecoder.decode(token);
        String id = decode.getId();
        if (StringUtils.hasText(id)) {
            return decode.getClaim(CLAIM_ID) + ":" + decode.getId();
        }
        return decode.getClaim(CLAIM_ID);
    }

    public String getCacheKey(String token) {

        String jti = getTokenId(token);

        return jti == null ? null : CACHE_KEY_PREFIX + jti;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public String createRefreshToken(SecurityUser user, long refreshExpirationSeconds) {

        Instant now = Instant.now();

        List<String> roles = user.getRoles().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .subject(user.getUsername())
                .claim(CLAIM_ID, user.getId())
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TENANT, user.getTenant())
                .claim(CLAIM_PURPOSE, CLAIM_PURPOSE_REFRESH)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(refreshExpirationSeconds))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    public enum TokenValidationResult {
        VALID, EXPIRED, INVALID
    }

    public TokenValidationResult validateTokenWithResult(String token) {

        try {
            jwtDecoder.decode(token);
            return TokenValidationResult.VALID;
        }
        catch (JwtValidationException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("expired")) {
                return TokenValidationResult.EXPIRED;
            }
            log.warn(LogMessage.format("JWT token 验证失败: %s", e.getMessage()));
            return TokenValidationResult.INVALID;
        }
        catch (JwtException e) {
            log.warn(LogMessage.format("JWT token 无效: %s", e.getMessage()));
            return TokenValidationResult.INVALID;
        }
    }

    public boolean validateToken(String token) {

        try {
            jwtDecoder.decode(token);
            return true;
        }
        catch (JwtValidationException e) {
            log.warn(LogMessage.format("JWT token 验证失败: %s", e.getMessage()));
        }
        catch (JwtException e) {
            log.warn(LogMessage.format("JWT token 无效: %s", e.getMessage()));
        }

        return false;
    }

    public Jwt getClaims(String token) {
        return jwtDecoder.decode(token);
    }

    public String getRefreshCacheKey(String token) {
        try {
            Jwt decode = jwtDecoder.decode(token);
            String id = decode.getId();
            if (StringUtils.hasText(id)) {
                return REFRESH_CACHE_KEY_PREFIX + decode.getClaim(CLAIM_ID) + ":" + id;
            }
            return REFRESH_CACHE_KEY_PREFIX + decode.getClaim(CLAIM_ID);
        }
        catch (JwtException e) {
            return null;
        }
    }

    public String getCacheKeyFromExpiredToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            String userId = signedJWT.getJWTClaimsSet().getStringClaim(CLAIM_ID);
            if (StringUtils.hasText(jti)) {
                return CACHE_KEY_PREFIX + userId + ":" + jti;
            }
            return CACHE_KEY_PREFIX + userId;
        }
        catch (ParseException e) {
            return null;
        }
    }

    public boolean isRefreshToken(Jwt jwt) {
        return CLAIM_PURPOSE_REFRESH.equals(jwt.getClaimAsString(CLAIM_PURPOSE));
    }

    public SecurityUser getUserFromClaims(Jwt jwt) {

        List<String> roles = jwt.getClaimAsStringList(CLAIM_ROLES);

        Set<GrantedAuthority> authorities =
                roles == null
                        ? Set.of()
                        : roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toSet());

        return new SecurityUser(
                jwt.getClaimAsString(CLAIM_ID),
                jwt.getSubject(),
                "",
                jwt.getClaimAsString(CLAIM_TENANT),
                null,
                null,
                true,
                true,
                true,
                true,
                authorities,
                CollectionUtils.isEmpty(authorities) ? null : authorities.iterator().next(),
                null
        );
    }
}
