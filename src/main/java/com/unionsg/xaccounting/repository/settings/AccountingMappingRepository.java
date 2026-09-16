package com.unionsg.xaccounting.repository.settings;

import com.unionsg.xaccounting.entity.settings.AccountingMapping;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountingMappingRepository extends JpaRepository<AccountingMapping, Long> {

    Optional<AccountingMapping> findByMappingKey(MappingKey mappingKey);

    boolean existsByMappingKey(MappingKey mappingKey);
}
