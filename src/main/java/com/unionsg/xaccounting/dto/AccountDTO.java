package com.unionsg.xaccounting.dto;
import lombok.*;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class AccountDTO {
    private Long id;
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
}
