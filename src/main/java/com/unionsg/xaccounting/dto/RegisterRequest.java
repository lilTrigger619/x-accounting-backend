package com.unionsg.xaccounting.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.


public record RegisterRequest(
        @Email String email,
        @NotBlank String password
){}

/**
public class RegisterRequest {
}

 **/