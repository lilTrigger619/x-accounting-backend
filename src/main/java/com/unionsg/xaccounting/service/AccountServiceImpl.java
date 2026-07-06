package com.unionsg.xaccounting.service;

import com.unionsg.xaccounting.dto.AccountListResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.enums.AccountStatus;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.repository.AccountRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

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

        List<AccountListResponse> mapped = page.getContent().stream()
                .map(this::toListResponse)
                .toList();

        return new PageImpl<>(mapped, pageable, page.getTotalElements());

    }

    private AccountListResponse toListResponse(AccountEntity entity) {
        // Project currently doesn't expose transactional balance in AccountEntity,
        // so we default to 0 for now.
        BigDecimal balance = BigDecimal.ZERO;

        AccountListResponse.AccountListResponseBuilder builder = AccountListResponse.builder()
                .accountNumber(entity.getAccountId())
                .accountName(entity.getAccountName())
                .accountType(entity.getCoaClearTo() != null && entity.getCoaClearTo().getChartOfAccount() != null
                        ? entity.getCoaClearTo().getChartOfAccount().getAccountType().name()
                        : null)
                .subType(entity.getCoaClearTo() != null
                        ? entity.getCoaClearTo().getId().toString()
                        : null)

                .status(entity.getIsActive() != null && entity.getIsActive() ? AccountStatus.ACTIVE : AccountStatus.INACTIVE)
                .balance(balance);

        return builder.build();
    }
}

