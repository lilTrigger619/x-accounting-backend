package com.unionsg.xaccounting.config.seeders.configSeeder;

import com.unionsg.xaccounting.entity.configuration.Config;
import com.unionsg.xaccounting.entity.configuration.ConfigItem;
import com.unionsg.xaccounting.repository.config.ConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Runs before the demo seeders, which create records whose fields are checked against configs. */
@Component
@Order(0)
@RequiredArgsConstructor
public class ConfigSeeder implements CommandLineRunner {

    private final ConfigRepository configRepository;
    private final ConfigSeedData configSeedData;

    @Override
    @Transactional
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

            Optional<Config> existing = configRepository.findByConfigKey(config.getConfigKey());

            if (existing.isEmpty()) {

                configRepository.save(config);

            } else {

                addMissingItems(existing.get(), config);

            }

        }

        System.out.println("Configs seeded successfully");

    }

    /**
     * Adds seed items introduced after a config was first created (e.g. new currencies), matched
     * by code, or by name for items without a code. Items users removed are only deactivated, so
     * they still match and are not brought back.
     */
    private void addMissingItems(Config existing, Config seed) {
        Set<String> known = existing.getItems().stream().map(ConfigSeeder::identity).collect(Collectors.toSet());
        List<ConfigItem> missing = seed.getItems().stream().filter(i -> !known.contains(identity(i))).toList();
        if (missing.isEmpty()) return;

        int nextOrder = existing.getItems().stream()
                .map(ConfigItem::getSortOrder).filter(Objects::nonNull).max(Integer::compare).orElse(0);
        for (ConfigItem item : missing) {
            item.setConfig(existing);
            item.setIsDefault(false);
            item.setSortOrder(++nextOrder);
            existing.getItems().add(item);
        }
        configRepository.save(existing);
    }

    private static String identity(ConfigItem item) {
        return item.getCode() != null && !item.getCode().isBlank() ? "code:" + item.getCode() : "name:" + item.getName();
    }

}