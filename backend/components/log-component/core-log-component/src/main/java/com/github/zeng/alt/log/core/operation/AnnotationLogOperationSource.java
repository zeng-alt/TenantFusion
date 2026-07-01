package com.github.zeng.alt.log.core.operation;


import com.github.zeng.alt.log.Log;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;

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

        // 方法优先
        Log log = AnnotatedElementUtils.findMergedAnnotation(method, Log.class);
        if (log != null) {
            return log;
        }

        // 再查类
        if (targetClass != null) {
            return AnnotatedElementUtils.findMergedAnnotation(targetClass, Log.class);
        }

        return null;
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