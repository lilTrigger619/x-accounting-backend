package com.unionsg.xaccounting.service;

import com.unionsg.xaccounting.entity.ChartOfAccount;
import com.unionsg.xaccounting.entity.ChartOfAccountClearTo_ENTITY;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.ChartOfAccountRepository;
import com.unionsg.xaccounting.repository.ChartOfAccountClearTo_Repository;
import com.unionsg.xaccounting.dto.AccountDTO;
//import com.unionsg.xaccounting.repository.ChartOfAccountRepository
import com.unionsg.xaccounting.entity.AccountEntity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final ChartOfAccountClearTo_Repository chartOfAccountClearToRepository;

    @Transactional
    public AccountDTO createAccount(AccountDTO accountDTO) {
        // Validate chart code exists
//        ChartOfAccount chartOfAccount = chartOfAccountRepository.findByCoaCode(accountDTO.getChartCode())
//                .orElseThrow(() -> new RuntimeException("Chart of Account not found with code: " + accountDTO.getChartCode()));
        ChartOfAccountClearTo_ENTITY chartOfAccountClearTo = chartOfAccountClearToRepository.findById(accountDTO.getCoaClearToId())
                .orElseThrow(() -> new RuntimeException("Chart of account clear to not found with code: "+ accountDTO.getCoaClearToId()));

       DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

       LocalDateTime dateTime = LocalDateTime.parse(accountDTO.getOpeningBalanceDate(), formatter);

        // Check if account ID already exists
        if (accountRepository.existsByAccountId(accountDTO.getAccountId())) {
            throw new RuntimeException("Account ID already exists: " + accountDTO.getAccountId());
        }

        AccountEntity entity = AccountEntity.builder()
                .accountId(accountDTO.getAccountId())
                .accountName(accountDTO.getAccountName())
                .coaClearTo(chartOfAccountClearTo)
                .openingBalance(accountDTO.getOpeningBalance())
                .opening_balance_date(dateTime)
                .taxRate(accountDTO.getTaxRate())
                .createdBy(accountDTO.getCreatedBy())
                .description(accountDTO.getDescription())
                .currency(accountDTO.getCurrency())
                //.dateCreated(accountDTO.getDateCreated())
                //.clearsTo(accountDTO.getClearsTo())
                //.restriction(accountDTO.getRestriction())
                //.postingLevel(accountDTO.getPostingLevel())
                //.levelId(accountDTO.getLevelId())
                //.statementType(accountDTO.getStatementType())
                //.statementCode(accountDTO.getStatementCode())
                //.societyId(accountDTO.getSocietyId())
                //.circuitId(accountDTO.getCircuitId())
                //.postedBy(accountDTO.getPostedBy())
                //.approvedBy(accountDTO.getApprovedBy())
                //.approvedStatus(accountDTO.getApprovedStatus())
                //.deleted(accountDTO.isDeleted())
                .build();

        AccountEntity saved = accountRepository.save(entity);
        return convertToDTO(saved);
    }

    @Transactional(readOnly = true)
    public AccountDTO getAccountById(Long id) {
        AccountEntity entity = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));
        return convertToDTO(entity);
    }

    @Transactional(readOnly = true)
    public List<AccountDTO> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountDTO updateAccount(Long id, AccountDTO accountDTO) {
        AccountEntity entity = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));

        // Update chart of account if changed
        if (accountDTO.getCoaClearToId() != null) {
//            ChartOfAccount chartOfAccount = chartOfAccountRepository.findByCoaCode(accountDTO.getChartCode())
//                    .orElseThrow(() -> new RuntimeException("Chart of Account not found with code: " + accountDTO.getChartCode()));

            ChartOfAccountClearTo_ENTITY coaClearTo = chartOfAccountClearToRepository.findById(accountDTO.getCoaClearToId())
                            .orElseThrow(() -> new RuntimeException("Chart of Account not found with code: "+accountDTO.getCoaClearToId()));
            //entity.setChartOfAccount(chartOfAccount);
            entity.setCoaClearTo(coaClearTo);
        }

        entity.setAccountId(accountDTO.getAccountId());
        entity.setAccountName(accountDTO.getAccountName());
        //entity.setClearsTo(accountDTO.getClearsTo());
//        entity.setRestriction(accountDTO.getRestriction());
//        entity.setPostingLevel(accountDTO.getPostingLevel());
//        entity.setLevelId(accountDTO.getLevelId());
        entity.setCurrency(accountDTO.getCurrency());
        //already set
        //entity.setCoaClearTo(accountDTO.getCoaClearToId());
        entity.setOpeningBalance(accountDTO.getOpeningBalance());
        entity.setTaxRate(accountDTO.getTaxRate());
        entity.setDescription(accountDTO.getDescription());
//        entity.setStatementType(accountDTO.getStatementType());
//        entity.setStatementCode(accountDTO.getStatementCode());
//        entity.setSocietyId(accountDTO.getSocietyId());
//        entity.setCircuitId(accountDTO.getCircuitId());
//        entity.setPostedBy(accountDTO.getPostedBy());
//        entity.setApprovedBy(accountDTO.getApprovedBy());
//        entity.setApprovedStatus(accountDTO.getApprovedStatus());
//        entity.setDeleted(accountDTO.getDeleted());

        AccountEntity updated = accountRepository.save(entity);
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteAccount(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new RuntimeException("Account not found with id: " + id);
        }
        accountRepository.deleteById(id);
    }

    // Soft delete - mark as deleted
    @Transactional
    public AccountDTO softDeleteAccount(Long id) {
        AccountEntity entity = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));
        entity.setDeleted(true);
        AccountEntity updated = accountRepository.save(entity);
        return convertToDTO(updated);
    }

    private AccountDTO convertToDTO(AccountEntity entity) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = entity.getOpening_balance_date();
        String dateString = dateTime.format(formatter);
        return AccountDTO.builder()
                .id(entity.getId())
                .accountId(entity.getAccountId())
                .accountName(entity.getAccountName())
                //.chartCode(entity.getChartOfAccount() != null ? entity.getChartOfAccount().getCoaCode() : null)
                //.clearsTo(entity.getClearsTo())
                .coaClearToId(entity.getCoaClearTo().getId())
//                .restriction(entity.getRestriction())
//                .postingLevel(entity.getPostingLevel())
//                .levelId(entity.getLevelId())
                .currency(entity.getCurrency())
                .description(entity.getDescription())
                .taxRate(entity.getTaxRate())
                .openingBalance(entity.getOpeningBalance())
                .openingBalanceDate(dateString)
                .createdBy(entity.getCreatedBy())
                .dateCreated(entity.getDateCreated())

//                .statementType(entity.getStatementType())
//                .statementCode(entity.getStatementCode())
//                .societyId(entity.getSocietyId())
//                .circuitId(entity.getCircuitId())
//                .postedBy(entity.getPostedBy())
//                .approvedBy(entity.getApprovedBy())
//                .approvedStatus(entity.getApprovedStatus())
//                .deleted(entity.isDeleted())
//                .dateCreated(entity.getDateCreated())
                .build();
    }
}