package com.homeservices.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeservices.entity.PushSubscription;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WebPushService {

  @Value("${vapid.public-key}")
  private String publicKey;

  @Value("${vapid.private-key}")
  private String privateKey;

  @Value("${vapid.subject}")
  private String subject;

  private final ObjectMapper objectMapper;

  private PushService pushService;

  @PostConstruct
  public void init() throws GeneralSecurityException {

    Security.addProvider(new BouncyCastleProvider());

    pushService = new PushService();

    pushService.setPublicKey(Utils.loadPublicKey(publicKey));

    pushService.setPrivateKey(Utils.loadPrivateKey(privateKey));

    pushService.setSubject(subject);
  }

  public void sendNotification(PushSubscription pushSubscription, String title, String message)
      throws Exception {

    Map<String, String> payloadMap = new HashMap<>();

    payloadMap.put("title", title);
    payloadMap.put("body", message);

    String payload = objectMapper.writeValueAsString(payloadMap);

    Notification notification =
        new Notification(pushSubscription.getEndpoint(), pushSubscription.getP256dh(),
            pushSubscription.getAuth(), payload.getBytes(StandardCharsets.UTF_8));
    pushService.send(notification);
  }
}
