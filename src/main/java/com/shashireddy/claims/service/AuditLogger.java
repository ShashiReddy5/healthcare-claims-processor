package com.shashireddy.claims.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logging-only audit trail. It's the integration seam for a real, queryable
 * audit store (a dedicated audit table or an external log sink) that a
 * regulated deployment would need — swapping the implementation is the
 * only change required, since every call site depends on this interface.
 */
@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    public void log(String event, String claimId, String actor) {
        log.info("event={} claimId={} actor={}", event, claimId, actor);
    }
}
