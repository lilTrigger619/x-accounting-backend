package com.unionsg.xaccounting.dto;

import com.unionsg.xaccounting.enums.TaxCategoryType;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxCategoryDTO {
    private Long id;
    private String name;
    private TaxCategoryType type;
    private BigDecimal rate;
}
