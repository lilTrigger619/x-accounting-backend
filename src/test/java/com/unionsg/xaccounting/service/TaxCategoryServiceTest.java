package com.unionsg.xaccounting.service;

import com.unionsg.xaccounting.dto.TaxCategoryDTO;
import com.unionsg.xaccounting.entity.TaxCategory;
import com.unionsg.xaccounting.enums.TaxCategoryType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.TaxCategoryRepository;
import com.unionsg.xaccounting.repository.product.ProductRepository;
import com.unionsg.xaccounting.service.config.ConfigValueValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxCategoryServiceTest {

    @Mock
    private TaxCategoryRepository repository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ConfigValueValidator configValues;

    @InjectMocks
    private TaxCategoryService service;

    private static TaxCategoryDTO request() {
        return TaxCategoryDTO.builder()
                .name(" VAT Standard ")
                .code("VAT-STD")
                .type(TaxCategoryType.VAT)
                .rate(new BigDecimal("15"))
                .authority("GRA")
                .appliesTo("BOTH")
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();
    }

    private static TaxCategory existing(long id, boolean active) {
        TaxCategory t = TaxCategory.builder().id(id).name("VAT").code("VAT").type(TaxCategoryType.VAT)
                .rate(BigDecimal.TEN).active(active).build();
        return t;
    }

    private void passConfigValues() {
        lenient().when(configValues.require(anyString(), any(), any(), anyString())).thenAnswer(inv -> inv.getArgument(1));
        lenient().when(configValues.validate(anyString(), any(), any(), anyString())).thenAnswer(inv -> inv.getArgument(1));
    }

    @Test
    void createTrimsTheNameAndStartsActive() {
        passConfigValues();
        when(repository.save(any(TaxCategory.class))).thenAnswer(inv -> {
            TaxCategory t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });

        TaxCategoryDTO saved = service.create(request());

        assertThat(saved.getName()).isEqualTo("VAT Standard");
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getAuthority()).isEqualTo("GRA");
    }

    @Test
    void createRejectsADuplicateCode() {
        when(repository.existsByCodeIgnoreCaseAndDeletedFalse("VAT-STD")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("code \"VAT-STD\" already exists");
    }

    @Test
    void createRejectsARateAbove100() {
        TaxCategoryDTO dto = request();
        dto.setRate(new BigDecimal("101"));

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("between 0 and 100");
    }

    @Test
    void createRejectsAnEndDateBeforeTheStart() {
        TaxCategoryDTO dto = request();
        dto.setEffectiveTo(LocalDate.of(2025, 12, 31));

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be before");
    }

    @Test
    void makingARateTheDefaultClearsThePreviousDefault() {
        passConfigValues();
        TaxCategory previous = existing(1L, true);
        previous.setDefaultForNewItems(true);
        when(repository.save(any(TaxCategory.class))).thenAnswer(inv -> {
            TaxCategory t = inv.getArgument(0);
            if (t.getId() == null) t.setId(2L);
            return t;
        });
        when(repository.findByDeletedFalseAndDefaultForNewItemsTrue()).thenReturn(List.of(previous));

        TaxCategoryDTO dto = request();
        dto.setDefaultForNewItems(true);
        service.create(dto);

        assertThat(previous.getDefaultForNewItems()).isFalse();
    }

    @Test
    void deleteRefusesARateUsedByProducts() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing(1L, true)));
        when(productRepository.existsByTaxCategoryIdAndDeletedFalse(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Deactivate it instead");
        verify(repository, never()).save(any());
    }

    @Test
    void deleteSoftDeletesAnUnusedRate() {
        TaxCategory rate = existing(1L, true);
        when(repository.findById(1L)).thenReturn(Optional.of(rate));
        when(productRepository.existsByTaxCategoryIdAndDeletedFalse(1L)).thenReturn(false);

        service.delete(1L);

        assertThat(rate.isDeleted()).isTrue();
        assertThat(rate.getDeletedAt()).isNotNull();
    }

    @Test
    void productsCannotNewlyPickAnInactiveRateButKeepTheirOwn() {
        when(repository.findById(3L)).thenReturn(Optional.of(existing(3L, false)));

        assertThatThrownBy(() -> service.getUsable(3L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");
        assertThat(service.getUsable(3L, 3L).getId()).isEqualTo(3L);
    }

    @Test
    void listCanHideInactiveRates() {
        when(repository.findByDeletedFalse()).thenReturn(List.of(existing(1L, true), existing(2L, false)));

        assertThat(service.getAll(true)).extracting(TaxCategoryDTO::getId).containsExactly(1L);
        assertThat(service.getAll(false)).hasSize(2);
    }

    @Test
    void authorityIsCheckedAgainstTheConfig() {
        when(configValues.require(eq("tax-authorities"), eq("XYZ"), isNull(), anyString()))
                .thenThrow(new BusinessException("\"XYZ\" is not a valid tax authority."));
        TaxCategoryDTO dto = request();
        dto.setAuthority("XYZ");

        assertThatThrownBy(() -> service.create(dto)).hasMessageContaining("not a valid tax authority");
    }
}
