package com.github.zeng.alt.tenant.row;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.TenantId;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Hibernate 6.6 的 {@code @TenantId} 判别器语义，尤其是
 * {@code CurrentTenantIdentifierResolver#isRoot} 为 true 时的行为。
 * <p>
 * 这一行为决定了混合部署的正确性：走独立库 / 独立 schema 的租户会被
 * {@code DefaultTenantDiscriminatorPolicy} 判为 root 以跳过判别条件，
 * 如果此时插入不再回填判别列，那么该租户日后降级回共享模式，数据就会失去归属。
 * 官方文档未明确这一点，故用实测锁定。
 * <p>
 * 刻意不引入 Spring：直接用 Hibernate 引导，把被测语义与容器装配隔离开。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
class HibernateTenantDiscriminatorTest {

    /**
     * 当前租户，由各测试用例直接改写。
     * <p>各用例共享同一个 {@code SessionFactory} 与同一个内存库，
     * 因此每个用例必须使用互不重叠的租户名，否则计数断言会被别的用例写入的数据污染。
     */
    private static String currentTenant = "ord-a";
    /** isRoot 的返回值，由各测试用例直接改写 */
    private static boolean rootFlag = false;

    private static SessionFactory sessionFactory;

    @Entity(name = "RowDoc")
    @Table(name = "row_doc")
    public static class RowDoc {
        @Id
        public Long id;
        public String title;
        /**
         * 显式指定列名：纯 Hibernate 引导下默认命名策略保留驼峰，
         * 不像 Spring Boot 那样转成蛇形，否则物理列会是 {@code tenantBy}。
         */
        @TenantId
        @Column(name = "tenant_by")
        public String tenantBy;
    }

    @BeforeAll
    static void bootstrap() {
        Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        configuration.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:tenant_row_test;DB_CLOSE_DELAY=-1");
        configuration.setProperty("hibernate.connection.username", "sa");
        configuration.setProperty("hibernate.connection.password", "");
        configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        configuration.setProperty("hibernate.show_sql", "false");
        configuration.addAnnotatedClass(RowDoc.class);
        configuration.getProperties().put(
                MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                new CurrentTenantIdentifierResolver<String>() {
                    @Override
                    public String resolveCurrentTenantIdentifier() {
                        return currentTenant;
                    }

                    @Override
                    public boolean validateExistingCurrentSessions() {
                        return false;
                    }

                    @Override
                    public boolean isRoot(String tenantId) {
                        return rootFlag;
                    }
                });
        sessionFactory = configuration.buildSessionFactory();
    }

    @AfterAll
    static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    @DisplayName("普通租户：插入自动回填判别列，查询自动追加判别条件")
    void ordinaryTenantIsStampedAndFiltered() {
        currentTenant = "ord-a";
        rootFlag = false;
        save(1L, "ord-a 的文档");

        assertThat(rawTenantColumn(1L)).isEqualTo("ord-a");

        // 切到另一个租户后应当看不见 ord-a 的数据
        currentTenant = "ord-b";
        assertThat(findById(1L)).isNull();
        assertThat(countViaHql()).isZero();

        // 切回来又可见
        currentTenant = "ord-a";
        assertThat(findById(1L)).isNotNull();
        assertThat(countViaHql()).isEqualTo(1);
    }

    @Test
    @DisplayName("isRoot=true：查询绕过判别条件，可跨租户读取")
    void rootTenantBypassesDiscriminator() {
        currentTenant = "root-a";
        rootFlag = false;
        save(10L, "root-a");
        currentTenant = "root-b";
        save(11L, "root-b");

        currentTenant = "root-observer";
        rootFlag = false;
        long scoped = countViaHql();

        rootFlag = true;
        long unscoped = countViaHql();

        assertThat(scoped).isZero();
        assertThat(unscoped).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("关键行为：isRoot=true 时插入是否仍回填判别列")
    void rootTenantInsertStampingBehaviour() {
        currentTenant = "gamma";
        rootFlag = true;
        save(20L, "root 模式下插入");

        String stamped = rawTenantColumn(20L);
        // 断言当前实测结论；若将来升级 Hibernate 后此处失败，
        // 说明该语义发生变化，DefaultTenantDiscriminatorPolicy 需随之调整
        assertThat(stamped)
                .as("isRoot=true 时 Hibernate 对 @TenantId 列的回填行为")
                .isEqualTo("gamma");
    }

    private void save(Long id, String title) {
        sessionFactory.inTransaction(session -> {
            RowDoc doc = new RowDoc();
            doc.id = id;
            doc.title = title;
            session.persist(doc);
        });
    }

    private RowDoc findById(Long id) {
        return sessionFactory.fromTransaction(session -> session.find(RowDoc.class, id));
    }

    private long countViaHql() {
        return sessionFactory.fromTransaction(session ->
                session.createQuery("select count(d) from RowDoc d", Long.class)
                        .getSingleResult());
    }

    /** 绕开 Hibernate 直读物理列，避免判别条件干扰断言 */
    private String rawTenantColumn(Long id) {
        return sessionFactory.fromTransaction(session -> {
            List<?> rows = session
                    .createNativeQuery("select tenant_by from row_doc where id = :id", Object.class)
                    .setParameter("id", id)
                    .getResultList();
            return rows.isEmpty() ? null : (String) rows.get(0);
        });
    }
}
