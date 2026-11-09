package com.unionsg.xaccounting.dto.journal;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateJournalLineRequest {
    @NotNull(message = "Account ID is required")
    private Long accountId;

    @Size(max = 500)
    private String description;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal debitAmount;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal creditAmount;

    /** Cost center this line is charged to; defaults to the account's home branch or the journal's. */
    private Long costCenterId;

    /** Document ID of one of the journal's attachments, e.g. "DOC-2". */
    @Size(max = 20, message = "A document ID can be at most 20 characters")
    private String documentRef;

    /** Optional tags; rejected unless subledger journals are turned on in settings. */
    private Long employeeId;
    /** Vendor (supplier) id. */
    private Long supplierId;
    private Long customerId;
    /** Bill this line is allocated to; its vendor fills in the vendor when that's empty. */
    private Long billId;
    /** Invoice this line is allocated to; its customer fills in the customer when that's empty. */
    private Long invoiceId;
}
