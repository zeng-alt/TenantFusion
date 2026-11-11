package com.github.zeng.alt.tenant.remote;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMode;
import com.github.zeng.alt.tenant.remote.client.TenantAdminApi;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feign 外壳：404 归一为空，非 404 原样上抛。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
class TenantAdminClientTest {

    private final TenantMetadata t001 = new TenantMetadata(
            "t001", "租户一", TenantMode.SCHEMA, null, "sch_t001", null, false, true);

    @Test
    @DisplayName("正常返回透传，404 归一")
    void delegatesAndMapsNotFound() {
        TenantAdminApi api = mock(TenantAdminApi.class);
        when(api.findById("t001")).thenReturn(t001);
        when(api.findById("unknown")).thenThrow(notFound());
        when(api.findAll()).thenReturn(List.of(t001));

        TenantAdminClient client = new TenantAdminClient(api);

        assertThat(client.findById("t001")).isEqualTo(t001);
        assertThat(client.findById("unknown")).isNull();
        assertThat(client.findAll()).containsExactly(t001);
    }

    @Test
    @DisplayName("非 404 异常直接上抛")
    void rethrowsOtherErrors() {
        TenantAdminApi api = mock(TenantAdminApi.class);
        when(api.findById("t001")).thenThrow(new FeignException.InternalServerError(
                "boom", dummyRequest(), "boom".getBytes(StandardCharsets.UTF_8), Map.of()));

        TenantAdminClient client = new TenantAdminClient(api);

        assertThatThrownBy(() -> client.findById("t001"))
                .isInstanceOf(FeignException.InternalServerError.class);
    }

    private FeignException.NotFound notFound() {
        return new FeignException.NotFound(
                "not found", dummyRequest(), new byte[0], Map.of());
    }

    private Request dummyRequest() {
        return Request.create(Request.HttpMethod.GET, "/v1/tenants/x",
                Map.of(), null, StandardCharsets.UTF_8);
    }
}
