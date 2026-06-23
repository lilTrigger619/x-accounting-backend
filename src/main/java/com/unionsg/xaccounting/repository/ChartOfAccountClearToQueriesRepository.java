package com.unionsg.xaccounting.repository;

import com.unionsg.xaccounting.entity.ChartOfAccountClearTo_ENTITY;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChartOfAccountClearToQueriesRepository extends JpaRepository<ChartOfAccountClearTo_ENTITY, Long> {

    Optional<ChartOfAccountClearTo_ENTITY> findByClearToCode(Long clearToCode);

    @Query("select c from ChartOfAccountClearTo_ENTITY c where c.deleted = false")
    List<ChartOfAccountClearTo_ENTITY> findAllActive();

    @Query("select c from ChartOfAccountClearTo_ENTITY c where c.deleted = false and c.clearToCode = :clearToCode")
    Optional<ChartOfAccountClearTo_ENTITY> findActiveByClearToCode(@Param("clearToCode") Long clearToCode);
}

