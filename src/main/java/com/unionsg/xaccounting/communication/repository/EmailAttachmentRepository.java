package com.unionsg.xaccounting.communication.repository;

import com.unionsg.xaccounting.communication.domain.EmailAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailAttachmentRepository extends JpaRepository<EmailAttachment, Long> {

    List<EmailAttachment> findByEmailLogId(Long emailLogId);
}

