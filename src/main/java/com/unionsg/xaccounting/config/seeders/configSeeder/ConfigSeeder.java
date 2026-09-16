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

    /**
     * Seeds each system-defined config category independently rather than short-circuiting the
     * whole method once any config exists. The previous {@code configRepository.count() > 0}
     * guard meant that once {@code CompanyConfigSeeder} inserted the unrelated "COMPANY" config,
     * this seeder considered the database "already seeded" and silently skipped Payment Types,
     * Item Categories, Expense Categories and the other five categories forever - they never
     * existed on this database at all, which is why their settings screens 404'd.
     */
    private void seedConfigs() {

        List<Config> configs = configSeedData.getConfigs();
        for (Config config : configs) {

            boolean exists = configRepository
                    .findByConfigKey(config.getConfigKey())
                    .isPresent();

            if (!exists) {

                configRepository.save(config);

            }

        }

        System.out.println("Configs seeded successfully");

    }

}