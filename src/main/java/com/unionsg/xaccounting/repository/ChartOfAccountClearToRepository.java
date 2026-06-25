package com.unionsg.xaccounting.repository;

import com.unionsg.xaccounting.entity.ChartOfAccountClearTo_ENTITY;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChartOfAccountClearToRepository extends JpaRepository<ChartOfAccountClearTo_ENTITY, Long> {

    Optional<ChartOfAccountClearTo_ENTITY> findByClearToCode(Long clearToCode);
}

