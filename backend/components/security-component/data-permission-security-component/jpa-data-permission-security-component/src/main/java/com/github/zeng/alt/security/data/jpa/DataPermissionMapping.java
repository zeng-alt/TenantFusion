package com.github.zeng.alt.security.data.jpa;




/**
 * 受保护资源的不可变 JPA 字段映射。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
record DataPermissionMapping(String resource, Class<?> entity, String tenant, String owner, String department) {
}
