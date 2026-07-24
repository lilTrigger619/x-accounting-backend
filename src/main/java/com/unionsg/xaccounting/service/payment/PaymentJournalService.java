package com.unionsg.xaccounting.service.payment;

import com.unionsg.xaccounting.entity.payment.PaymentAllocationEntity;
import com.unionsg.xaccounting.entity.payment.PaymentEntity;
import com.unionsg.xaccounting.entity.payment.PaymentRefundEntity;

public interface PaymentJournalService {

    /**
     * Post journal entries for a received payment.
     * Debit Bank, Credit Accounts Receivable (allocated) + Customer Advances (unallocated).
     */
    void postPaymentJournal(PaymentEntity payment);

    /**
     * Post journal entry for an additional allocation against an existing unallocated payment.
     * Debit Customer Advances, Credit Accounts Receivable.
     */
    void postAdditionalAllocationJournal(PaymentEntity payment, PaymentAllocationEntity allocation);

    /**
     * Post journal entry for removing an allocation.
     * Debit Accounts Receivable, Credit Customer Advances.
     */
    void postRemoveAllocationJournal(PaymentEntity payment, PaymentAllocationEntity allocation);

    /**
     * Post journal entry for a refund.
     * Debit Accounts Receivable (or Customer Advances), Credit Bank.
     */
    void postRefundJournal(PaymentEntity payment, PaymentRefundEntity refund);

    /**
     * Reverse the original payment journal (cancellation).
     */
    void postCancellationJournal(PaymentEntity payment);
}
