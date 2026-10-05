package com.unionsg.xaccounting.service.config;

import com.unionsg.xaccounting.entity.configuration.Config;
import com.unionsg.xaccounting.entity.configuration.ConfigItem;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.config.ConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ConfigValueValidatorTest {

    @Mock
    private ConfigRepository configRepository;

    @InjectMocks
    private ConfigValueValidator validator;

    @BeforeEach
    void setUp() {
        Config config = Config.builder().configKey("tax-authorities").build();
        config.setItems(List.of(
                ConfigItem.builder().name("Ghana Revenue Authority").code("GRA").status("ACTIVE").build(),
                ConfigItem.builder().name("Old Agency").code("OLD").status("INACTIVE").build(),
                ConfigItem.builder().name("No Code Agency").status("ACTIVE").build()
        ));
        lenient().when(configRepository.findByConfigKey("tax-authorities")).thenReturn(Optional.of(config));
    }

    @Test
    void acceptsAnActiveItemsCodeOrItsNameWhenItHasNoCode() {
        assertThat(validator.validate("tax-authorities", " GRA ", null, "Tax authority")).isEqualTo("GRA");
        assertThat(validator.validate("tax-authorities", "No Code Agency", null, "Tax authority")).isEqualTo("No Code Agency");
    }

    @Test
    void rejectsUnknownAndInactiveValues() {
        assertThatThrownBy(() -> validator.validate("tax-authorities", "XYZ", null, "Tax authority"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not a valid tax authority");
        assertThatThrownBy(() -> validator.validate("tax-authorities", "OLD", null, "Tax authority"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void keepsAnUnchangedValueEvenIfItsItemWasDeactivated() {
        assertThat(validator.validate("tax-authorities", "OLD", "OLD", "Tax authority")).isEqualTo("OLD");
    }

    @Test
    void blankIsNullUnlessRequired() {
        assertThat(validator.validate("tax-authorities", " ", null, "Tax authority")).isNull();
        assertThatThrownBy(() -> validator.require("tax-authorities", "", null, "Tax authority"))
                .hasMessageContaining("Tax authority is required");
    }
}
