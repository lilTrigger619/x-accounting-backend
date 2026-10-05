package com.unionsg.xaccounting.service;

import com.unionsg.xaccounting.dto.AccountCreationDTO;
import com.unionsg.xaccounting.dto.AccountDTO;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.ChartOfAccountClearTo_ENTITY;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.ChartOfAccountClearToRepository;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.service.config.ConfigValueValidator;
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
    private final TaxCategoryService taxCategoryService;
    private final ConfigValueValidator configValues;

    @Override
    @Transactional
    public AccountCreationDTO createAccount(AccountCreationDTO accountCreationDTO) {
        if (accountCreationDTO.getAccountId() == null || accountCreationDTO.getAccountId().isBlank()) {
            throw new BusinessException("Account code is required");
        }
        if (accountCreationDTO.getAccountName() == null || accountCreationDTO.getAccountName().isBlank()) {
            throw new BusinessException("Account name is required");
        }
        if (accountCreationDTO.getClearsTo() == null || accountCreationDTO.getClearsTo().isBlank()) {
            throw new BusinessException("Clears to is required");
        }
        Long clearToCode;
        try {
            clearToCode = Long.parseLong(accountCreationDTO.getClearsTo().trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("\"" + accountCreationDTO.getClearsTo() + "\" is not a valid clears-to code");
        }
        ChartOfAccountClearTo_ENTITY chartOfAccountClearTo = chartOfAccountClearToRepository.findByClearToCode(clearToCode)
                .orElseThrow(() -> new BusinessException("Chart of account clear to not found with code: " + accountCreationDTO.getClearsTo()));

        if (accountRepository.existsByAccountId(accountCreationDTO.getAccountId())) {
            throw new BusinessException("Account code already exists: " + accountCreationDTO.getAccountId());
        }

        String taxRate = accountCreationDTO.getDefaultTaxRate();
        if (taxRate != null && !taxRate.isBlank()) {
            try {
                taxCategoryService.getUsable(Long.parseLong(taxRate.trim()), null);
            } catch (NumberFormatException e) {
                throw new BusinessException("\"" + taxRate + "\" is not a valid tax rate");
            }
        } else {
            taxRate = null;
        }

        User user = SecurityUtils.getCurrentUser();

        AccountEntity entity = AccountEntity.builder()
                .accountId(accountCreationDTO.getAccountId().trim())
                .accountName(accountCreationDTO.getAccountName().trim())
                .coaClearTo(chartOfAccountClearTo)
                .taxRate(taxRate)
                .createdBy(user)
                .description(accountCreationDTO.getDescription())
                .currency(configValues.validate("currencies", accountCreationDTO.getCurrency(), null, "Currency"))
                .isActive(accountCreationDTO.getIsActive() == null || accountCreationDTO.getIsActive())
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
                .id(entity.getId())
                .accountId(entity.getAccountId())
                .accountName(entity.getAccountName())
                .clearsTo(entity.getCoaClearTo().getId().toString())
                .currency(entity.getCurrency())
                .description(entity.getDescription())
                .defaultTaxRate(entity.getTaxRate())
                .isActive(entity.getIsActive())
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
                .controlAccount(Boolean.TRUE.equals(entity.getIsControlAccount()))
                .build();
    }
}

