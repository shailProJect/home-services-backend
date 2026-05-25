package com.homeservices.entity.enums;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public enum SubscriptionPlan {

    DAY   (BigDecimal.valueOf(99),   1,   "1 Day"),
    WEEK  (BigDecimal.valueOf(299),  7,   "1 Week"),
    MONTH (BigDecimal.valueOf(999),  30,  "1 Month"),
    YEAR  (BigDecimal.valueOf(9999), 365, "1 Year");

    private final BigDecimal price;
    private final int        durationDays;
    private final String     label;

    SubscriptionPlan(BigDecimal price, int durationDays, String label) {
        this.price        = price;
        this.durationDays = durationDays;
        this.label        = label;
    }

    public BigDecimal getPrice()        { return price; }
    public int        getDurationDays() { return durationDays; }
    public String     getLabel()        { return label; }

    public LocalDateTime expiryFromNow() {
        return LocalDateTime.now().plusDays(durationDays);
    }
}
