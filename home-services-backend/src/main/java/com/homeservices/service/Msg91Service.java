package com.homeservices.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class Msg91Service {

  @Value("${msg91.auth-key}")
  private String authKey;

  private final RestTemplate restTemplate = new RestTemplate();

  public void sendOtp(String phone, String otp) {

    try {

      String message = "ApnaAdmi OTP: " + otp + ". Valid for 5 mins.";

      String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);

      String url =
          "https://api.msg91.com/api/sendhttp.php"
              + "?authkey=" + authKey
              + "&mobiles=91" + phone
              + "&message=" + encodedMessage
              + "&sender=APNAADMI"
              + "&route=4"
              + "&country=91";

      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
      System.out.println("response " +response);
      System.out.println(response.getBody());

    } catch (Exception e) {

      e.printStackTrace();
    }
  }
}
