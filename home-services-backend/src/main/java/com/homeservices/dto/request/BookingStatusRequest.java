package com.homeservices.dto.request;

import com.homeservices.entity.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingStatusRequest {

    @NotNull(message = "Status is required")
    private BookingStatus status;
}
