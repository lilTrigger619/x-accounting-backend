package com.unionsg.xaccounting.service.settings;

import com.unionsg.xaccounting.dto.settings.OrganizationResponse;
import com.unionsg.xaccounting.dto.settings.UpdateOrganizationRequest;
import com.unionsg.xaccounting.entity.settings.Organization;
import com.unionsg.xaccounting.enums.settings.SettingType;
import com.unionsg.xaccounting.repository.settings.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * The organization profile is a deliberate singleton (Settings & Setup §2): the application is
 * single-tenant today, so there is exactly one row, created with sensible blank defaults the
 * first time anything asks for it.
 */
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository repository;
    private final SettingsAuditLogService auditLogService;

    @Transactional
    public Organization getOrCreate() {
        return repository.findAll().stream().findFirst().orElseGet(() -> repository.save(new Organization()));
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get() {
        return toResponse(getOrCreate());
    }

    @Transactional
    public OrganizationResponse update(UpdateOrganizationRequest request) {
        Organization org = getOrCreate();

        recordIfChanged("legalName", org.getLegalName(), request.getLegalName());
        recordIfChanged("taxId", org.getTaxId(), request.getTaxId());
        recordIfChanged("registrationNumber", org.getRegistrationNumber(), request.getRegistrationNumber());

        org.setLegalName(request.getLegalName());
        org.setTradingName(request.getTradingName());
        org.setRegistrationNumber(request.getRegistrationNumber());
        org.setTaxId(request.getTaxId());
        org.setBusinessType(request.getBusinessType());
        org.setIndustry(request.getIndustry());
        org.setDescription(request.getDescription());
        org.setPhone(request.getPhone());
        org.setEmail(request.getEmail());
        org.setWebsite(request.getWebsite());
        org.setLogoFileId(request.getLogoFileId());

        org.setPrimaryAddressLine1(request.getPrimaryAddressLine1());
        org.setPrimaryAddressLine2(request.getPrimaryAddressLine2());
        org.setPrimaryCity(request.getPrimaryCity());
        org.setPrimaryState(request.getPrimaryState());
        org.setPrimaryPostalCode(request.getPrimaryPostalCode());
        org.setPrimaryCountry(request.getPrimaryCountry());

        org.setBillingSameAsPrimary(request.getBillingSameAsPrimary());
        org.setBillingAddressLine1(request.getBillingAddressLine1());
        org.setBillingAddressLine2(request.getBillingAddressLine2());
        org.setBillingCity(request.getBillingCity());
        org.setBillingState(request.getBillingState());
        org.setBillingPostalCode(request.getBillingPostalCode());
        org.setBillingCountry(request.getBillingCountry());

        org.setShippingSameAsPrimary(request.getShippingSameAsPrimary());
        org.setShippingAddressLine1(request.getShippingAddressLine1());
        org.setShippingAddressLine2(request.getShippingAddressLine2());
        org.setShippingCity(request.getShippingCity());
        org.setShippingState(request.getShippingState());
        org.setShippingPostalCode(request.getShippingPostalCode());
        org.setShippingCountry(request.getShippingCountry());

        return toResponse(repository.save(org));
    }

    private void recordIfChanged(String field, String oldValue, String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            auditLogService.record(SettingType.ORGANIZATION, field, oldValue, newValue, null);
        }
    }

    private OrganizationResponse toResponse(Organization org) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .legalName(org.getLegalName())
                .tradingName(org.getTradingName())
                .registrationNumber(org.getRegistrationNumber())
                .taxId(org.getTaxId())
                .businessType(org.getBusinessType())
                .industry(org.getIndustry())
                .description(org.getDescription())
                .phone(org.getPhone())
                .email(org.getEmail())
                .website(org.getWebsite())
                .logoFileId(org.getLogoFileId())
                .primaryAddressLine1(org.getPrimaryAddressLine1())
                .primaryAddressLine2(org.getPrimaryAddressLine2())
                .primaryCity(org.getPrimaryCity())
                .primaryState(org.getPrimaryState())
                .primaryPostalCode(org.getPrimaryPostalCode())
                .primaryCountry(org.getPrimaryCountry())
                .billingSameAsPrimary(org.getBillingSameAsPrimary())
                .billingAddressLine1(org.getBillingAddressLine1())
                .billingAddressLine2(org.getBillingAddressLine2())
                .billingCity(org.getBillingCity())
                .billingState(org.getBillingState())
                .billingPostalCode(org.getBillingPostalCode())
                .billingCountry(org.getBillingCountry())
                .shippingSameAsPrimary(org.getShippingSameAsPrimary())
                .shippingAddressLine1(org.getShippingAddressLine1())
                .shippingAddressLine2(org.getShippingAddressLine2())
                .shippingCity(org.getShippingCity())
                .shippingState(org.getShippingState())
                .shippingPostalCode(org.getShippingPostalCode())
                .shippingCountry(org.getShippingCountry())
                .build();
    }
}
