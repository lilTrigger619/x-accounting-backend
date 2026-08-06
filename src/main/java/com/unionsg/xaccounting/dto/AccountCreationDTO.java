package com.unionsg.xaccounting.dto;

import java.time.LocalDateTime;
import java.util.Optional;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
public class AccountCreationDTO {
    private String accountId;
    private String accountName;
    private Long chartCode;
    private String clearsTo;
    //    private String restriction;
//    private String postingLevel;
//    private String levelId;
    private String currency;
    //    private String statementType;
//    private String statementCode;
//    private String societyId;
//    private String circuitId;
//    private String postedBy;
//    private String approvedBy;
//    private String approvedStatus;
    private Boolean deleted;
    //private LocalDateTime dateCreated;

    //private Long openingBalance;
    private String openingBalance;
    private String openingBalanceDate;
    //private double taxRate;
    private String taxRate;
    private String createdBy;

    private Long coaClearToId;
    private String description;
    private LocalDateTime dateCreated;
//    private int accountType ;
//    private int subAccountType ;
}
**/


public class AccountCreationDTO {
    private String accountId;
    private String accountName;
    private String ChartCode;
    private String clearsTo;
    private String currency;
    private String defaultTaxRate;
    private String description;
    @Builder.Default
    private Optional<String> openingBalance = Optional.empty();
    @Builder.Default
    private Optional<String> openingBalanceDate = Optional.empty();
    // private String statementCode;
    // private String statementType;
}
