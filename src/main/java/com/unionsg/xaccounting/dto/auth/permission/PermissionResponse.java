package com.unionsg.xaccounting.dto.auth.permission;
 
import lombok.Builder;
import lombok.Getter;
 
@Getter
@Builder
public class PermissionResponse {
    private Long id;
    private String name;
    private String guardName;
    private String status;
}