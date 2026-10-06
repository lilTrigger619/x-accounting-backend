package com.unionsg.xaccounting.repository.bankrec;

import com.unionsg.xaccounting.entity.bankrec.BankStatementImportProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankStatementImportProfileRepository extends JpaRepository<BankStatementImportProfile, Long> {

    Optional<BankStatementImportProfile> findByIdAndDeletedFalse(Long id);

    List<BankStatementImportProfile> findByDeletedFalseOrderByNameAsc();

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndDeletedFalseAndIdNot(String name, Long id);

    boolean existsByDeletedFalse();
}
