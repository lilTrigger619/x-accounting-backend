package com.unionsg.xaccounting.repository.settings;

import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.settings.BankAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByStatus(BankAccountStatus status);

    boolean existsByGlAccountCode(String glAccountCode);
}
