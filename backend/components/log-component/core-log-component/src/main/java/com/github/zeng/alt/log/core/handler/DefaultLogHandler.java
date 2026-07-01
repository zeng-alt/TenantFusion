package com.github.zeng.alt.log.core.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.zeng.alt.log.OperLogEvent;
import com.github.zeng.alt.log.core.operation.LogInvocation;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@RequiredArgsConstructor
public class DefaultLogHandler
        implements LogHandler {

    private final LogRecordFactory factory;

    private final ApplicationEventPublisher publisher;

    @Override
    public void handle(LogInvocation invocation) throws JsonProcessingException {

        OperLogEvent event =
                factory.create(invocation);

        publisher.publishEvent(event);

    }

}