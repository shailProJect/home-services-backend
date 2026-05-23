package com.homeservices.dto.request;

import lombok.Data;

@Data
public class VerifyEmailRequest {

    private String email;

    private String otp;
}