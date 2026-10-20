package com.unionsg.xaccounting.repository.fixedasset;

import com.unionsg.xaccounting.entity.fixedasset.FixedAsset;
import com.unionsg.xaccounting.enums.fixedasset.FixedAssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface FixedAssetRepository extends JpaRepository<FixedAsset, Long>, JpaSpecificationExecutor<FixedAsset> {

    boolean existsByCategoryIdAndDeletedFalse(Long categoryId);

    long countByCategoryIdAndDeletedFalse(Long categoryId);

    boolean existsByAssetNumberIgnoreCaseAndDeletedFalse(String assetNumber);

    boolean existsByAssetNumberIgnoreCaseAndDeletedFalseAndIdNot(String assetNumber, Long id);

    List<FixedAsset> findByDeletedFalseOrderByAssetNumberAsc();

    List<FixedAsset> findByDeletedFalseAndStatusIn(Collection<FixedAssetStatus> statuses);

    /** Assets that may owe depreciation for a period ending on {@code periodEnd}. */
    @Query("""
            select a from FixedAsset a
            where a.deleted = false
              and a.status = com.unionsg.xaccounting.enums.fixedasset.FixedAssetStatus.ACTIVE
              and coalesce(a.depreciationStartDate, a.availableForUseDate) <= :periodEnd
            order by a.assetNumber asc
            """)
    List<FixedAsset> findDepreciable(@Param("periodEnd") LocalDate periodEnd);
}
