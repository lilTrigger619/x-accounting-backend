package com.unionsg.xaccounting.service.analytics;

import com.unionsg.xaccounting.enums.AccountType;

/** A ledger account with the BI class it rolls up to and the category it is grouped under. */
public record ClassifiedAccount(
        Long id,
        String code,
        String name,
        AccountType accountType,
        String category,
        AccountClass accountClass
) {
}
