package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatbotResponse {
    private String reply;
    @com.fasterxml.jackson.annotation.JsonProperty("isReadyToSuggest")
    private boolean isReadyToSuggest;
    private List<PetProfileResponse> suggestions;
}
