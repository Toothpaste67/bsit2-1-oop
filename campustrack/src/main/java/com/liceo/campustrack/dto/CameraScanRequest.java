package com.liceo.campustrack.dto;

/** Body sent by the camera scanner and by the offline queue sync. */
public record CameraScanRequest(String qrPayload) {
}
