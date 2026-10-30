package com.enterprise.mongodb.changestream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Downstream consumer receiving change stream notifications asynchronously.
 * Can be used for search-index syncing, cache invalidation, or webhook dispatches.
 */
@Slf4j
@Component
public class ChangeStreamEventConsumer {

    @Async
    @EventListener
    public void handleAccountChange(AccountChangeEvent event) {
        log.info("Processing async change stream event for document '{}' with operation '{}'",
                event.getAccountId(), event.getOperationType());
    }
}
