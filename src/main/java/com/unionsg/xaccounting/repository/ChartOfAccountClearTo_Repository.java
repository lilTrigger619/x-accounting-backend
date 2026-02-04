package com.unionsg.xaccounting.repository;

import com.unionsg.xaccounting.entity.ChartOfAccountClearTo_ENTITY;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Repository
public interface ChartOfAccountClearTo_Repository extends JpaRepository<ChartOfAccountClearTo_ENTITY, Long>{ }
