package com.github.zeng.alt.security.abac.method;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import org.springframework.util.Assert;

/**
 * 让事务 Advisor 包住 ABAC，后置拒绝发生在事务提交之前。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class AbacTransactionAdvisorHandler implements BeanPostProcessor {
    private final ObjectProvider<AbacMethodProperties> properties;

    public AbacTransactionAdvisorHandler(ObjectProvider<AbacMethodProperties> properties) {
        this.properties = properties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String name) {
        if (bean instanceof BeanFactoryTransactionAttributeSourceAdvisor transaction) {
            final int order = properties.getObject().getOrder();
            Assert.state(order > Integer.MIN_VALUE, "ABAC order 必须允许事务 Advisor 排在其之前");
            if (transaction.getOrder() >= order) { transaction.setOrder(order - 1); }
        }
        return bean;
    }
}
