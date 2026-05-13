package com.homeservices.dto.request;

import lombok.Data;

/**
 * Used by admin to attach a note or rejection reason to a provider.
 */
@Data
public class AdminNoteRequest {

  private String notes;
}
