package com.unionsg.xaccounting.dto.auth;

import lombok.Builder;
import lombok.Getter;

import java.util.*;

@Getter
@Builder
public class CurrentUserResponse {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private List<RoleResponse> roles;
    private Set<String> directPermissions;
    private String photoFileId;
    private String photoUrl;
}
