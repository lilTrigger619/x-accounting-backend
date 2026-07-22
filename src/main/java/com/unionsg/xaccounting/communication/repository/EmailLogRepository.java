package com.unionsg.xaccounting.communication.repository;

import com.unionsg.xaccounting.communication.domain.EmailLog;
import com.unionsg.xaccounting.communication.enums.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Page<EmailLog> findByEntityType(String entityType, Pageable pageable);

    Page<EmailLog> findByStatus(EmailStatus status, Pageable pageable);

    Page<EmailLog> findByEntityTypeAndStatus(String entityType, EmailStatus status, Pageable pageable);

    List<EmailLog> findByEntityTypeAndEntityId(String entityType, String entityId);
}

