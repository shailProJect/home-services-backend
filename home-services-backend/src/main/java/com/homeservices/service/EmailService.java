package com.homeservices.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${resend.api-key}")
    private String resendApiKey;

    public void sendOtpEmail(String to, String otp)
            throws ResendException {

        Resend resend = new Resend(resendApiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()

                // CHANGE THIS LATER TO YOUR DOMAIN
                // Example:
                // .from("Apna Admi <noreply@apnaadmi.in>")
                .from("Apna Admi <noreply@apnaadmi.in>")

                .to(to)

                .subject("Verify your email - Apna Admi")

                .html("""
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" />
  <meta name="viewport"
        content="width=device-width, initial-scale=1.0"/>
</head>

<body style="
  margin:0;
  padding:0;
  background:#f5f7fb;
  font-family:Arial,sans-serif;
">

  <table width="100%%"
         cellspacing="0"
         cellpadding="0"
         style="padding:40px 16px;">

    <tr>
      <td align="center">

        <table width="100%%"
               style="
                max-width:520px;
                background:#ffffff;
                border-radius:20px;
                overflow:hidden;
                box-shadow:0 10px 30px rgba(0,0,0,0.08);
               ">

          <!-- HEADER -->
          <tr>
            <td style="
              background:linear-gradient(
                135deg,
                #111827,
                #1f2937
              );
              padding:32px;
              text-align:center;
            ">

              <div style="
                width:64px;
                height:64px;
                margin:0 auto 16px;
                border-radius:16px;
                background:#ffffff10;
                display:flex;
                align-items:center;
                justify-content:center;
                font-size:30px;
              ">
                🔧
              </div>

              <h1 style="
                color:white;
                margin:0;
                font-size:28px;
                font-weight:700;
              ">
                Apna Admi
              </h1>

              <p style="
                color:#d1d5db;
                margin-top:8px;
                font-size:14px;
              ">
                Trusted Local Services
              </p>

            </td>
          </tr>

          <!-- BODY -->
          <tr>
            <td style="padding:40px 32px;">

              <h2 style="
                margin:0 0 16px;
                color:#111827;
                font-size:24px;
              ">
                Verify Your Email
              </h2>

              <p style="
                color:#4b5563;
                font-size:15px;
                line-height:1.7;
                margin:0 0 24px;
              ">
                Welcome to
                <strong>Apna Admi</strong>.

                Please use the OTP below
                to verify your email address
                and activate your account.
              </p>

              <!-- OTP BOX -->
              <div style="
                background:#f3f4f6;
                border:2px dashed #d1d5db;
                border-radius:16px;
                padding:24px;
                text-align:center;
                margin-bottom:24px;
              ">

                <p style="
                  margin:0;
                  color:#6b7280;
                  font-size:13px;
                  letter-spacing:1px;
                ">
                  YOUR OTP CODE
                </p>

                <h1 style="
                  margin:12px 0 0;
                  font-size:42px;
                  color:#111827;
                  letter-spacing:8px;
                ">
                  %s
                </h1>

              </div>

              <p style="
                color:#6b7280;
                font-size:14px;
                line-height:1.7;
                margin:0;
              ">
                This OTP will expire in
                <strong>10 minutes</strong>.
              </p>

              <p style="
                color:#6b7280;
                font-size:14px;
                line-height:1.7;
                margin-top:16px;
              ">
                If you did not create this
                account, please ignore
                this email.
              </p>

            </td>
          </tr>

          <!-- FOOTER -->
          <tr>
            <td style="
              background:#f9fafb;
              padding:24px;
              text-align:center;
              border-top:1px solid #e5e7eb;
            ">

              <p style="
                margin:0;
                color:#9ca3af;
                font-size:13px;
              ">
                © 2026 Apna Admi.
                All rights reserved.
              </p>

            </td>
          </tr>

        </table>

      </td>
    </tr>
  </table>

</body>
</html>
""".formatted(otp))

                .build();

        resend.emails().send(params);
    }
}