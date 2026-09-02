package com.unionsg.xaccounting.service.customer;

import com.unionsg.xaccounting.dto.customer.CustomerActivityLogResponseDto;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.customer.CustomerActivityLog;
import com.unionsg.xaccounting.enums.CustomerActivityReferenceType;
import com.unionsg.xaccounting.enums.CustomerActivityType;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.repository.customer.CustomerActivityLogRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records the customer activity feed (status changes, invoices, payments,
 * emails). Deliberately tolerant of failure — a broken log write must never
 * roll back the business transaction it's attached to (e.g. a payment being
 * recorded), so every entry point swallows and logs its own exceptions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerActivityLogService {

    private final CustomerActivityLogRepository repository;
    private final CustomerRepository customerRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            Long customerId,
            CustomerActivityType type,
            String title,
            String description,
            CustomerActivityReferenceType referenceType,
            Long referenceId
    ) {
        try {
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null) {
                return;
            }

            CustomerActivityLog logEntry = CustomerActivityLog.builder()
                    .customer(customer)
                    .type(type)
                    .title(title)
                    .description(description)
                    .referenceType(referenceType != null ? referenceType : CustomerActivityReferenceType.NONE)
                    .referenceId(referenceId)
                    .actor(resolveActor())
                    .build();

            repository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to record customer activity log for customer {}: {}", customerId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<CustomerActivityLogResponseDto> getForCustomer(Long customerId, Pageable pageable) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::toResponse);
    }

    private String resolveActor() {
        try {
            User user = SecurityUtils.getCurrentUser();
            return user != null ? user.getFullName() : "System";
        } catch (Exception e) {
            return "System";
        }
    }

    private CustomerActivityLogResponseDto toResponse(CustomerActivityLog entity) {
        return CustomerActivityLogResponseDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .actor(entity.getActor())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
