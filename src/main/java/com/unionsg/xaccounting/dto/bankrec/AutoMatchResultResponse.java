package com.unionsg.xaccounting.dto.bankrec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoMatchResultResponse {
    private Integer statementCandidates;
    private Integer bookCandidates;
    private Integer confirmed;
    private Integer proposed;
    private List<String> rulesApplied;
    private List<MatchResponse> matches;
}
