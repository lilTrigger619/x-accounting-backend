package com.unionsg.xaccounting.service.reports.engine.view;

public interface AccountAssignmentView {

    Long accountId();

    /** Optional presentation fields */
    String accountCode();

    String accountName();

    String sectionCode();
}



