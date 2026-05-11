package com.unionsg.xaccounting.dto.auth.permission;
 
import lombok.Builder;
import lombok.Getter;
import java.util.UUID;
 
@Getter
@Builder
public class PermissionResponse {
    private UUID id;
    private String name;
    private String guardName;
    private String status;
}