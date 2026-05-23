package com.homeservices.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ProviderLocationUpdateRequest {

    @NotNull
    private UUID bookingId;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    /** Provider's estimated minutes to arrival (optional, calculated server-side if omitted) */
    private Integer estimatedArrivalMinutes;
}
