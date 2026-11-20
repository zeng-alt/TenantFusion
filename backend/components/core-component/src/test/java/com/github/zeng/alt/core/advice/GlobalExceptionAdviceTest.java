package com.github.zeng.alt.core.advice;

import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.api.exception.BaseI18nException;
import com.github.zeng.alt.api.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * 业务异常的 HTTP 状态和 ProblemDetail 响应回归。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class GlobalExceptionAdviceTest {

    private static final String PATH = "/v1/dict/type/25";
    private static final String FORBIDDEN_MESSAGE = "当前用户权限无法修改字典类型的【是否默认】属性";

    @Test
    void shouldReturnForbiddenStatusForDictionaryFailure() throws Exception {
        mvc(new ForbiddenException(FORBIDDEN_MESSAGE)).perform(patch(PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("权限不足!!!"))
                .andExpect(jsonPath("$.detail").value(FORBIDDEN_MESSAGE))
                .andExpect(jsonPath("$.instance").value(PATH));
    }

    @Test
    void shouldReturnForbiddenStatusForTransactionWrappedFailure() throws Exception {
        final TransactionSystemException failure = new TransactionSystemException(
                "提交事务失败", new RuntimeException("刷新失败", new ForbiddenException(FORBIDDEN_MESSAGE)));

        mvc(failure).perform(patch(PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("权限不足!!!"))
                .andExpect(jsonPath("$.detail").value(FORBIDDEN_MESSAGE))
                .andExpect(jsonPath("$.instance").value(PATH));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 404, 409, 422, 500, 503})
    void shouldUseStandardHttpStatusForBusinessFailure(int code) throws Exception {
        mvc(new BaseException(code, "业务错误", "操作失败")).perform(patch(PATH))
                .andExpect(status().is(code))
                .andExpect(jsonPath("$.status").value(code))
                .andExpect(jsonPath("$.title").value("业务错误"))
                .andExpect(jsonPath("$.detail").value("操作失败"));
    }

    @ParameterizedTest
    @ValueSource(ints = {600, 10001})
    void shouldPreserveCustomBusinessCodeWithHttpInternalServerError(int code) throws Exception {
        mvc(new BaseException(code, "业务错误", "操作失败")).perform(patch(PATH))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(code))
                .andExpect(jsonPath("$.title").value("业务错误"))
                .andExpect(jsonPath("$.detail").value("操作失败"));
    }

    @Test
    void shouldUseStandardHttpStatusForI18nFailure() throws Exception {
        mvc(new BaseI18nException(403, FORBIDDEN_MESSAGE)).perform(patch(PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value(FORBIDDEN_MESSAGE))
                .andExpect(jsonPath("$.detail").value(FORBIDDEN_MESSAGE));
    }

    @Test
    void shouldPreserveCustomI18nBusinessCode() throws Exception {
        mvc(new BaseI18nException(600, "操作失败")).perform(patch(PATH))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(600));
    }

    @Test
    void shouldReturnInternalServerErrorForUnexpectedTransactionFailure() throws Exception {
        mvc(new TransactionSystemException("提交事务失败", new IllegalStateException("连接中断")))
                .perform(patch(PATH))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    private MockMvc mvc(RuntimeException failure) {
        return standaloneSetup(new FailingController(failure))
                .setControllerAdvice(new GlobalExceptionAdvice(
                        new DefaultListableBeanFactory().getBeanProvider(MessageSourceAccessor.class)))
                .build();
    }

    @RestController
    private static class FailingController {

        private final RuntimeException failure;

        private FailingController(RuntimeException failure) {
            this.failure = failure;
        }

        @PatchMapping("/v1/dict/type/{id}")
        public void update() {
            throw failure;
        }
    }
}
