package com.homeservices.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Sent by mobile clients after Firebase Phone Authentication completes.
 * The client obtains a Firebase ID token via signInWithPhoneNumber() and
 * forwards it here. The backend verifies the token with Firebase Admin SDK,
 * extracts the phone number, and issues its own JWT pair.
 */
@Data
public class PhoneLoginRequest {

  @NotBlank(message = "Firebase ID token is required")
  private String firebaseToken;
}
