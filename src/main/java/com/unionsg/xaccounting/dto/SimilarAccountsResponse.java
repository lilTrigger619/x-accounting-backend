package com.unionsg.xaccounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Existing accounts whose names look like a proposed one, most alike first. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimilarAccountsResponse {
    /** Every similar account, even when {@link #accounts} is capped. */
    private long count;
    private List<AccountListResponse> accounts;
}
