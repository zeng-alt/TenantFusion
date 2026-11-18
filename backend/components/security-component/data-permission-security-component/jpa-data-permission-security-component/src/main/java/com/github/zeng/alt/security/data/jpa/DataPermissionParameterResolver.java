package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataPermissionQuery;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;
import java.util.function.Supplier;

/**
 * 在 JDBC 参数绑定时读取当前方法快照，不把用户值拼接进 SQL。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class DataPermissionParameterResolver implements Supplier<Object> {
    private final DataPermissionEntityMapping mapping;
    private final String parameter;

    DataPermissionParameterResolver(DataPermissionEntityMapping mapping, String parameter) {
        this.mapping = mapping;
        this.parameter = parameter;
    }

    @Override
    public Object get() {
        if (parameter.equals("internalRead")) {
            return DataPermissionContext.policyRead();
        }
        if (DataPermissionContext.policyRead()) {
            return switch (parameter) {
                case "tenant" -> "";
                case "owner" -> owner("0");
                case "departments" -> List.of(0L);
                default -> false;
            };
        }
        final DataPermissionQuery scope = DataPermissionContext.require(mapping.entity());
        return switch (parameter) {
            case "tenant" -> scope.tenant();
            case "owner" -> owner(scope.userId());
            case "allRows" -> scope.all();
            case "self" -> scope.self();
            case "hasDepartments" -> !scope.departmentIds().isEmpty();
            case "departments" -> scope.departmentIds().isEmpty() ? List.of(0L) : List.copyOf(scope.departmentIds());
            default -> throw new IllegalArgumentException("Unknown data permission parameter: " + parameter);
        };
    }

    private Object owner(String userId) {
        return DefaultConversionService.getSharedInstance().convert(userId, mapping.owner().type());
    }
}
