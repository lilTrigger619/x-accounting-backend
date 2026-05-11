package com.unionsg.xaccounting.MapperLayer;


import com.unionsg.xaccounting.dto.config.ConfigDto;
import com.unionsg.xaccounting.dto.config.ConfigItemDto;
import com.unionsg.xaccounting.entity.configuration.Config;
import com.unionsg.xaccounting.entity.configuration.ConfigItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConfigMapper {

    public ConfigDto toDto(Config config) {
//        List<ConfigItem> = config.getItems().si
        List<ConfigItem> configItems = config.getItems();
        return ConfigDto.builder()
                .key(config.getConfigKey())
                .title(config.getTitle())
                .description(config.getDescription())
                .itemLabel(config.getItemLabel())
                .showValueField(config.getShowValueField())
                .valueFieldLabel(config.getValueFieldLabel())
                .valueFieldPlaceholder(config.getValueFieldPlaceholder())
                .items(
                        configItems != null ?
                        config.getItems()
                                .stream()
                                .filter(i-> "ACTIVE".equals(i.getStatus()))
                                .map(this::toItemDto)
                                .toList(): null
                )
                .build();
    }


    public ConfigItemDto toItemDto(ConfigItem item) {

        return ConfigItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .code(item.getCode())
                .value(item.getValue())
                .description(item.getDescription())
                .isDefault(item.getIsDefault())
                .status(item.getStatus())
                .build();

    }

}