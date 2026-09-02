package com.unionsg.xaccounting.dto.product;

import com.unionsg.xaccounting.enums.ProductItemType;
import com.unionsg.xaccounting.enums.TaxCategoryType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ProductResponse {

    private Long id;

    private String name;

    private ProductItemType itemType;

    private String category;

    private String costGroup;

    private String imageFileId;

    private String imageUrl;

    private String description;

    private BigDecimal price;

    private Long incomeAccountId;

    private String incomeAccountCode;

    private String incomeAccountName;

    private Long taxCategoryId;

    private String taxCategoryName;

    private TaxCategoryType taxCategoryType;

    private BigDecimal taxRate;

    private LocalDateTime createdAt;

}
