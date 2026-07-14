package com.unionsg.xaccounting.service.reports.engine.view.impl;

import com.unionsg.xaccounting.service.reports.engine.view.AccountAssignmentView;

public record SimpleAccountAssignmentView(
        Long accountId,
        String accountCode,
        String accountName,
        String sectionCode
) implements AccountAssignmentView {
}



