package com.unionsg.xaccounting.dto.auth;
import lombok.Builder;
import lombok.Setter;

@Setter
@Builder
public class LoginResponse {
    private String token;
    private String userId;
    private String email;
    private String role;

}
