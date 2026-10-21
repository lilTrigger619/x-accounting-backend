package com.unionsg.xaccounting.repository.fixedasset;

import com.unionsg.xaccounting.entity.fixedasset.AssetEstimateChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetEstimateChangeRepository extends JpaRepository<AssetEstimateChange, Long> {

    List<AssetEstimateChange> findByAssetIdOrderByIdDesc(Long assetId);
}
