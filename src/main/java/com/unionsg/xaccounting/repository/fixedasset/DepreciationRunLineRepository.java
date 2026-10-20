package com.unionsg.xaccounting.repository.fixedasset;

import com.unionsg.xaccounting.entity.fixedasset.DepreciationRunLine;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationLineStatus;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface DepreciationRunLineRepository extends JpaRepository<DepreciationRunLine, Long> {

    List<DepreciationRunLine> findByAssetIdOrderByFromDateAsc(Long assetId);

    /** Lines of the asset sitting in runs that are not posted yet. */
    @Query("select l from DepreciationRunLine l join fetch l.run r where l.asset.id = :assetId and r.status in :statuses")
    List<DepreciationRunLine> findByAssetInRunStatuses(@Param("assetId") Long assetId,
                                                       @Param("statuses") Collection<DepreciationRunStatus> statuses);

    @Query("""
            select l from DepreciationRunLine l join fetch l.run r join fetch l.asset a
            where l.status in :statuses and r.postingDate between :from and :to
            order by r.postingDate asc, a.assetNumber asc
            """)
    List<DepreciationRunLine> findPostedBetween(@Param("statuses") Collection<DepreciationLineStatus> statuses,
                                               @Param("from") LocalDate from, @Param("to") LocalDate to);
}
