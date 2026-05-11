package com.unionsg.xaccounting.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.unionsg.xaccounting.entity.FileEntity;
import java.util.UUID;
import java.util.List;
import com.unionsg.xaccounting.enums.EntityType;


public interface FileRepository extends JpaRepository<FileEntity, Long> {
   Page<FileEntity> findByEntityTypeAndEntityIdAndIsDeletedFalse(
           EntityType entityType,
           UUID entityId,
           Pageable pageable
   );

   List<FileEntity> findByEntityTypeAndEntityIdAndIsDeletedFalse(
           EntityType entityType,
           UUID entityId
   );

   Optional<FileEntity> findByIdAndIsDeletedFalse(UUID id);
}
