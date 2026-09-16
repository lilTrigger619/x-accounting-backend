package com.unionsg.xaccounting.entity.settings;

import com.unionsg.xaccounting.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * The organization's profile - the source of truth for what appears on outbound documents
 * (invoices, quotes, receipts, statements, payslips, reports) (Settings & Setup §2). The
 * application is single-tenant today, so this table is a deliberate singleton: exactly one
 * row, fetched or created lazily by {@code OrganizationService}.
 */
@Entity
@Table(name = "organization")
@Getter
@Setter
public class Organization extends BaseEntity {

    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(name = "trading_name", length = 200)
    private String tradingName;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "tax_id", length = 100)
    private String taxId;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(length = 100)
    private String industry;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 200)
    private String website;

    @Column(name = "logo_file_id", length = 100)
    private String logoFileId;

    // Primary address
    @Column(name = "primary_address_line1", length = 200)
    private String primaryAddressLine1;
    @Column(name = "primary_address_line2", length = 200)
    private String primaryAddressLine2;
    @Column(name = "primary_city", length = 100)
    private String primaryCity;
    @Column(name = "primary_state", length = 100)
    private String primaryState;
    @Column(name = "primary_postal_code", length = 30)
    private String primaryPostalCode;
    @Column(name = "primary_country", length = 100)
    private String primaryCountry;

    // Billing address
    @Column(name = "billing_same_as_primary")
    private Boolean billingSameAsPrimary = true;
    @Column(name = "billing_address_line1", length = 200)
    private String billingAddressLine1;
    @Column(name = "billing_address_line2", length = 200)
    private String billingAddressLine2;
    @Column(name = "billing_city", length = 100)
    private String billingCity;
    @Column(name = "billing_state", length = 100)
    private String billingState;
    @Column(name = "billing_postal_code", length = 30)
    private String billingPostalCode;
    @Column(name = "billing_country", length = 100)
    private String billingCountry;

    // Shipping address
    @Column(name = "shipping_same_as_primary")
    private Boolean shippingSameAsPrimary = true;
    @Column(name = "shipping_address_line1", length = 200)
    private String shippingAddressLine1;
    @Column(name = "shipping_address_line2", length = 200)
    private String shippingAddressLine2;
    @Column(name = "shipping_city", length = 100)
    private String shippingCity;
    @Column(name = "shipping_state", length = 100)
    private String shippingState;
    @Column(name = "shipping_postal_code", length = 30)
    private String shippingPostalCode;
    @Column(name = "shipping_country", length = 100)
    private String shippingCountry;
}
