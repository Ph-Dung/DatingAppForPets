package com.petmatch.backend.service;

import com.petmatch.backend.dto.response.MatchRequestResponse;
import com.petmatch.backend.enums.MatchStatus;

import java.util.List;
import java.util.UUID;

public interface MatchRequestService {
    MatchRequestResponse sendRequest(UUID receiverPetId);

    MatchRequestResponse respond(UUID matchId, MatchStatus newStatus);

    List<MatchRequestResponse> getMySentRequests();

    List<MatchRequestResponse> getMyPendingReceived();

    List<MatchRequestResponse> getMyMatches();
}
