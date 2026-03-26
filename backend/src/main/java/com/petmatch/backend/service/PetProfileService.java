package com.petmatch.backend.service;

import com.petmatch.backend.dto.request.PetProfileRequest;
import com.petmatch.backend.dto.request.PetVaccinationRequest;
import com.petmatch.backend.dto.response.PetProfileResponse;
import com.petmatch.backend.entity.PetPhoto;
import com.petmatch.backend.entity.PetVaccination;
import com.petmatch.backend.enums.Gender;
import com.petmatch.backend.enums.LookingFor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface PetProfileService {
    PetProfileResponse createProfile(PetProfileRequest req);

    PetProfileResponse updateProfile(PetProfileRequest req);

    void toggleHidden();

    PetProfileResponse getMyProfile();

    PetProfileResponse getById(UUID petId);

    Page<PetProfileResponse> getSuggestions(int page, int size);

    Page<PetProfileResponse> search(String species, String breed,
                                    Gender gender, LookingFor lookingFor,
                                    int page, int size);

    // ── Vaccination ──────────────────────────────────────
    PetVaccination addVaccination(PetVaccinationRequest req);

    List<PetVaccination> getVaccinations();

    // ── Photos ───────────────────────────────────────────
    PetPhoto addPhoto(String photoUrl, boolean setAsAvatar);

    void deletePhoto(UUID photoId);
}
