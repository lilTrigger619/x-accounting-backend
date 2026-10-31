package com.unionsg.xaccounting.service;

import com.unionsg.xaccounting.dto.AccountListResponse;
import com.unionsg.xaccounting.dto.SimilarAccountsResponse;
import com.unionsg.xaccounting.enums.AccountStatus;
import com.unionsg.xaccounting.enums.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

public interface AccountService {

    Page<AccountListResponse> getAccounts(
            String search,
            AccountType accountType,
            AccountStatus status,
            Pageable pageable
    );

    /** The accounts with these account numbers, with their current balances. */
    List<AccountListResponse> getAccountsByNumbers(Collection<String> accountNumbers);

    /** Existing accounts whose names look like {@code name}, most alike first, at most {@code limit} of them. */
    SimilarAccountsResponse findSimilarByName(String name, int limit);
}
