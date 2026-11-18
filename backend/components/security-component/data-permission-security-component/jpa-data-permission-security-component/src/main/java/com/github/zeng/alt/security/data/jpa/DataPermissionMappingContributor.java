package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataResource;
import org.hibernate.MappingException;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ProvidedInstanceManagedBeanImpl;
import org.hibernate.type.StandardBasicTypes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 在 Hibernate 构建持久化元数据时，将资源注解转换为参数化过滤器。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class DataPermissionMappingContributor implements AdditionalMappingContributor {
    static final String PREFIX = "alt_data_permission_";

    @Override
    public void contribute(
            AdditionalMappingContributions contributions,
            InFlightMetadataCollector metadata,
            ResourceStreamLocator resources,
            MetadataBuildingContext context
    ) {
        metadata.getEntityBindings().stream()
                .filter(binding -> binding.getMappedClass().isAnnotationPresent(DataResource.class))
                .forEach(binding -> register(binding, metadata, context));
        // 集合的元素映射在第二阶段才完整；实体 Filter 不会自动成为关联 JOIN 的集合 Filter。
        metadata.addSecondPass(bindings -> metadata.getCollectionBindings().forEach(collection ->
                registerCollection(collection, bindings, metadata)));
    }

    private void registerCollection(
            Collection collection, Map<String, PersistentClass> bindings, InFlightMetadataCollector metadata
    ) {
        final String entityName;
        if (collection.getElement() instanceof OneToMany element) {
            entityName = element.getReferencedEntityName();
        } else if (collection.getElement() instanceof ManyToOne element) {
            entityName = element.getReferencedEntityName();
        } else {
            return;
        }
        final PersistentClass target = bindings.get(entityName);
        if (target == null || !target.getMappedClass().isAnnotationPresent(DataResource.class)) {
            return;
        }
        final String filter = PREFIX + target.getJpaEntityName();
        final String condition = metadata.getFilterDefinition(filter).getDefaultFilterCondition();
        final Map<String, String> aliases = Map.of("alias", target.getTable().getName());
        if (collection.getElement() instanceof OneToMany) {
            collection.addFilter(filter, condition, false, aliases, Map.of());
        } else {
            // Hibernate 7.4 的多对多 FilterHelper 没有 table-to-entity 映射，显式别名路径会空指针。
            // 条件仅含组件生成的单表列，使用 Hibernate 自带的自动别名路径。
            collection.addManyToManyFilter(filter, condition.replace("{alias}.", ""), true, Map.of(), Map.of());
        }
    }

    private void register(PersistentClass binding, InFlightMetadataCollector metadata, MetadataBuildingContext context) {
        final var settings = context.getBootstrapContext().getServiceRegistry()
                .getService(ConfigurationService.class).getSettings();
        // Hibernate 7.4 的查询缓存键仅包含显式设置的 Filter 参数，不含 Supplier 延迟参数。
        if (Boolean.parseBoolean(String.valueOf(settings.get("hibernate.cache.use_query_cache")))
                || binding.getCacheConcurrencyStrategy() != null) {
            throw new MappingException("Data permission resources cannot use query or entity second-level caches");
        }
        final var mapping = DataPermissionEntityMapping.from(binding, metadata.getDatabase().getDialect());
        final String name = PREFIX + binding.getJpaEntityName();
        final String condition = "(:internalRead = true or ({alias}." + mapping.tenant().column() + " = :tenant and "
                + "(:allRows = true or (:self = true and {alias}." + mapping.owner().column() + " = :owner) or "
                + "(:hasDepartments = true and {alias}." + mapping.department().column() + " in (:departments)))))";
        final var types = context.getBootstrapContext().getTypeConfiguration().getBasicTypeRegistry();
        final Map<String, JdbcMapping> parameters = new LinkedHashMap<>();
        parameters.put("internalRead", types.resolve(StandardBasicTypes.BOOLEAN));
        parameters.put("tenant", types.resolve(StandardBasicTypes.STRING));
        parameters.put("owner", types.getRegisteredType(mapping.owner().type()));
        parameters.put("allRows", types.resolve(StandardBasicTypes.BOOLEAN));
        parameters.put("self", types.resolve(StandardBasicTypes.BOOLEAN));
        parameters.put("hasDepartments", types.resolve(StandardBasicTypes.BOOLEAN));
        parameters.put("departments", types.resolve(StandardBasicTypes.LONG));
        final Map<String, ManagedBean<? extends Supplier<?>>> resolvers = new LinkedHashMap<>();
        parameters.keySet().forEach(parameter -> resolvers.put(parameter,
                new ProvidedInstanceManagedBeanImpl<>(new DataPermissionParameterResolver(mapping, parameter))));
        metadata.addFilterDefinition(new FilterDefinition(name, condition, true, true, parameters, resolvers));
        binding.addFilter(name, condition, false, Map.of("alias", binding.getTable().getName()), Map.of());
    }
}
