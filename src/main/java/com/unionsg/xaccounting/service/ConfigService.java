package com.unionsg.xaccounting.service;


import com.unionsg.xaccounting.MapperLayer.ConfigMapper;
import com.unionsg.xaccounting.dto.config.ConfigDto;
import com.unionsg.xaccounting.dto.config.ConfigItemDto;
import com.unionsg.xaccounting.dto.config.UpsertConfigDto;
import com.unionsg.xaccounting.dto.config.UpsertConfigItemDto;
import com.unionsg.xaccounting.entity.configuration.Config;
import com.unionsg.xaccounting.entity.configuration.ConfigItem;
import com.unionsg.xaccounting.repository.config.ConfigItemRepository;
import com.unionsg.xaccounting.repository.config.ConfigRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final ConfigRepository configRepository;
    private final ConfigMapper configMapper;
    private final ConfigItemRepository configItemRepository;


    public List<ConfigDto> getAllConfigs() {

        return configRepository
                .findByStatusOrderBySortOrderAsc("ACTIVE")
                .stream()
                .map(configMapper::toDto)
                .toList();

    }


    public ConfigDto getConfigByKey(String key) {

        Config config = configRepository
                .findByConfigKey(key)
                .orElseThrow(() ->
                        new RuntimeException("Config not found: " + key)
                );

        return configMapper.toDto(config);

    }

    public ConfigDto createConfig(UpsertConfigDto request){
        if (configRepository.findByConfigKey(request.getKey()).isPresent()){
            throw new RuntimeException("Config already exists");
        }

        Config config = Config.builder()
                .configKey(request.getKey())
                .title(request.getTitle())
                .description(request.getDescription())
                .itemLabel(request.getItemLabel())
                .showValueField(request.getShowValueField())
                .valueFieldLabel(request.getValueFieldLabel())
                .valueFieldPlaceholder(request.getValueFieldPlaceholder())
                .systemDefined(false)
                .build();

        return configMapper.toDto(configRepository.save(config));
    }

    public ConfigDto updateConfig(String key, UpsertConfigDto request){
        Config config = configRepository.findByConfigKey(key)
                .orElseThrow(()->new RuntimeException("Config not found"));

        if (Boolean.TRUE.equals(config.getSystemDefined())) {
            throw new RuntimeException("System config cannot be modified");
        }

        config.setTitle(request.getTitle());
        config.setDescription(request.getDescription());
        config.setItemLabel(request.getItemLabel());
        config.setShowValueField(request.getShowValueField());
        config.setValueFieldLabel(request.getValueFieldLabel());
        config.setValueFieldPlaceholder(request.getValueFieldPlaceholder());

        return configMapper.toDto(configRepository.save(config));
    }


    @Transactional
    public void deleteConfig(String key) {

        Config config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new RuntimeException("Config not found"));

        if (Boolean.TRUE.equals(config.getSystemDefined())) {
            throw new RuntimeException("System config cannot be deleted");
        }

        config.setStatus("INACTIVE");

        configRepository.save(config);
    }

    @Transactional
    public ConfigItemDto updateItem(String itemId, UpsertConfigItemDto request) {

        ConfigItem item = configItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        item.setName(request.getName());
        item.setCode(request.getCode());
        item.setValue(request.getValue());
        item.setDescription(request.getDescription());

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(item.getConfig());
            item.setIsDefault(true);
        }

        return configMapper.toItemDto(configItemRepository.save(item));
    }

    @Transactional
    public void deleteItem(String itemId){
        ConfigItem item = configItemRepository.findById(itemId)
                .orElseThrow(()-> new RuntimeException("Item not found"));
        item.setStatus("INACTIVE");
        configItemRepository.save(item);
    }


    private void clearDefault(Config config) {

        config.getItems().forEach(i -> i.setIsDefault(false));

    }


    @Transactional
    public ConfigItemDto addItem(String key, UpsertConfigItemDto request) {

        Config config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new RuntimeException("Config not found"));

        // check if config item exists
        boolean exists = configItemRepository.existsByConfigIdAndCode(config.getId(), request.getCode());
        if (exists) {
            throw new RuntimeException("Config item already exits with same code!");
        }
        ConfigItem item = ConfigItem.builder()
                .name(request.getName())
                .code(request.getCode())
                .value(request.getValue())
                .description(request.getDescription())
                .isDefault(request.getIsDefault())
                .config(config)
                .build();

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(config);
        }

        return configMapper.toItemDto(configItemRepository.save(item));
    }


}