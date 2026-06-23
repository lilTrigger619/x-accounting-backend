package com.unionsg.xaccounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartClearToResponseDto {
    private Long id;
    private Long clearToCode;
    private Long chartCode;
    private String description;
    private LocalDateTime dateCreated;
    private Boolean deleted;
    private String deletedBy;
    private LocalDateTime dateDeleted;
}

