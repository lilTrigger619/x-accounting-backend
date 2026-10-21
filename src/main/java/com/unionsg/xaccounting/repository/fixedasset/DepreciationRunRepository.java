package com.unionsg.xaccounting.repository.fixedasset;

import com.unionsg.xaccounting.entity.fixedasset.DepreciationRun;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DepreciationRunRepository extends JpaRepository<DepreciationRun, Long>, JpaSpecificationExecutor<DepreciationRun> {

    Optional<DepreciationRun> findByActivePeriodId(Long periodId);

    List<DepreciationRun> findByAccountingPeriodIdOrderByIdDesc(Long periodId);

    List<DepreciationRun> findByStatusInOrderByPeriodStartDesc(Collection<DepreciationRunStatus> statuses);

    List<DepreciationRun> findTop10ByDeletedFalseOrderByIdDesc();
}
