package com.github.zeng.alt.log.core.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.log.BusinessStatus;
import com.github.zeng.alt.log.OperLogEvent;
import com.github.zeng.alt.log.core.operation.LogInvocation;
import com.github.zeng.alt.log.core.operation.LogOperation;
import com.github.zeng.alt.log.core.support.IpResolver;
import com.github.zeng.alt.log.core.support.RequestParameterResolver;
import com.github.zeng.alt.log.core.support.RequestResolver;
import com.github.zeng.alt.log.core.support.UserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;

@RequiredArgsConstructor
public class DefaultLogRecordFactory
        implements LogRecordFactory {

    private final UserResolver userResolver;
    private final IpResolver ipResolver;
    private final RequestResolver requestResolver;
    private final RequestParameterResolver parameterResolver;
    private final ObjectProvider<ObjectMapper> provider;

    public ObjectMapper getMapper() {
        return provider.getObject();
    }

    @Override
    public OperLogEvent create(LogInvocation invocation) throws JsonProcessingException {

        OperLogEvent log = new OperLogEvent();

        LogOperation op =
                invocation.getOperation();

        log.setTitle(op.getTitle());

        log.setBusinessType(
                op.getBusinessType().ordinal());

        log.setOperatorType(
                op.getOperatorType().ordinal());

        log.setOperIp(
                ipResolver.resolve());

        log.setOperUrl(
                requestResolver.requestURI());

        String user =
                userResolver.currentUser();

        if (user != null) {
            log.setOperName(user);
//            log.setDeptName(user.getDeptName());
        }

        log.setRequestMethod(
                requestResolver.method());

        if (op.isSaveRequest()) {
            log.setOperParam(
                    parameterResolver.resolve(invocation));
        }

        if (op.isSaveRequest()
                && invocation.getResult() != null) {

            log.setJsonResult(
                    getMapper().writeValueAsString(
                            invocation.getResult()));
        }

        if (invocation.getThrowable() != null) {

            log.setStatus(
                    BusinessStatus.FAIL.ordinal());

            log.setErrorMsg(
                    invocation.getThrowable()
                            .getMessage());
        }

        log.setCostTime(
                invocation.getDuration().toMillis());

        return log;

    }

}