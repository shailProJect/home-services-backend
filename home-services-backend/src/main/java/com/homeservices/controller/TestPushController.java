package com.homeservices.controller;

import com.homeservices.repository.PushSubscriptionRepository;
import com.homeservices.service.WebPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestPushController {

  private final PushSubscriptionRepository repository;

  private final WebPushService webPushService;

  @GetMapping("/push")
  public String testPush() throws Exception {

    var subscriptions = repository.findAll();
    System.out.println("subscriptions"  +subscriptions);
    for (var sub : subscriptions) {

      webPushService.sendNotification(sub, "Test Notification 🚀",
          "Push notification working successfully");
    }

    return "Push sent successfully";
  }
}
