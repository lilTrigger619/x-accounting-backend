package com.unionsg.xaccounting.utils;

import com.unionsg.xaccounting.entity.configuration.Config;
import com.unionsg.xaccounting.entity.configuration.ConfigItem;

public class ConfigUtils {

    private void linkItems(Config config) {

        config.getItems()
                .forEach(item -> item.setConfig(config));

    }

    private ConfigItem item(
            String name,
            String code,
            String value,
            String description,
            Boolean isDefault,
            Integer order
    ) {

        return ConfigItem.builder()
                .name(name)
                .code(code)
                .value(value)
                .description(description)
                .isDefault(isDefault)
                .sortOrder(order)
                .build();

    }

}
