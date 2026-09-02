package com.unionsg.xaccounting.dto.customer;

import com.unionsg.xaccounting.enums.CustomerActivityReferenceType;
import com.unionsg.xaccounting.enums.CustomerActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerActivityLogResponseDto {
    private Long id;
    private CustomerActivityType type;
    private String title;
    private String description;
    private CustomerActivityReferenceType referenceType;
    private Long referenceId;
    private String actor;
    private LocalDateTime createdAt;
}
