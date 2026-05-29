package com.homeservices.dto.request;

import lombok.Data;

@Data
public class AdminSupportReplyRequest {
    /** UUID of the user whose thread admin is replying to */
    private String userId;
    private String content;
}
