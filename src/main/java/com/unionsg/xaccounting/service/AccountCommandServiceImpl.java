package com.unionsg.xaccounting.service;

import com.unionsg.xaccounting.dto.AccountCreationDTO;
import com.unionsg.xaccounting.dto.AccountDTO;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.ChartOfAccountClearTo_ENTITY;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.ChartOfAccountClearToRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.unionsg.xaccounting.security.util.SecurityUtils;

@Service
@RequiredArgsConstructor
public class AccountCommandServiceImpl implements AccountCommandService {

    private final AccountRepository accountRepository;
    private final ChartOfAccountClearToRepository chartOfAccountClearToRepository;

    @Override
    @Transactional
    public AccountCreationDTO createAccount(AccountCreationDTO accountCreationDTO) {
        Long clearToCode = Long.parseLong(accountCreationDTO.getClearsTo());
        ChartOfAccountClearTo_ENTITY chartOfAccountClearTo = chartOfAccountClearToRepository.findByClearToCode(clearToCode)
                .orElseThrow(() -> new RuntimeException("Chart of account clear to not found with code: " + accountCreationDTO.getClearsTo()));

        if (accountRepository.existsByAccountId(accountCreationDTO.getAccountId())) {
            throw new RuntimeException("Account ID already exists: " + accountCreationDTO.getAccountId());
        }

        User user = SecurityUtils.getCurrentUser();

        AccountEntity entity = AccountEntity.builder()
                .accountId(accountCreationDTO.getAccountId())
                .accountName(accountCreationDTO.getAccountName())
                .coaClearTo(chartOfAccountClearTo)
                .taxRate(accountCreationDTO.getDefaultTaxRate())
                .createdBy(user)
                .description(accountCreationDTO.getDescription())
                .currency(accountCreationDTO.getCurrency())
                .build();

        AccountEntity saved = accountRepository.save(entity);
        return convertToAccountCreationDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountById(Long id) {
        AccountEntity entity = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));
        return convertToDTO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountDTO> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AccountDTO updateAccount(Long id, AccountDTO accountDTO) {
        AccountEntity entity = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));

        if (accountDTO.getCoaClearToId() != null) {
            ChartOfAccountClearTo_ENTITY coaClearTo = chartOfAccountClearToRepository.findById(accountDTO.getCoaClearToId())
                    .orElseThrow(() -> new RuntimeException("Chart of Account not found with code: " + accountDTO.getCoaClearToId()));
            entity.setCoaClearTo(coaClearTo);
        }

        entity.setAccountId(accountDTO.getAccountId());
        entity.setAccountName(accountDTO.getAccountName());
        entity.setCurrency(accountDTO.getCurrency());
        entity.setTaxRate(accountDTO.getTaxRate());
        entity.setDescription(accountDTO.getDescription());

        AccountEntity updated = accountRepository.save(entity);
        return convertToDTO(updated);
    }

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new RuntimeException("Account not found with id: " + id);
        }
        accountRepository.deleteById(id);
    }

    @Override
    @Transactional
    public AccountDTO softDeleteAccount(Long id) {
        AccountEntity entity = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));
        entity.setDeleted(true);
        AccountEntity updated = accountRepository.save(entity);
        return convertToDTO(updated);
    }

    private AccountCreationDTO convertToAccountCreationDTO(AccountEntity entity) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dateString = null;
        return AccountCreationDTO.builder()
                .accountId(entity.getAccountId())
                .accountName(entity.getAccountName())
                .clearsTo(entity.getCoaClearTo().getId().toString())
                .currency(entity.getCurrency())
                .description(entity.getDescription())
                .defaultTaxRate(entity.getTaxRate())
                .build();
    }

    private AccountDTO convertToDTO(AccountEntity entity) {
        return AccountDTO.builder()
                .id(entity.getId())
                .accountId(entity.getAccountId())
                .accountName(entity.getAccountName())
                .coaClearToId(entity.getCoaClearTo().getId())
                .currency(entity.getCurrency())
                .description(entity.getDescription())
                .taxRate(entity.getTaxRate())
                .createdBy(entity.getCreatedBy().getFullName())
                .dateCreated(entity.getDateCreated())
                .build();
    }
}

