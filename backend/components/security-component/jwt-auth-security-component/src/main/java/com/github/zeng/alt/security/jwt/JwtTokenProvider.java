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
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@CommonsLog
public class JwtTokenProvider {
    private static final String CLAIM_ID = "id";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_CURRENT_ROLE = "currentRole";
    private static final String CLAIM_TENANT = "tenant";
    static final String CACHE_KEY_PREFIX = "jwt:token:";
    static final String REFRESH_CACHE_KEY_PREFIX = "jwt:refresh:";
    static final String REFRESH_USER_CACHE_KEY_PREFIX = "jwt:refresh:user:";

    private final JwtProperties jwtProperties;
    private final JwtEncoder accessEncoder;
    private final JwtDecoder accessDecoder;
    private final JwtEncoder refreshEncoder;
    private final JwtDecoder refreshDecoder;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        byte[] accessKeyBytes = Base64.getDecoder().decode(jwtProperties.getBase64Secret());
        SecretKey accessSecretKey = new SecretKeySpec(accessKeyBytes, "HmacSHA256");
        this.accessEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(accessSecretKey));
        this.accessDecoder = NimbusJwtDecoder.withSecretKey(accessSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        String refreshSecretStr = jwtProperties.getBase64SecretRefresh();
        if (!StringUtils.hasText(refreshSecretStr)) {
            refreshSecretStr = jwtProperties.getBase64Secret();
        }
        byte[] refreshKeyBytes = Base64.getDecoder().decode(refreshSecretStr);
        SecretKey refreshSecretKey = new SecretKeySpec(refreshKeyBytes, "HmacSHA512");
        this.refreshEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(refreshSecretKey));
        this.refreshDecoder = NimbusJwtDecoder.withSecretKey(refreshSecretKey)
                .macAlgorithm(MacAlgorithm.HS512)
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
                .claim(CLAIM_CURRENT_ROLE, user.getCurrentRole().getAuthority())
                .claim(CLAIM_TENANT, user.getTenant())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(jwtProperties.getExpiration()))
                .build();

        return accessEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    public String createRefreshToken(SecurityUser user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .subject(user.getId())
                .claim(CLAIM_ID, user.getId())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(jwtProperties.getRememberMeExpiration()))
                .build();

        return refreshEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS512).build(), claims))
                .getTokenValue();
    }

    public String getTokenId(String token) {
        Jwt decode = accessDecoder.decode(token);
        return getTokenId(decode);
    }

    public String getTokenId(Jwt jwt) {
        String id = jwt.getId();
        if (StringUtils.hasText(id)) {
            return jwt.getClaim(CLAIM_ID) + ":" + jwt.getId();
        }
        return jwt.getClaim(CLAIM_ID);
    }

    public String getCacheKey(String token) {
        try {
            String jti = getTokenId(token);
            return jti == null ? null : CACHE_KEY_PREFIX + jti;
        } catch (JwtException e) {
            return null;
        }
    }

    public String getCacheKey(Jwt jwt) {
        String jti = getTokenId(jwt);
        return jti == null ? null : CACHE_KEY_PREFIX + jti;
    }

    public long getExpirationSeconds() {
        return jwtProperties.getExpiration();
    }

    public enum TokenValidationResult {
        VALID, EXPIRED, INVALID
    }

    public TokenValidationResult validateTokenWithResult(String token) {
        try {
            accessDecoder.decode(token);
            return TokenValidationResult.VALID;
        } catch (JwtValidationException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("expired")) {
                return TokenValidationResult.EXPIRED;
            }
            log.warn(LogMessage.format("JWT token 验证失败: %s", e.getMessage()));
            return TokenValidationResult.INVALID;
        } catch (JwtException e) {
            log.warn(LogMessage.format("JWT token 无效: %s", e.getMessage()));
            return TokenValidationResult.INVALID;
        }
    }

    public boolean validateToken(String token) {
        try {
            accessDecoder.decode(token);
            return true;
        } catch (JwtValidationException e) {
            log.warn(LogMessage.format("JWT token 验证失败: %s", e.getMessage()));
        } catch (JwtException e) {
            log.warn(LogMessage.format("JWT token 无效: %s", e.getMessage()));
        }
        return false;
    }

    public boolean validateRefreshToken(String token) {
        try {
            refreshDecoder.decode(token);
            return true;
        } catch (JwtException e) {
            log.warn(LogMessage.format("Refresh JWT token 无效: %s", e.getMessage()));
            return false;
        }
    }

    public Jwt getJwt(String token) {
        return accessDecoder.decode(token);
    }

    public Jwt getRefreshJwt(String token) {
        return refreshDecoder.decode(token);
    }

    public String getRefreshCacheKey(String token) {
        try {
            Jwt decode = refreshDecoder.decode(token);
            String id = decode.getId();
            if (StringUtils.hasText(id)) {
                return REFRESH_CACHE_KEY_PREFIX + decode.getClaim(CLAIM_ID) + ":" + id;
            }
            return REFRESH_CACHE_KEY_PREFIX + decode.getClaim(CLAIM_ID);
        } catch (JwtException e) {
            return null;
        }
    }

    public String getUserCacheKey(String userId) {
        return REFRESH_USER_CACHE_KEY_PREFIX + userId;
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
        } catch (ParseException e) {
            return null;
        }
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
                Optional.ofNullable(jwt.getClaimAsString(CLAIM_CURRENT_ROLE)).map(SimpleGrantedAuthority::new).orElse(null),
                null
        );
    }
}
