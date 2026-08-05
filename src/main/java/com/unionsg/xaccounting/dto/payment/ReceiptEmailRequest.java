package com.unionsg.xaccounting.dto.payment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReceiptEmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String to;

    @Size(max = 500, message = "Subject must not exceed 500 characters")
    private String subject;

    @Size(max = 2000, message = "Body must not exceed 2000 characters")
    private String body;

    private List<@Email(message = "Invalid email format") String> cc;

    private List<@Email(message = "Invalid email format") String> bcc;
}
