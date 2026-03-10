package com.unionsg.xaccounting.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
//public record LoginRequest(
//        @Email String email,
//        @NotBlank String password
//){}
public class LoginRequest  {
    private String email;
    private String password;
}
/*
public class LoginRequest {
}
 */
