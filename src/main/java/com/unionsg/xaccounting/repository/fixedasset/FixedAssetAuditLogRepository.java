package com.unionsg.xaccounting.repository.fixedasset;

import com.unionsg.xaccounting.entity.fixedasset.FixedAssetAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FixedAssetAuditLogRepository extends JpaRepository<FixedAssetAuditLog, Long> {

    List<FixedAssetAuditLog> findByAssetIdOrderByPerformedAtDescIdDesc(Long assetId);

    List<FixedAssetAuditLog> findByRunIdOrderByPerformedAtDescIdDesc(Long runId);
}
