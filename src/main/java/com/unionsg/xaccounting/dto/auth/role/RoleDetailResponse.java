package com.unionsg.xaccounting.dto.auth.role;
 
import lombok.Builder;
import lombok.Getter;
 
import java.util.Set;
 
@Getter
@Builder
public class RoleDetailResponse {
    private Long id;
    private String name;
    private String guardName;
    private String status;
    private Set<String> permissions;
}