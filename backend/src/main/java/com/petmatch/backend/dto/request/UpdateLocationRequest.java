package com.petmatch.backend.dto.request;

import lombok.Data;

@Data
public class UpdateLocationRequest {
    private Double latitude;
    private Double longitude;
}
