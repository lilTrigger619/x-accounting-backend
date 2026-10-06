package com.unionsg.xaccounting.service;

import com.unionsg.xaccounting.dto.AccountListResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.enums.AccountStatus;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalLineRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;


import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    /** A reversed journal and its posted reversal cancel out, so both count toward balances. */
    private static final Set<JournalStatus> BALANCE_STATUSES = Set.of(JournalStatus.POSTED, JournalStatus.REVERSED);

    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;

    @Override
    public Page<AccountListResponse> getAccounts(String search,
                                                  AccountType accountType,
                                                  AccountStatus status,
                                                  Pageable pageable) {

        Specification<AccountEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only non-deleted accounts
            predicates.add(cb.equal(root.get("deleted"), false));

            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("accountId")), like),
                        cb.like(cb.lower(root.get("accountName")), like)
                ));
            }

            if (accountType != null) {
                predicates.add(cb.equal(root.get("coaClearTo").get("chartOfAccount").get("accountType"), accountType));
            }

            if (status != null) {
                boolean isActive = status == AccountStatus.ACTIVE;
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AccountEntity> page = accountRepository.findAll(spec, pageable);
        return new PageImpl<>(toListResponses(page.getContent()), pageable, page.getTotalElements());
    }

    @Override
    public List<AccountListResponse> getAccountsByNumbers(Collection<String> accountNumbers) {
        if (accountNumbers == null || accountNumbers.isEmpty()) {
            return List.of();
        }
        Specification<AccountEntity> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("deleted"), false),
                root.get("accountId").in(accountNumbers)
        );
        return toListResponses(accountRepository.findAll(spec));
    }

    private List<AccountListResponse> toListResponses(List<AccountEntity> accounts) {
        List<String> codes = accounts.stream().map(AccountEntity::getAccountId).toList();
        Map<String, BigDecimal> movements = codes.isEmpty()
                ? Map.of()
                : journalLineRepository.sumNetMovementByAccountCodes(codes, BALANCE_STATUSES).stream()
                        .collect(Collectors.toMap(
                                JournalLineRepository.AccountMovement::getAccountCode,
                                JournalLineRepository.AccountMovement::getNetMovement));
        return accounts.stream()
                .map(a -> toListResponse(a, movements.getOrDefault(a.getAccountId(), BigDecimal.ZERO)))
                .toList();
    }

    /**
     * The balance is shown on the account's normal side: debit-minus-credit for assets and
     * expenses, credit-minus-debit for liabilities, equity and income.
     */
    private AccountListResponse toListResponse(AccountEntity entity, BigDecimal netMovement) {
        AccountType type = entity.getCoaClearTo() != null && entity.getCoaClearTo().getChartOfAccount() != null
                ? entity.getCoaClearTo().getChartOfAccount().getAccountType()
                : null;
        boolean creditNormal = type == AccountType.LIABILITY || type == AccountType.EQUITY || type == AccountType.INCOME;
        BigDecimal balance = creditNormal ? netMovement.negate() : netMovement;

        AccountListResponse.AccountListResponseBuilder builder = AccountListResponse.builder()
                .id(entity.getId())
                .accountNumber(entity.getAccountId())
                .accountName(entity.getAccountName())
                .accountType(type != null ? type.name() : null)
                .subType(entity.getCoaClearTo() != null
                        ? entity.getCoaClearTo().getDescription()
                        : null)

                .status(entity.getIsActive() != null && entity.getIsActive() ? AccountStatus.ACTIVE : AccountStatus.INACTIVE)
                .balance(balance)
                .controlAccount(Boolean.TRUE.equals(entity.getIsControlAccount()));

        return builder.build();
    }
}

