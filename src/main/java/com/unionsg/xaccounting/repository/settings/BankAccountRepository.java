package com.unionsg.xaccounting.repository.settings;

import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.settings.BankAccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByStatus(BankAccountStatus status);

    boolean existsByGlAccountCode(String glAccountCode);

    /** Locks the row so concurrent postings that draw on the same account run one at a time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BankAccount b WHERE b.id = :id")
    Optional<BankAccount> findByIdForUpdate(@Param("id") Long id);
}
