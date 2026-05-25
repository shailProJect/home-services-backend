package com.homeservices.dto.request;

import com.homeservices.entity.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionOrderRequest {

    @NotNull(message = "Plan is required")
    private SubscriptionPlan plan;   // DAY | WEEK | MONTH | YEAR
}
