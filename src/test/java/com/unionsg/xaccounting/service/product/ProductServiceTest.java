package com.unionsg.xaccounting.service.product;

import com.unionsg.xaccounting.dto.product.CreateProductRequest;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.enums.ProductItemType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.product.ProductRepository;
import com.unionsg.xaccounting.service.FileService.FileService;
import com.unionsg.xaccounting.service.TaxCategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TaxCategoryService taxCategoryService;

    @Mock
    private FileService fileService;

    @InjectMocks
    private ProductService service;

    private static CreateProductRequest request() {
        CreateProductRequest r = new CreateProductRequest();
        r.setName("Consulting");
        r.setItemType(ProductItemType.SERVICE);
        r.setPrice(new BigDecimal("100"));
        return r;
    }

    @Test
    void createRejectsBlankName() {
        CreateProductRequest r = request();
        r.setName("  ");
        assertThatThrownBy(() -> service.createProduct(null, r))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Product name is required");
        verify(productRepository, never()).save(any());
    }

    @Test
    void createRejectsMissingItemType() {
        CreateProductRequest r = request();
        r.setItemType(null);
        assertThatThrownBy(() -> service.createProduct(null, r))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Item type is required");
    }

    @Test
    void createRejectsNegativePrice() {
        CreateProductRequest r = request();
        r.setPrice(new BigDecimal("-1"));
        assertThatThrownBy(() -> service.createProduct(null, r))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Price can't be negative");
    }

    @Test
    void createRejectsDeletedIncomeAccount() {
        CreateProductRequest r = request();
        r.setIncomeAccountId(7L);
        AccountEntity deleted = new AccountEntity();
        deleted.setDeleted(true);
        when(accountRepository.findById(7L)).thenReturn(Optional.of(deleted));
        assertThatThrownBy(() -> service.createProduct(null, r))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Income account not found");
        verify(productRepository, never()).save(any());
    }
}
