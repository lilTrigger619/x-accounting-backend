package com.unionsg.xaccounting.repository.config;


import com.unionsg.xaccounting.entity.configuration.ConfigItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConfigItemRepository extends JpaRepository<ConfigItem, String> {

    List<ConfigItem> findByConfigIdOrderBySortOrderAsc(String configId);

    Boolean existsByConfigIdAndCode(String configId, String configCode);

}