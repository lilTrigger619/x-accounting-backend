package com.unionsg.xaccounting.service.config;

import com.unionsg.xaccounting.entity.configuration.ConfigItem;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.config.ConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Checks that a field stored as a configuration value (e.g. a tax authority or a title) names
 * an active item of that configuration. Items are stored by code, or by name when they have no
 * code, which is how the frontend's config pickers write them.
 */
@Component
@RequiredArgsConstructor
public class ConfigValueValidator {

    private final ConfigRepository configRepository;

    /**
     * Returns the trimmed value, or null when blank. Throws when the value is not an active item
     * of the configuration. An unchanged value is accepted as is, so records keep values whose
     * item was later deactivated.
     */
    @Transactional(readOnly = true)
    public String validate(String configKey, String value, String previous, String fieldLabel) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        if (Objects.equals(v, previous)) return v;

        boolean found = configRepository.findByConfigKey(configKey)
                .map(c -> c.getItems().stream().anyMatch(i -> isActive(i) && v.equals(storedValue(i))))
                .orElse(false);
        if (!found) {
            throw new BusinessException("\"" + v + "\" is not a valid " + fieldLabel.toLowerCase() + ". Pick one from the list or add it first.");
        }
        return v;
    }

    /** Same as {@link #validate} but the value is required. */
    public String require(String configKey, String value, String previous, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(fieldLabel + " is required");
        }
        return validate(configKey, value, previous, fieldLabel);
    }

    /** The stored value of the configuration's active default item, or null when it has none. */
    @Transactional(readOnly = true)
    public String defaultValue(String configKey) {
        return configRepository.findByConfigKey(configKey)
                .flatMap(c -> c.getItems().stream()
                        .filter(i -> isActive(i) && Boolean.TRUE.equals(i.getIsDefault()))
                        .findFirst())
                .map(ConfigValueValidator::storedValue)
                .orElse(null);
    }

    private static boolean isActive(ConfigItem item) {
        return item.getStatus() == null || "ACTIVE".equalsIgnoreCase(item.getStatus());
    }

    private static String storedValue(ConfigItem item) {
        return item.getCode() != null && !item.getCode().isBlank() ? item.getCode() : item.getName();
    }
}
