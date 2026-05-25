package com.homeservices.entity.enums;

public enum BookingStatus {
    PENDING,           // Waiting for provider to accept
    CONFIRMED,         // Provider accepted
    PROVIDER_EN_ROUTE, // Provider is travelling to user (tracking active)
    IN_PROGRESS,       // Provider has arrived and work started
    COMPLETED,         // Work done
    CANCELLED,         // User cancelled
    REJECTED,          // Provider rejected
    URGENT_PENDING,    // Urgent booking awaiting acceptance
    URGENT_CONFIRMED,   // Urgent booking accepted — must arrive in 30 min
    ARRIVED,
    ACCEPTED
}
