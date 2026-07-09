package com.github.zeng.alt.security.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.github.zeng.alt.security.api.SecurityUser;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.log.LogMessage;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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
    static final String CACHE_KEY_PREFIX = "jwt:token:";

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

        return jwtEncoder.encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }

    public String getTokenId(String token) {
        return jwtDecoder.decode(token).getId();
    }

    public String getCacheKey(String token) {

        String jti = getTokenId(token);

        return jti == null ? null : CACHE_KEY_PREFIX + jti;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
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
                null,
                null
        );
    }
}
