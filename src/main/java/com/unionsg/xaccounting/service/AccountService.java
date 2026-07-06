package com.unionsg.xaccounting.service;

import com.unionsg.xaccounting.dto.AccountListResponse;
import com.unionsg.xaccounting.enums.AccountStatus;
import com.unionsg.xaccounting.enums.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountService {

    Page<AccountListResponse> getAccounts(
            String search,
            AccountType accountType,
            AccountStatus status,
            Pageable pageable
    );
}


