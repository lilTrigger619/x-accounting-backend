package com.unionsg.xaccounting.service;

import com.unionsg.xaccounting.dto.AccountCreationDTO;
import com.unionsg.xaccounting.dto.AccountDTO;

import java.util.List;

public interface AccountCommandService {
    AccountCreationDTO createAccount(AccountCreationDTO accountCreationDTO);

    AccountDTO getAccountById(Long id);

    List<AccountDTO> getAllAccounts();

    AccountDTO updateAccount(Long id, AccountDTO accountDTO);

    void deleteAccount(Long id);

    AccountDTO softDeleteAccount(Long id);
}

