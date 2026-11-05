package com.unionsg.xaccounting.dto.journal;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class JournalLineResponse {

    private Long id;

    private Integer lineNumber;

    private Long accountId;

    private String accountCode;

    private String accountName;

    private String description;

    private BigDecimal debitAmount;

    private BigDecimal creditAmount;

    private Long branchId;
    private String branchCode;
    private String branchName;
    private Long costCenterId;
    private String costCenterCode;
    private String costCenterName;
    private Long counterpartyBranchId;
    private String counterpartyBranchName;
    private Boolean interBranch;

    /** The attachment this line points at; file id and name are empty when it isn't attached. */
    private String documentRef;
    private String documentFileId;
    private String documentName;

    private Long employeeId;
    private String employeeNumber;
    private String employeeName;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private Long billId;
    private String billNumber;
    private Long invoiceId;
    private String invoiceNumber;
}
