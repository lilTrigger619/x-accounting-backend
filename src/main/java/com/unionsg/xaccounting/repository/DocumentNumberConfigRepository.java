package com.unionsg.xaccounting.repository;

import com.unionsg.xaccounting.entity.DocumentNumberConfig;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface DocumentNumberConfigRepository extends JpaRepository<DocumentNumberConfig, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DocumentNumberConfig> findByModuleAndCompanyIdAndBranchId(
            String module,
            Long companyId,
            Long branchId
    );
}
