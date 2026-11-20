package com.github.zeng.alt.security.core;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import io.vavr.control.Either;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 指定的登录身份可切换到启用中的租户。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class TenantSwitchTemplate {
    private final SecurityProperties properties;
    private final ObjectProvider<TenantMetadataProvider> providers;

    /** 判断登录身份是否可以选择其他租户。 */
    public boolean canSwitch(SecurityUser user) {
        final SecurityProperties.AdminRole admin = properties.getAdmin();
        return user != null && user.isEnabled() && Boolean.TRUE.equals(admin.getEnabled())
                && StringUtils.hasText(admin.getId()) && StringUtils.hasText(admin.getTenant())
                && Objects.equals(admin.getId(), user.getId()) && Objects.equals(admin.getTenant(), user.getTenant());
    }

    /** 校验当前身份与目标租户，并返回有效租户标识。 */
    public Either<String, String> select(SecurityUser user, String tenant) {
        if (!canSwitch(user)) {
            return Either.left("无权切换租户");
        }
        return Option.of(tenant).filter(StringUtils::hasText)
                .flatMap(id -> Option.of(providers.getIfAvailable())
                        .flatMap(provider -> Option.ofOptional(provider.findCurrentById(id))))
                .filter(TenantMetadata::enabled).map(TenantMetadata::tenantId)
                .toEither("目标租户不存在或已停用");
    }

    /** 获取当前身份可选择的租户。 */
    public Either<String, List<TenantMetadata>> list(SecurityUser user) {
        if (!canSwitch(user)) {
            return Either.left("无权切换租户");
        }
        return Option.of(providers.getIfAvailable()).map(TenantMetadataProvider::findAll)
                .toEither("租户服务不可用");
    }
}
