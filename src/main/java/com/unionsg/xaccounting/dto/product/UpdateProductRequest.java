package com.unionsg.xaccounting.dto.product;

import com.unionsg.xaccounting.enums.ProductItemType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateProductRequest {

    private String name;

    private ProductItemType itemType;

    private String category;

    private String costGroup;

    private String description;

    private BigDecimal price;

    private Long incomeAccountId;

    private Long taxCategoryId;

    /** When true and no new image is uploaded alongside this request, the existing image is removed. */
    private boolean removeImage;

}
