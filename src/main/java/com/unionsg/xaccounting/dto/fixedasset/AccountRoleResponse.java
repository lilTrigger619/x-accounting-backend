package com.unionsg.xaccounting.dto.fixedasset;

import lombok.Getter;
import lombok.Setter;

/**
 * One posting role (cost, accumulated depreciation, ...) with the account set on the record itself
 * ({@code own}), the account it actually resolves to and where that came from: ASSET, CATEGORY or
 * MAPPING. {@code error} explains why the resolved account can't be posted to.
 */
@Getter
@Setter
public class AccountRoleResponse {
    private String role;
    private String label;
    private AccountRef own;
    private AccountRef resolved;
    private String source;
    private String error;
}
