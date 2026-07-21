package com.unionsg.xaccounting.documenttemplate.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_template_contents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentTemplateContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private DocumentTemplate template;

    @Column(name = "form_title", length = 200)
    private String formTitle;

    @Column(name = "show_company_name", nullable = false)
    private boolean showCompanyName = true;

    @Column(name = "show_phone", nullable = false)
    private boolean showPhone = true;

    @Column(name = "show_email", nullable = false)
    private boolean showEmail = true;

    @Column(name = "show_website", nullable = false)
    private boolean showWebsite = true;

    @Column(name = "show_address", nullable = false)
    private boolean showAddress = true;

    @Column(name = "show_billing_address", nullable = false)
    private boolean showBillingAddress = true;

    @Column(name = "show_shipping_address", nullable = false)
    private boolean showShippingAddress = true;

    @Column(name = "show_terms", nullable = false)
    private boolean showTerms = true;

    @Column(name = "show_due_date", nullable = false)
    private boolean showDueDate = true;

    @Column(name = "show_payment_method", nullable = false)
    private boolean showPaymentMethod = true;
}

