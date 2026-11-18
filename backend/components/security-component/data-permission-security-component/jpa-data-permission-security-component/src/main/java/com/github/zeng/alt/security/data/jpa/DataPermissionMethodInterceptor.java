package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataPermission;
import com.github.zeng.alt.security.data.DataPermissionTemplate;
import com.github.zeng.alt.security.data.DataResource;
import jakarta.persistence.EntityManager;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.stream.BaseStream;

/**
 * 在有界读事务中启用权限快照；分页内容和 count 共享一次授权。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class DataPermissionMethodInterceptor implements MethodInterceptor {
    private final EntityManager entityManager;
    private final DataPermissionTemplate permissions;
    private final TransactionTemplate transaction;

    DataPermissionMethodInterceptor(
            EntityManager entityManager, DataPermissionTemplate permissions, PlatformTransactionManager transactions
    ) {
        this.entityManager = entityManager;
        this.permissions = permissions;
        transaction = new TransactionTemplate(transactions);
        transaction.setReadOnly(true);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final Class<?> target = invocation.getThis().getClass();
        final DataPermission annotation = DataPermissionMethodHelper.resolve(invocation.getMethod(), target).get();
        final DataResource resource = annotation.resource().getAnnotation(DataResource.class);
        if (resource == null || annotation.permission().isBlank()
                || BaseStream.class.isAssignableFrom(invocation.getMethod().getReturnType())) {
            throw new IllegalArgumentException("Invalid @DataPermission resource, permission or streaming return type");
        }
        entityManager.getMetamodel().entity(annotation.resource());
        final var scope = DataPermissionContext.readPolicy(() -> permissions.resource(resource.value())
                .permission(annotation.permission()).get())
                .getOrElseThrow(AccessDeniedException::new);
        if (scope.tenant() == null || scope.tenant().isBlank() || scope.userId() == null || scope.userId().isBlank()) {
            throw new AccessDeniedException("Data permission subject requires a tenant and user ID");
        }
        try (final var ignored = DataPermissionContext.open(annotation.resource(), scope)) {
            try {
                return transaction.execute(status -> {
                    DataPermissionLoadListener.validateManaged(entityManager);
                    return proceed(invocation);
                });
            } catch (InvocationFailure failure) {
                throw failure.getCause();
            }
        }
    }

    private Object proceed(MethodInvocation invocation) {
        try {
            final Object returned = invocation.proceed();
            if (returned instanceof BaseStream<?, ?> stream) {
                stream.close();
                throw new IllegalArgumentException("Permission-scoped streams must be consumed inside the method");
            }
            return returned;
        } catch (RuntimeException | Error failure) {
            throw failure;
        } catch (Throwable failure) {
            throw new InvocationFailure(failure);
        }
    }

    private static final class InvocationFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        InvocationFailure(Throwable cause) { super(cause); }
    }
}
