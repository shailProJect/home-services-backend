package com.homeservices.dto.request;

import lombok.Data;

@Data
public class VerifyPhoneRequest {
  private String phone;
  private String otp;
}
