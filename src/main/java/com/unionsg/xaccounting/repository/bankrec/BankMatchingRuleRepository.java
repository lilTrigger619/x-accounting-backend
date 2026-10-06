package com.unionsg.xaccounting.repository.bankrec;

import com.unionsg.xaccounting.entity.bankrec.BankMatchingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BankMatchingRuleRepository extends JpaRepository<BankMatchingRule, Long> {

    Optional<BankMatchingRule> findByIdAndDeletedFalse(Long id);

    List<BankMatchingRule> findByDeletedFalseOrderByPriorityAscIdAsc();

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndDeletedFalseAndIdNot(String name, Long id);

    boolean existsByDeletedFalse();

    @Query("""
            select r from BankMatchingRule r
            where r.deleted = false and r.active = true
              and (r.bankAccount is null or r.bankAccount.id = :bankAccountId)
            order by r.priority asc, r.id asc
            """)
    List<BankMatchingRule> findApplicable(@Param("bankAccountId") Long bankAccountId);
}
