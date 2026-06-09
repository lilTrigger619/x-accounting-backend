package com.unionsg.xaccounting.repository;


import com.unionsg.xaccounting.entity.DocumentSequence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentSequenceRepository
        extends JpaRepository<DocumentSequence, Long> {

    Optional<DocumentSequence> findByCode(String code);
}