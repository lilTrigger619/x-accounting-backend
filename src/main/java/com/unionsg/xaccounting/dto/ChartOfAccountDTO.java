package com.unionsg.xaccounting.dto;
import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartOfAccountDTO {
    private Long id;
    private Long coaCode;
    private String coaDescription;
    private LocalDate dateCreated;
}