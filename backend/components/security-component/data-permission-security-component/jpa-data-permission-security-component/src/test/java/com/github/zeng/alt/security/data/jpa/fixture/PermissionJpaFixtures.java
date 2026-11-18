package com.github.zeng.alt.security.data.jpa.fixture;

import com.github.zeng.alt.security.data.DataColumn;
import com.github.zeng.alt.security.data.DataColumnType;
import com.github.zeng.alt.security.data.DataPermission;
import com.github.zeng.alt.security.data.DataResource;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 普通 Repository 和真实 Hibernate 查询夹具。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = PermissionJpaFixtures.class)
@EnableJpaRepositories(basePackageClasses = PermissionJpaFixtures.class, considerNestedRepositories = true)
public class PermissionJpaFixtures {
    @Bean
    public SqlRecorder sqlRecorder() { return new SqlRecorder(); }

    @Bean
    public HibernatePropertiesCustomizer statementRecorder(SqlRecorder recorder) {
        return properties -> properties.put("hibernate.session_factory.statement_inspector", recorder);
    }

    @Bean
    public InterfaceQueries interfaceQueries(Documents documents) { return new InterfaceQueriesImpl(documents); }

    @Bean
    public ClassQueries classQueries(Documents documents) { return new ClassQueries(documents); }

    @Bean
    public Queries queries(Documents documents, EntityManager entityManager) {
        return new Queries(documents, entityManager);
    }

    /**
     * 继承属性及自定义列名映射。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @MappedSuperclass
    public static class Ownership {
        @DataColumn(DataColumnType.TENANT)
        @Column(name = "tenant_key")
        public String tenant;
        @DataColumn(DataColumnType.OWNER)
        @Column(name = "owner_key")
        public Long owner;
        @DataColumn(DataColumnType.DEPARTMENT)
        @Column(name = "department_key")
        public Long department;
    }

    /**
     * 受保护业务实体。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Entity(name = "ScopedDocument")
    @Table(name = "permission_document")
    @DataResource("document")
    public static class Document extends Ownership {
        @Id public Long id;
        public String title;
        public boolean enabled;
    }

    /**
     * 未受保护的父实体，用于验证关联集合不会绕过子实体权限。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Entity(name = "PermissionFolder")
    @Table(name = "permission_folder")
    public static class Folder {
        @Id public Long id;
        @OneToMany @JoinColumn(name = "folder_id")
        public List<Document> documents;
    }

    /**
     * 无权限模板依赖的普通 Spring Data 接口。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public interface Documents extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {
        List<Document> findByEnabledTrueOrderByIdAsc();
        Page<Document> findByEnabledTrue(Pageable pageable);
        long countByEnabledTrue();
        @Query("select d from ScopedDocument d where d.enabled = true order by d.id")
        List<Document> jpql();
        @Query(value = "select d from PermissionFolder f join f.documents d where d.enabled = true order by d.id",
                countQuery = "select count(d) from PermissionFolder f join f.documents d where d.enabled = true")
        Page<Document> joined(Pageable pageable);
    }

    /**
     * 方法注解开启过滤，保持集合、分页、计数原查询写法。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public static class Queries {
        private final Documents documents;
        private final EntityManager entityManager;

        public Queries(Documents documents, EntityManager entityManager) {
            this.documents = documents;
            this.entityManager = entityManager;
        }

        @DataPermission(resource = Document.class, permission = "read")
        public List<Document> list() { return documents.findByEnabledTrueOrderByIdAsc(); }

        @DataPermission(resource = Document.class, permission = "read")
        public Page<Document> page(Pageable pageable) { return documents.findByEnabledTrue(pageable); }

        @DataPermission(resource = Document.class, permission = "read")
        public long count() { return documents.countByEnabledTrue(); }

        @DataPermission(resource = Document.class, permission = "read")
        public List<Document> jpql() { return documents.jpql(); }

        @DataPermission(resource = Document.class, permission = "read")
        public Page<Document> specification(Pageable pageable) {
            return documents.findAll((root, query, cb) -> cb.or(
                    cb.equal(root.get("title"), "own"), cb.equal(root.get("title"), "foreign")), pageable);
        }

        @DataPermission(resource = Document.class, permission = "read")
        public List<Document> querydsl() {
            final var document = new PathBuilder<>(Document.class, "d");
            return new JPAQueryFactory(entityManager).selectFrom(document)
                    .where(document.getBoolean("enabled").isTrue()).orderBy(document.getNumber("id", Long.class).asc())
                    .fetch();
        }

        @DataPermission(resource = Document.class, permission = "read")
        public long nativeCount() {
            return ((Number) entityManager.createNativeQuery("select count(*) from permission_document", Long.class)
                    .getSingleResult()).longValue();
        }

        @DataPermission(resource = Document.class, permission = "read")
        public int associationCount() {
            return entityManager.find(Folder.class, 1L).documents.size();
        }

        @DataPermission(resource = Document.class, permission = "read")
        public int bulkDelete() {
            return entityManager.createQuery("delete from ScopedDocument").executeUpdate();
        }

        @DataPermission(resource = Document.class, permission = "read")
        public java.util.Optional<Document> detail(Long id) { return documents.findById(id); }

        @DataPermission(resource = Document.class, permission = "read")
        public Document cachedDetail(Long id) {
            documents.findById(id).orElseThrow();
            return documents.findById(id).orElseThrow();
        }

        @DataPermission(resource = Document.class, permission = "read")
        public boolean exists(Long id) { return documents.existsById(id); }

        @DataPermission(resource = Document.class, permission = "read")
        public void fail() {
            documents.findAll();
            throw new IllegalArgumentException("business failure");
        }
    }

    /**
     * 接口方法的权限注解。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public interface InterfaceQueries {
        @DataPermission(resource = Document.class, permission = "read")
        long count();
    }

    /**
     * 实现类不重复声明权限。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public static class InterfaceQueriesImpl implements InterfaceQueries {
        private final Documents documents;
        public InterfaceQueriesImpl(Documents documents) { this.documents = documents; }
        @Override public long count() { return documents.countByEnabledTrue(); }
    }

    /**
     * 类级别启用及嵌套调用。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @DataPermission(resource = Document.class, permission = "read")
    public static class ClassQueries {
        private final Documents documents;
        public ClassQueries(Documents documents) { this.documents = documents; }
        public long count() { return documents.countByEnabledTrue(); }
        public long nested(InterfaceQueries other) {
            final long count = other.count();
            return count + documents.countByEnabledTrue();
        }
    }

    /**
     * 记录 Hibernate 实际提交的 SQL，同时验证组件保留宿主已有 StatementInspector。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public static class SqlRecorder implements org.hibernate.resource.jdbc.spi.StatementInspector {
        private static final long serialVersionUID = 1L;
        public final List<String> statements = new CopyOnWriteArrayList<>();
        @Override public String inspect(String sql) { statements.add(sql); return sql; }
    }
}
