package com.unionsg.xaccounting.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DraftLockTtlConfig {

    private final long ttlSeconds;

    public DraftLockTtlConfig(
            @Value("${report.draftLock.ttlSeconds:300}") long ttlSeconds
    ) {
        this.ttlSeconds = ttlSeconds;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }
}

