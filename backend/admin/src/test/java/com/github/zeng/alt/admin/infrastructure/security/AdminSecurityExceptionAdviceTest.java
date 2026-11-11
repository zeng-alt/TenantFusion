package com.github.zeng.alt.admin.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 授权拒绝渲染为 403 ProblemDetail，而非被 RuntimeException 兜底成 500。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AdminSecurityExceptionAdviceTest {

    @Test
    void shouldRenderForbiddenProblemDetail() {
        final AdminSecurityExceptionAdvice advice = new AdminSecurityExceptionAdvice();
        final MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/v1/user/42");

        final ErrorResponse response = advice.handleAccessDenied(new AccessDeniedException("不满足资源访问条件"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getDetail()).isEqualTo("不满足资源访问条件");
        assertThat(response.getBody().getTitle()).isEqualTo("访问被拒绝");
        assertThat(response.getBody().getInstance()).hasToString("/v1/user/42");
    }
}