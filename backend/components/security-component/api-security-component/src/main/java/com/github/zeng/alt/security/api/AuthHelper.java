package com.github.zeng.alt.security.api;

/**
 * @author zengJiaJun
 * @since 2026年07月23日
 * @version 1.0
 */
public interface AuthHelper {

    /** 当前身份具备管理员及以上操作权限，包含没有管理员角色的内置超级用户。 */
    boolean hasAdmin();

    /** 匹配受保护的内置管理员角色，与当前操作者及管理员豁免开关无关。 */
    boolean hasAdminRole(String code);

    /** 匹配受保护的内置超级用户，与当前操作者及管理员豁免开关无关。 */
    boolean hasSuperUser(String id);

    default boolean hasSuperUser(Long id) {
        return hasSuperUser(String.valueOf(id));
    }

    /** 检查当前激活角色；未选择角色时检查已授予角色，角色码不添加 ROLE_ 前缀。 */
    boolean hasRole(String role);

    /** 当前角色满足任意一个角色码。 */
    boolean hasAnyRole(String... roles);

    /** 精确检查 GrantedAuthority 或扩展权限源提供的权限 code。 */
    boolean hasAuthority(String authority);

    /** 满足任意一个权限 code。 */
    boolean hasAnyAuthority(String... authorities);
}
