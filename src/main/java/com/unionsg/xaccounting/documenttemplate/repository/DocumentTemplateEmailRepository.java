package com.unionsg.xaccounting.documenttemplate.repository;

import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplate;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateEmail;
import com.unionsg.xaccounting.documenttemplate.enums.EmailType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentTemplateEmailRepository extends JpaRepository<DocumentTemplateEmail, Long> {

    Optional<DocumentTemplateEmail> findByTemplateAndEmailType(DocumentTemplate template, EmailType emailType);
}

