package com.unionsg.xaccounting.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.
import lombok.Getter;
import lombok.Setter;


//public record CreateUserRequest(
//        @Email String email,
//        @NotBlank String password
//){}


@Getter
@Setter
public class CreateUserRequest{
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String roleName;
}

/**
public class RegisterRequest {
}

 **/