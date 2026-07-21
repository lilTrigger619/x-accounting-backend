package com.unionsg.xaccounting.document.repository;

import com.unionsg.xaccounting.document.entity.GeneratedDocument;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, Long> {

    List<GeneratedDocument> findByDocumentTypeAndEntityIdOrderByGeneratedAtDesc(DocumentType documentType, Long entityId);
}

