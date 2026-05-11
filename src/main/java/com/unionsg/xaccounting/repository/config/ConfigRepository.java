package com.unionsg.xaccounting.repository.config;

import com.unionsg.xaccounting.entity.configuration.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigRepository extends JpaRepository<Config, String> {

    Optional<Config> findByConfigKey(String configKey);

    List<Config> findByStatusOrderBySortOrderAsc(String status);

}