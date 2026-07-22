package com.unionsg.xaccounting.documenttemplate.repository;

import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplate;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, Long> {

    Optional<DocumentTemplate> findByDocumentTypeAndIsDefaultTrue(DocumentType documentType);

    boolean existsByNameAndDocumentType(String name, DocumentType documentType);

    List<DocumentTemplate> findByDocumentType(DocumentType documentType);

    Page<DocumentTemplate> findByDocumentType(DocumentType documentType, Pageable pageable);

    @Modifying
    @Query("UPDATE DocumentTemplate t SET t.isDefault = false WHERE t.documentType = :documentType AND t.id <> :excludeId")
    void clearDefaultForOtherTemplates(@Param("documentType") DocumentType documentType, @Param("excludeId") Long excludeId);

    boolean existsByDocumentTypeAndIsDefaultTrue(DocumentType documentType);
}

