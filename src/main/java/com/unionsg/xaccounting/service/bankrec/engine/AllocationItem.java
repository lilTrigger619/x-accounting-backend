package com.unionsg.xaccounting.service.bankrec.engine;

import java.math.BigDecimal;

/**
 * An item picked for a manual match. {@code signedRemaining} is from the bank's point of view
 * (deposits positive). {@code requested} is an optional explicit allocation (absolute).
 */
public record AllocationItem(Long id, BigDecimal signedRemaining, BigDecimal requested) {
}
