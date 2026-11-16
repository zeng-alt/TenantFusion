package com.github.zeng.alt.config.client;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.ConfigSourceTemplate;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 远程协议、旧缓存校准和失败后重试测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ConfigSynchronizationTest {
    @TempDir
    Path directory;

    @Test
    void readsTypedSnapshotsAndRejectsBusinessFailureResponses() {
        final var properties = properties();
        final var fetch = new RestTemplate();
        final var server = MockRestServiceServer.bindTo(fetch).build();
        final var remote = new ConfigRemoteTemplate(properties, new JsonMapper(), fetch, new RestTemplate());
        final String uri = "http://localhost:8085/api/config/fetch?appCode=shop&listenTimeoutMs=30000";
        server.expect(requestTo(uri)).andRespond(withSuccess(
            """
                    {"status":200,"data":[{"configId":"4000000000","dataId":"enabled","group":"payment",
                    "content":"true","format":"text","version":3,"appCode":"shop"}]}
                    """,
            MediaType.APPLICATION_JSON
        ));
        server.expect(requestTo(uri)).andRespond(withSuccess("{\"status\":600,\"data\":[]}", MediaType.APPLICATION_JSON));

        assertThat(remote.getAll().getFirst().getConfigId()).isEqualTo(4000000000L);
        assertThatThrownBy(remote::getAll).isInstanceOf(ConfigException.class);
        server.verify();
    }

    @Test
    void refreshesDiskCacheAtStartup() {
        final var properties = properties();
        properties.setRetryDelayMs(60000);
        final var cache = new ConfigCacheManager(directory.toString(), "shop");
        cache.updateAll(List.of(item("true")));
        final var restored = new ConfigCacheManager(directory.toString(), "shop");
        final var remote = mock(ConfigRemoteTemplate.class);
        when(remote.getAll()).thenReturn(List.of(item("false")));
        final var initializer = initializer(properties, restored, remote, new CountDownLatch(0));

        initializer.start();
        try {
            assertThat(restored.get("enabled", "payment").getContent()).isEqualTo("false");
        } finally {
            initializer.stop();
        }
        assertThat(initializer.isRunning()).isFalse();
    }

    @Test
    void retriesAfterInitialFailureAndKeepsTheLastKnownValues() throws InterruptedException {
        final var properties = properties();
        properties.setRetryDelayMs(20);
        final var cache = new ConfigCacheManager(directory.toString(), "shop");
        cache.updateAll(List.of(item("true")));
        final var remote = mock(ConfigRemoteTemplate.class);
        when(remote.getAll()).thenThrow(new ConfigException("unavailable")).thenReturn(List.of(item("false")));
        final var refreshed = new CountDownLatch(1);
        final var initializer = initializer(properties, cache, remote, refreshed);

        initializer.start();
        try {
            assertThat(refreshed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(cache.get("enabled", "payment").getContent()).isEqualTo("false");
        } finally {
            initializer.stop();
        }
    }

    private ConfigStartupInitializer initializer(
        ConfigClientProperties properties,
        ConfigCacheManager cache,
        ConfigRemoteTemplate remote,
        CountDownLatch refreshed
    ) {
        final var beans = new DefaultListableBeanFactory();
        return new ConfigStartupInitializer(
            remote,
            new ConfigSubscriber(cache, event -> refreshed.countDown(), properties),
            properties,
            beans.getBeanProvider(ConfigSourceTemplate.class)
        );
    }

    private ConfigClientProperties properties() {
        final var properties = new ConfigClientProperties();
        properties.setAppCode("shop");
        return properties;
    }

    private ConfigItemDTO item(String value) {
        return new ConfigItemDTO("enabled", "payment", value, "text");
    }
}
