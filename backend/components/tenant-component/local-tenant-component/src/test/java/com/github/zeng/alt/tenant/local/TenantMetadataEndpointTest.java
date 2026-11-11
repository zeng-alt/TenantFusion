package com.github.zeng.alt.tenant.local;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.api.TenantMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 租户元数据端点：委托 provider，不存在时 404。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
class TenantMetadataEndpointTest {

    private final TenantMetadata master = new TenantMetadata(
            "master", "主租户", TenantMode.NONE, null, null, null, false, true);
    private final TenantMetadata t001 = new TenantMetadata(
            "t001", "租户一", TenantMode.SCHEMA, null, "sch_t001", null, false, true);

    private final TenantMetadataProvider stub = new TenantMetadataProvider() {
        private final Map<String, TenantMetadata> store = List.of(master, t001).stream()
                .collect(Collectors.toMap(TenantMetadata::tenantId, Function.identity()));

        @Override
        public Optional<TenantMetadata> findById(String tenantId) {
            return Optional.ofNullable(store.get(tenantId));
        }

        @Override
        public List<TenantMetadata> findAll() {
            return List.copyOf(store.values());
        }
    };

    private final TenantMetadataEndpoint endpoint = new TenantMetadataEndpoint(stub);

    @Test
    @DisplayName("全量查询透传 provider 结果")
    void listAllDelegates() {
        assertThat(endpoint.listAll())
                .extracting(TenantMetadata::tenantId)
                .containsExactlyInAnyOrder("master", "t001");
    }

    @Test
    @DisplayName("单查命中返回裸元数据，缺席返回 404")
    void getByIdOr404() {
        assertThat(endpoint.getById("t001").getBody().schemaName()).isEqualTo("sch_t001");
        assertThat(endpoint.getById("t001").getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(endpoint.getById("unknown").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(endpoint.getById("unknown").getBody()).isNull();
    }
}
