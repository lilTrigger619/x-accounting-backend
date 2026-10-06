package com.unionsg.xaccounting.service.analytics;

import com.unionsg.xaccounting.dto.analytics.AlertSettingDto;
import com.unionsg.xaccounting.dto.analytics.UpdateAlertSettingRequest;
import com.unionsg.xaccounting.entity.analytics.AnalyticsAlertSetting;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.analytics.AnalyticsAlertSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AlertSettingsService {

    private final AnalyticsAlertSettingRepository repository;

    /** Effective setting for one alert: the saved row, or the defaults when nothing is saved. */
    public record Effective(AlertDefinition definition, boolean enabled, BigDecimal threshold, Integer windowDays) {
    }

    @Transactional(readOnly = true)
    public Effective effective(AlertDefinition def) {
        return repository.findByAlertKey(def.name())
                .map(s -> new Effective(def, s.isEnabled(),
                        s.getThreshold() != null ? s.getThreshold() : def.defaultThreshold(),
                        s.getWindowDays() != null ? s.getWindowDays() : def.defaultWindowDays()))
                .orElse(new Effective(def, true, def.defaultThreshold(), def.defaultWindowDays()));
    }

    @Transactional(readOnly = true)
    public List<AlertSettingDto> list() {
        return Arrays.stream(AlertDefinition.values()).map(this::toDto).toList();
    }

    @Transactional
    public AlertSettingDto update(String key, UpdateAlertSettingRequest req) {
        AlertDefinition def;
        try {
            def = AlertDefinition.valueOf(key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Unknown alert: " + key);
        }
        if (req.threshold() != null && req.threshold().signum() <= 0) {
            throw new BusinessException("The threshold must be greater than zero.");
        }
        if (req.windowDays() != null && (req.windowDays() < 1 || req.windowDays() > 366)) {
            throw new BusinessException("The window must be between 1 and 366 days.");
        }
        if (req.windowDays() != null && def.defaultWindowDays() == null) {
            throw new BusinessException("This alert does not use a window.");
        }
        AnalyticsAlertSetting s = repository.findByAlertKey(def.name()).orElseGet(() -> {
            AnalyticsAlertSetting n = new AnalyticsAlertSetting();
            n.setAlertKey(def.name());
            n.setThreshold(def.defaultThreshold());
            n.setWindowDays(def.defaultWindowDays());
            return n;
        });
        if (req.enabled() != null) s.setEnabled(req.enabled());
        if (req.threshold() != null) s.setThreshold(req.threshold());
        if (req.windowDays() != null) s.setWindowDays(req.windowDays());
        s.setUpdatedBy(currentUser());
        repository.save(s);
        return toDto(def);
    }

    private AlertSettingDto toDto(AlertDefinition def) {
        Effective e = effective(def);
        AnalyticsAlertSetting saved = repository.findByAlertKey(def.name()).orElse(null);
        return new AlertSettingDto(def.name(), def.title(), def.description(), e.enabled(), e.threshold(), def.unit(),
                e.windowDays(), def.defaultThreshold(), def.defaultWindowDays(), def.available(),
                saved != null ? saved.getUpdatedBy() : null, saved != null ? saved.getUpdatedAt() : null);
    }

    private static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}
