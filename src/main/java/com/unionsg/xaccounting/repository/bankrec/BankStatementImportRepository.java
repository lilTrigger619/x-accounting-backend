package com.unionsg.xaccounting.repository.bankrec;

import com.unionsg.xaccounting.entity.bankrec.BankStatementImport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankStatementImportRepository extends JpaRepository<BankStatementImport, Long> {

    Optional<BankStatementImport> findByIdAndDeletedFalse(Long id);

    List<BankStatementImport> findByBankAccountIdAndDeletedFalseOrderByIdDesc(Long bankAccountId);

    List<BankStatementImport> findByDeletedFalseOrderByIdDesc();

    boolean existsByProfileIdAndDeletedFalse(Long profileId);
}
