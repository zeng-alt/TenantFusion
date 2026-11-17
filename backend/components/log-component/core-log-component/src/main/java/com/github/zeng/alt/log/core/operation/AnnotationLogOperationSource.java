package com.github.zeng.alt.log.core.operation;


import com.github.zeng.alt.log.Log;
import java.lang.reflect.Method;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;

public class AnnotationLogOperationSource
    extends AbstractFallbackLogOperationSource {

    @Override
    @Nullable
    protected LogOperation computeLogOperation(
        Method method,
        @Nullable Class<?> targetClass) {

        Log log = findLogAnnotation(method, targetClass);

        if (log == null) {
            return null;
        }

        return convert(log);
    }

    @Nullable
    protected Log findLogAnnotation(
        Method method,
        @Nullable Class<?> targetClass) {

        final Method specific = BridgeMethodResolver.findBridgedMethod(AopUtils.getMostSpecificMethod(method, targetClass));
        final Log log = AnnotatedElementUtils.findMergedAnnotation(specific, Log.class);
        if (log != null) {
            return log;
        }
        return AnnotatedElementUtils.findMergedAnnotation(method, Log.class);
    }

    protected LogOperation convert(Log log) {

        LogOperation operation = new LogOperation();

        operation.setTitle(log.title());
        operation.setBusinessType(log.businessType());
        operation.setOperatorType(log.operatorType());
        operation.setSaveRequest(log.isSaveRequestData());
        operation.setSaveResponse(log.isSaveResponseData());
        operation.setExcludeParams(log.excludeParamNames());

        return operation;
    }

}
