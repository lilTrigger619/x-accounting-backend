package com.unionsg.xaccounting.dto.auth.role;
 
import lombok.Builder;
import lombok.Getter;
 
import java.util.Set;
import java.util.UUID;
 
@Getter
@Builder
public class RoleDetailResponse {
    private UUID id;
    private String name;
    private String guardName;
    private String status;
    private Set<String> permissions;
}