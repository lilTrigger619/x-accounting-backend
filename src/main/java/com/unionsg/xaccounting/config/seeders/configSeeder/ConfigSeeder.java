package com.unionsg.xaccounting.config.seeders.configSeeder;

import com.unionsg.xaccounting.entity.configuration.Config;
import com.unionsg.xaccounting.repository.config.ConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConfigSeeder implements CommandLineRunner {

    private final ConfigRepository configRepository;
    private final ConfigSeedData configSeedData;

    @Override
    public void run(String... args) {

        seedConfigs();

    }

    private void seedConfigs() {

        if (configRepository.count() > 0) {
            return;
        }

        List<Config> configs = configSeedData.getConfigs();
        for (Config config : configs) {

            boolean exists = configRepository
                    .findByConfigKey(config.getConfigKey())
                    .isPresent();

            if (!exists) {

                configRepository.save(config);

            }

        }
        configRepository.saveAll(configs);

        System.out.println("Configs seeded successfully");

    }

}