package com.unionsg.xaccounting.service.lookup;

import com.unionsg.xaccounting.dto.lookup.LookupDefinitionDto;
import com.unionsg.xaccounting.dto.lookup.LookupOptionDto;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LookupServiceTest {

    private final LookupService service = new LookupService();

    @Test
    void humanizesEnumNamesIntoLabels() {
        LookupDefinitionDto lookup = service.get("bill-statuses");

        assertThat(lookup.getOptions())
                .extracting(LookupOptionDto::getValue, LookupOptionDto::getLabel)
                .contains(org.assertj.core.groups.Tuple.tuple("PARTIALLY_PAID", "Partially Paid"));
    }

    @Test
    void usesTheEnumsOwnLabelWhenItHasOne() {
        LookupDefinitionDto lookup = service.get("customer-types");

        assertThat(lookup.getOptions())
                .extracting(LookupOptionDto::getLabel)
                .containsExactly("Individual", "Business");
    }

    @Test
    void rejectsUnknownKeys() {
        assertThatThrownBy(() -> service.get("not-a-lookup"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listsEveryRegisteredLookup() {
        assertThat(service.getAll()).extracting(LookupDefinitionDto::getKey)
                .contains("journal-types", "payment-methods", "tax-category-types");
    }
}
