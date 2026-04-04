package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatbotResponse {
    private String reply;
    private boolean isReadyToSuggest;
    private List<PetProfileResponse> suggestions;
}
