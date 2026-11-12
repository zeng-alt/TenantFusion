package com.github.zeng.alt.security.data.core;

import com.github.zeng.alt.security.data.DataPermissionDepartmentResolver;
import com.github.zeng.alt.security.data.DataPermissionPolicyLoader;
import com.github.zeng.alt.security.data.DataPermissionSubject;
import com.github.zeng.alt.security.data.DataPermissionSubjectResolver;
import com.github.zeng.alt.security.data.DataPermissionTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 数据权限公共装配，缺少宿主 SPI 时启动失败。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration
@EnableConfigurationProperties(DataPermissionProperties.class)
public class DataPermissionAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(DataPermissionTemplate.class)
    public DataPermissionTemplate dataPermissionTemplate(
            DataPermissionSubjectResolver subjects,
            DataPermissionPolicyLoader policies,
            DataPermissionDepartmentResolver departments,
            DataPermissionProperties properties,
            ObjectProvider<DataPermissionCustomizer> customizers
    ) {
        customizers.orderedStream().forEach(customizer -> customizer.customize(properties));
        return new DataPermissionTemplateImpl(subjects, policies, departments, properties);
    }
}
