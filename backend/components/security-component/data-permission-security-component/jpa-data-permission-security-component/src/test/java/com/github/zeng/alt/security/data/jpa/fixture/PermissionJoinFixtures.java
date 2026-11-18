package com.github.zeng.alt.security.data.jpa.fixture;

import com.github.zeng.alt.security.data.DataPermission;
import com.github.zeng.alt.security.data.jpa.fixture.PermissionJpaFixtures.Document;
import com.github.zeng.alt.security.data.jpa.fixture.PermissionJpaFixtures.Documents;
import com.github.zeng.alt.security.data.jpa.fixture.PermissionJpaFixtures.Folder;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

/**
 * 验证显式实体 JOIN、关联 JOIN 和集合 FETCH JOIN 的真实查询。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Configuration(proxyBeanMethods = false)
public class PermissionJoinFixtures {
    @Bean
    public JoinQueries joinQueries(EntityManager entityManager, Documents documents) {
        return new JoinQueries(entityManager, documents);
    }

    /**
     * 使用连接表关联受保护实体。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Entity(name = "PermissionGroup")
    @Table(name = "permission_group")
    public static class Group {
        @Id public Long id;
        @ManyToMany
        @JoinTable(name = "permission_group_document", joinColumns = @JoinColumn(name = "group_id"),
                inverseJoinColumns = @JoinColumn(name = "document_id"))
        public Set<Document> documents;
    }

    /**
     * 普通方法注解，查询代码不调用权限模板。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @DataPermission(resource = Document.class, permission = "read")
    public static class JoinQueries {
        private final EntityManager entityManager;
        private final Documents documents;

        public JoinQueries(EntityManager entityManager, Documents documents) {
            this.entityManager = entityManager;
            this.documents = documents;
        }

        public List<Long> explicit() {
            return entityManager.createQuery("""
                    select d.id from ScopedDocument d join PermissionFolder f on f.id = 1
                    where d.enabled = true order by d.id
                    """, Long.class).getResultList();
        }

        public List<Long> association() {
            return entityManager.createQuery("""
                    select d.id from PermissionFolder f join f.documents d
                    where d.enabled = true order by d.id
                    """, Long.class).getResultList();
        }

        public List<Object[]> left() {
            return entityManager.createQuery("""
                    select f.id, d.id from PermissionFolder f left join f.documents d on d.enabled = true
                    order by f.id, d.id
                    """, Object[].class).getResultList();
        }

        public List<Folder> fetch() {
            return entityManager.createQuery("""
                    select distinct f from PermissionFolder f left join fetch f.documents order by f.id
                    """, Folder.class).getResultList();
        }

        public Page<Document> page(Pageable pageable) { return documents.joined(pageable); }

        public List<Long> manyToMany() {
            return entityManager.createQuery("""
                    select distinct d.id from PermissionGroup g join g.documents d
                    where d.enabled = true order by d.id
                    """, Long.class).getResultList();
        }
    }
}
