package com.github.zeng.alt.camunda.identity.api;

import java.util.List;
import java.util.Optional;

/**
 * Camunda 用户/组 SPI。
 * <p>
 * 由具体存储层实现（如 JPA 读取 admin 的 main_user/main_role），
 * 实现者负责完成密码匹配、启用/删除状态过滤等业务规则。
 */
public interface CamundaUserGroupSource {

    /**
     * 按用户名查找用户。
     */
    Optional<CamundaIdentityUser> findByUsername(String username);

    /**
     * 校验用户原始密码是否与存储值匹配。
     */
    boolean matchesPassword(String username, String rawPassword);

    /**
     * 查找用户所属的全部组。
     */
    List<CamundaIdentityGroup> findGroupsByUsername(String username);

    /**
     * 查找属于某个组的所有用户。
     */
    List<CamundaIdentityUser> findUsersByGroupCode(String code);

    /**
     * 按组编码查找组。
     */
    Optional<CamundaIdentityGroup> findByGroupCode(String code);

    /**
     * 查找全部组。
     */
    List<CamundaIdentityGroup> findAllGroups();
}
