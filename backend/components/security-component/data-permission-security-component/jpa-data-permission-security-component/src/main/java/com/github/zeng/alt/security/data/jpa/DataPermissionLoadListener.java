package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataColumn;
import com.github.zeng.alt.security.data.DataColumnType;
import com.github.zeng.alt.security.data.DataPermissionQuery;
import com.github.zeng.alt.security.data.DataResource;
import jakarta.persistence.EntityManager;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.InitializeCollectionEvent;
import org.hibernate.event.spi.InitializeCollectionEventListener;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.EntityType;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.AnnotatedElement;
import java.util.Objects;

/**
 * 在主键读取和已有持久化上下文命中时检查权限，避免绕过 SQL 过滤。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class DataPermissionLoadListener implements Integrator, LoadEventListener, InitializeCollectionEventListener {
    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrap, SessionFactoryImplementor factory) {
        factory.getServiceRegistry().getService(EventListenerRegistry.class).prependListeners(EventType.LOAD, this);
        factory.getServiceRegistry().getService(EventListenerRegistry.class)
                .prependListeners(EventType.INIT_COLLECTION, this);
    }

    @Override
    public void onInitializeCollection(InitializeCollectionEvent event) {
        if (DataPermissionContext.policyRead()) {
            return;
        }
        final var mappings = event.getSession().getFactory().getMappingMetamodel();
        final var element = mappings.getCollectionDescriptor(event.getCollection().getRole()).getElementType();
        if (element instanceof EntityType entityType && mappings.getEntityDescriptor(entityType.getAssociatedEntityName())
                .getMappedClass().isAnnotationPresent(DataResource.class)) {
            throw new AccessDeniedException("Collection associations of protected resources require an explicit query");
        }
    }

    @Override
    public void onLoad(LoadEvent event, LoadType type) {
        final var session = event.getSession();
        final var persister = session.getFactory().getMappingMetamodel().getEntityDescriptor(event.getEntityClassName());
        if (!persister.getMappedClass().isAnnotationPresent(DataResource.class) || DataPermissionContext.policyRead()) {
            return;
        }
        DataPermissionContext.require(persister.getMappedClass());
        final Object cached = session.getPersistenceContextInternal()
                .getEntity(session.generateEntityKey(event.getEntityId(), persister));
        if (cached != null) {
            validate(cached, persister, session);
        }
    }

    static void validateManaged(EntityManager entityManager) {
        final var session = entityManager.unwrap(SharedSessionContractImplementor.class);
        session.getPersistenceContextInternal().managedEntitiesIterator().forEachRemaining(entity -> {
            final var entry = session.getPersistenceContextInternal().getEntry(entity);
            if (entry.getPersister().getMappedClass().isAnnotationPresent(DataResource.class)) {
                validate(entity, entry.getPersister(), session);
            }
        });
    }

    private static void validate(Object entity, EntityPersister persister, SharedSessionContractImplementor session) {
        final DataPermissionQuery scope = DataPermissionContext.require(persister.getMappedClass());
        final var properties = session.getFactory().getJpaMetamodel().entity(persister.getMappedClass()).getAttributes();
        boolean tenant = false;
        boolean visible = scope.all();
        for (var attribute : properties) {
            final var member = (AnnotatedElement) attribute.getJavaMember();
            final var column = member.getAnnotation(DataColumn.class);
            if (column == null && !member.isAnnotationPresent(TenantId.class)) {
                continue;
            }
            final DataColumnType role = column == null ? DataColumnType.TENANT : column.value();
            final Object value = attribute.isCollection() ? null : attribute.getName().equals(persister.getIdentifierPropertyName())
                    ? persister.getIdentifier(entity, session) : persister.getPropertyValue(entity, attribute.getName());
            switch (role) {
                case TENANT -> tenant = Objects.equals(value, scope.tenant());
                case OWNER -> visible |= scope.self() && Objects.equals(String.valueOf(value), scope.userId());
                case DEPARTMENT -> visible |= scope.departmentIds().contains(value);
            }
        }
        if (!tenant || !visible) {
            throw new AccessDeniedException("Existing persistence context contains data outside the permission scope");
        }
    }
}
