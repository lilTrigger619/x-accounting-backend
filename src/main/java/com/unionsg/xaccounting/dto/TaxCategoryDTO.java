package com.unionsg.xaccounting.dto;

import com.unionsg.xaccounting.enums.TaxCategoryType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A tax rate as sent to and returned by {@code /api/tax-categories}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxCategoryDTO {
    private Long id;
    private String name;
    private String code;
    private TaxCategoryType type;
    private BigDecimal rate;
    private String description;
    /** Code of a "tax-authorities" config item. */
    private String authority;
    private String registrationNumber;
    /** Code of a "filing-frequencies" config item. */
    private String filingFrequency;
    private String reportingCode;
    /** Code of a "tax-applies-to" config item. */
    private String appliesTo;
    /** Code of a "tax-transaction-types" config item. */
    private String transactionType;
    private Long linkedAccountId;
    private String linkedAccountCode;
    private String linkedAccountName;
    /** Code of a "tax-groups" config item. */
    private String taxGroup;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean active;
    private Boolean compound;
    private Boolean recoverable;
    private Boolean defaultForNewItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
