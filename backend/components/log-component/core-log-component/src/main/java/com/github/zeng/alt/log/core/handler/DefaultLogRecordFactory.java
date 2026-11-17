package com.github.zeng.alt.log.core.handler;

import com.github.zeng.alt.log.BusinessStatus;
import com.github.zeng.alt.log.OperLogEvent;
import com.github.zeng.alt.log.core.operation.LogInvocation;
import com.github.zeng.alt.log.core.operation.LogOperation;
import com.github.zeng.alt.log.core.support.IpResolver;
import com.github.zeng.alt.log.core.support.LogValueHelper;
import com.github.zeng.alt.log.core.support.RequestParameterResolver;
import com.github.zeng.alt.log.core.support.RequestResolver;
import com.github.zeng.alt.log.core.support.TenantResolver;
import com.github.zeng.alt.log.core.support.UserResolver;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.aop.support.AopUtils;

/**
 * 创建包含请求身份、方法执行结果和时间的操作日志。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@CommonsLog
@RequiredArgsConstructor
public class DefaultLogRecordFactory implements LogRecordFactory {
    private final UserResolver userResolver;
    private final IpResolver ipResolver;
    private final RequestResolver requestResolver;
    private final RequestParameterResolver parameterResolver;
    private final TenantResolver tenantResolver;
    private final LogValueHelper values;

    @Override
    public OperLogEvent create(LogInvocation invocation) {
        final OperLogEvent event = new OperLogEvent();
        final LogOperation operation = invocation.getOperation();
        event.setTitle(operation.getTitle());
        event.setBusinessType(operation.getBusinessType().ordinal());
        event.setOperatorType(operation.getOperatorType().ordinal());
        event.setOperIp(ipResolver.resolve());
        event.setOperUrl(requestResolver.requestURI());
        event.setOperName(userResolver.currentUser());
        event.setTenantId(tenantResolver.currentTenant().getOrElse(""));
        event.setRequestMethod(requestResolver.method());
        final Method method = AopUtils.getMostSpecificMethod(
                invocation.getInvocation().getMethod(),
                invocation.getInvocation().getThis().getClass()
        );
        event.setMethod(method.getDeclaringClass().getName() + "." + method.getName());
        event.setOperTime(LocalDateTime.now());
        event.setStatus(invocation.getThrowable() == null
                ? BusinessStatus.SUCCESS.ordinal() : BusinessStatus.FAIL.ordinal());
        event.setCostTime(invocation.getDuration().toMillis());
        if (invocation.getThrowable() != null) {
            event.setErrorMsg(invocation.getThrowable().getMessage());
        }
        if (operation.isSaveRequest()) {
            event.setOperParam(serialize(() -> parameterResolver.resolve(invocation)));
        }
        if (operation.isSaveResponse() && invocation.getResult() != null) {
            event.setJsonResult(serialize(() -> values.toJson(invocation.getResult(), operation.getExcludeParams())));
        }
        return event;
    }

    private String serialize(CheckedFunction0<String> serialization) {
        return Try.of(serialization).onFailure(error -> log.warn("Failed to serialize operation log value", error))
                .getOrElse("");
    }
}
