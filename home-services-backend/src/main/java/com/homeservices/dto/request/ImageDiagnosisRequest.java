package com.homeservices.dto.request;

import lombok.Data;

@Data
public class ImageDiagnosisRequest {
    /** Cloudinary URL of the uploaded appliance problem image */
    private String imageUrl;

    /** Optional description of the issue from the user */
    private String description;
}
