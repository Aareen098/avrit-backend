package com.hireflow.usage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

// Writes one usage row. It always uses its own small transaction, so:
//  - a usage row is kept even if the caller's transaction is read-only or rolls back (the resource was used anyway), and
//  - a problem while measuring is only logged and can never break the real action.
@Service
public class UsageService {

    private static final Logger log = LoggerFactory.getLogger(UsageService.class);

    private final UsageEventRepository events;
    private final TransactionTemplate tx;

    public UsageService(UsageEventRepository events, PlatformTransactionManager transactionManager) {
        this.events = events;
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void record(Long companyId, String kind, long units) {
        if (units <= 0) return;
        try {
            tx.executeWithoutResult(status -> events.save(new UsageEvent(companyId, kind, units)));
        } catch (RuntimeException e) {
            log.warn("Could not record usage {} ({}): {}", kind, units, e.toString());
        }
    }
}