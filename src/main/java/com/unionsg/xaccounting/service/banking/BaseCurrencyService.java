package com.unionsg.xaccounting.service.banking;

import com.unionsg.xaccounting.dto.config.ConfigDto;
import com.unionsg.xaccounting.dto.config.ConfigItemDto;
import com.unionsg.xaccounting.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * The organization's accounting currency: the item marked default in the Currencies
 * configuration (the same thing the setup checklist calls "Base currency"). Falls back to the
 * journal entity's own default when nothing is marked.
 */
@Service
@RequiredArgsConstructor
public class BaseCurrencyService {

    static final String FALLBACK_CURRENCY = "GHS";

    private final ConfigService configService;

    @Transactional(readOnly = true)
    public String resolve() {
        try {
            ConfigDto currencies = configService.getConfigByKey("currencies");
            if (currencies != null && currencies.getItems() != null) {
                return currencies.getItems().stream()
                        .filter(item -> Boolean.TRUE.equals(item.getIsDefault()))
                        .map(ConfigItemDto::getCode)
                        .filter(code -> code != null && !code.isBlank())
                        .map(code -> code.trim().toUpperCase(Locale.ROOT))
                        .findFirst()
                        .orElse(FALLBACK_CURRENCY);
            }
        } catch (RuntimeException ignored) {
            // No currencies configuration yet.
        }
        return FALLBACK_CURRENCY;
    }
}
