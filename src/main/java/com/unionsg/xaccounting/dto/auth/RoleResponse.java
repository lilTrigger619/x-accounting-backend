package com.unionsg.xaccounting.dto.auth;

import lombok.Builder;
import lombok.Getter;
import java.util.Set;

@Getter
@Builder
public class RoleResponse {
    private String name;
    private Set<String> permissions;
}