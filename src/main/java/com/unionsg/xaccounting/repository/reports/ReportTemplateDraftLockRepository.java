package com.unionsg.xaccounting.repository.reports;

import com.unionsg.xaccounting.entity.reports.ReportTemplateDraftLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ReportTemplateDraftLockRepository extends JpaRepository<ReportTemplateDraftLock, Long> {

    Optional<ReportTemplateDraftLock> findByTemplateId(Long templateId);

    @Modifying
    @Query("delete from ReportTemplateDraftLock l where l.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);

    @Modifying
    void deleteByTemplateId(Long templateId);
}

