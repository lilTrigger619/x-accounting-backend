package com.unionsg.xaccounting.repository;

import com.unionsg.xaccounting.entity.TaxCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxCategoryRepository extends JpaRepository<TaxCategory, Long> {
    List<TaxCategory> findByDeletedFalse();
}
