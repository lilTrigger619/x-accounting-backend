package com.unionsg.xaccounting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import com.unionsg.xaccounting.entity.FileEntity;


public interface FileRepository extends JpaRepository<FileEntity, String>, JpaSpecificationExecutor<FileEntity> {

   Optional<FileEntity> findByIdAndIsDeletedFalse(String id);
}
