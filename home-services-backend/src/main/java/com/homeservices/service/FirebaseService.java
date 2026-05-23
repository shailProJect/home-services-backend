package com.homeservices.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FirebaseService {

  // ── Verify any Firebase ID token ─────────────────────────────────────────

  /**
   * Verifies a Firebase ID token and returns the phone number embedded in it.
   * Used for legacy phone-OTP verification flow.
   */
  public String verifyToken(String token) throws Exception {
    FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
    Map<String, Object> claims = decodedToken.getClaims();
    String phoneNumber = (String) claims.get("phone_number");
    if (phoneNumber == null) {
      throw new RuntimeException("Phone number not found in token");
    }
    return phoneNumber;
  }

  // ── Phone-Login: verify and return full decoded token info ───────────────

  /**
   * Verifies a Firebase Phone Auth ID token and returns a {@link FirebasePhoneAuthResult}
   * containing the verified phone number and the Firebase UID.
   *
   * @param idToken Firebase ID token issued after signInWithPhoneNumber on the mobile client
   * @return uid + e164 phone number
   * @throws Exception if the token is invalid, expired, or lacks phone_number claim
   */
  public FirebasePhoneAuthResult verifyPhoneToken(String idToken) throws Exception {
    FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
    String uid = decoded.getUid();
    Map<String, Object> claims = decoded.getClaims();
    String phoneNumber = (String) claims.get("phone_number");
    if (phoneNumber == null) {
      throw new RuntimeException(
          "Firebase token does not contain a phone_number claim. "
              + "Ensure the token was issued via Phone Authentication.");
    }
    return new FirebasePhoneAuthResult(uid, phoneNumber);
  }

  // ── Result record ────────────────────────────────────────────────────────

  public record FirebasePhoneAuthResult(String uid, String phoneNumber) {}
}
