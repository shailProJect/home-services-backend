package com.homeservices.dto.request;

import lombok.Data;

@Data
public class VerifyProviderOtpRequest {
  private String phone;

  private String otp;
}
