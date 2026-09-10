package com.unionsg.xaccounting.dto.accounting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOpeningBalanceRequest {

    @NotNull(message = "Financial Year is required")
    private Long financialYearId;

    @Valid
    @NotEmpty(message = "Opening balance must contain at least one line")
    private List<OpeningBalanceLineRequest> lines;
}
