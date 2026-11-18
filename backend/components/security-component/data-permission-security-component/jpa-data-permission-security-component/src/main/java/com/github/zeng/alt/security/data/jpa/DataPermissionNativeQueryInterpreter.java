package com.github.zeng.alt.security.data.jpa;

import org.hibernate.ScrollMode;
import org.hibernate.engine.query.internal.NativeQueryInterpreterStandardImpl;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.sql.spi.NativeSelectQueryDefinition;
import org.hibernate.query.sql.spi.NativeSelectQueryPlan;
import org.hibernate.query.sql.spi.ParameterRecognizer;
import org.hibernate.sql.results.spi.ResultsConsumer;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

/**
 * 受保护持久化单元的原生 SQL 不经过实体过滤，执行时明确拒绝。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class DataPermissionNativeQueryInterpreter implements NativeQueryInterpreter {
    private final NativeQueryInterpreter delegate;

    DataPermissionNativeQueryInterpreter(boolean ignoreJdbcParameters) {
        delegate = new NativeQueryInterpreterStandardImpl(ignoreJdbcParameters);
    }

    @Override
    public void recognizeParameters(String sql, ParameterRecognizer recognizer) {
        if (DataPermissionContext.active() && !DataPermissionContext.policyRead()) {
            throw new AccessDeniedException("Native SQL is not supported in @DataPermission methods");
        }
        delegate.recognizeParameters(sql, recognizer);
    }

    @Override
    public <R> NativeSelectQueryPlan<R> createQueryPlan(
            NativeSelectQueryDefinition<R> definition, SessionFactoryImplementor factory
    ) {
        final boolean protectedUnit = factory.getDefinedFilterNames().stream()
                .anyMatch(name -> name.startsWith(DataPermissionMappingContributor.PREFIX));
        return new GuardedPlan<>(delegate.createQueryPlan(definition, factory), protectedUnit);
    }

    private record GuardedPlan<R>(
            NativeSelectQueryPlan<R> delegate,
            boolean protectedUnit
    ) implements NativeSelectQueryPlan<R> {
        private void requireSupported() {
            if (protectedUnit && !DataPermissionContext.policyRead()) {
                throw new AccessDeniedException("Native SQL is not supported for a protected persistence unit");
            }
        }

        @Override
        public <T> T executeQuery(DomainQueryExecutionContext context, ResultsConsumer<T, R> consumer) {
            requireSupported();
            return delegate.executeQuery(context, consumer);
        }

        @Override
        public List<R> performList(DomainQueryExecutionContext context) {
            requireSupported();
            return delegate.performList(context);
        }

        @Override
        public ScrollableResultsImplementor<R> performScroll(ScrollMode mode, DomainQueryExecutionContext context) {
            requireSupported();
            return delegate.performScroll(mode, context);
        }
    }
}
