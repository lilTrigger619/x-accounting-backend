package com.unionsg.xaccounting.repository.fixedasset;

import com.unionsg.xaccounting.entity.fixedasset.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {

    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);

    boolean existsByCodeIgnoreCaseAndDeletedFalseAndIdNot(String code, Long id);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndDeletedFalseAndIdNot(String name, Long id);

    List<AssetCategory> findByDeletedFalseOrderByNameAsc();

    List<AssetCategory> findByActiveTrueAndDeletedFalseOrderByNameAsc();
}
