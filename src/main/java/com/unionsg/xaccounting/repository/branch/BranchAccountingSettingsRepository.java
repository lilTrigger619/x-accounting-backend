package com.unionsg.xaccounting.repository.branch;

import com.unionsg.xaccounting.entity.branch.BranchAccountingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchAccountingSettingsRepository extends JpaRepository<BranchAccountingSettings, Long> {
    Optional<BranchAccountingSettings> findFirstByOrderByIdAsc();
}
