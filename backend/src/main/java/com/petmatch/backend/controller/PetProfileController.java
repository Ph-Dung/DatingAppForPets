package com.petmatch.backend.controller;

import com.petmatch.backend.dto.request.PetProfileRequest;
import com.petmatch.backend.dto.request.PetVaccinationRequest;
import com.petmatch.backend.dto.response.PetProfileResponse;
import com.petmatch.backend.dto.response.VaccinationResponse;
import com.petmatch.backend.entity.PetPhoto;
import com.petmatch.backend.enums.Gender;
import com.petmatch.backend.enums.HealthStatus;
import com.petmatch.backend.enums.LookingFor;
import com.petmatch.backend.service.CloudinaryService;
import com.petmatch.backend.service.PetProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetProfileController {
    private final PetProfileService petService;
    private final CloudinaryService cloudinaryService;

    // ── Profile ──────────────────────────────────────────
    @PostMapping
    public ResponseEntity<PetProfileResponse> create(
            @Valid @RequestBody PetProfileRequest req) {
        return ResponseEntity.status(201).body(petService.createProfile(req));
    }

    @PutMapping
    public ResponseEntity<PetProfileResponse> update(
            @Valid @RequestBody PetProfileRequest req) {
        return ResponseEntity.ok(petService.updateProfile(req));
    }

    @GetMapping("/me")
    public ResponseEntity<PetProfileResponse> getMyProfile() {
        return ResponseEntity.ok(petService.getMyProfile());
    }

    @GetMapping("/{petId}")
    public ResponseEntity<PetProfileResponse> getById(@PathVariable Long petId) {
        return ResponseEntity.ok(petService.getById(petId));
    }

    @PatchMapping("/toggle-hidden")
    public ResponseEntity<Void> toggleHidden() {
        petService.toggleHidden();
        return ResponseEntity.noContent().build();
    }

    // ── Suggestions & Search ─────────────────────────────
    @GetMapping("/suggestions")
    public ResponseEntity<?> suggestions(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean smart) {
        if (smart) {
            return ResponseEntity.ok(petService.getSmartSuggestions(page, size));
        }
        return ResponseEntity.ok(petService.getSuggestions(page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PetProfileResponse>> search(
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) LookingFor lookingFor,
            @RequestParam(required = false) HealthStatus healthStatus,
            @RequestParam(required = false) BigDecimal minWeight,
            @RequestParam(required = false) BigDecimal maxWeight,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                petService.search(species, breed, gender, lookingFor,
                        healthStatus, minWeight, maxWeight, minAge, maxAge, page, size));
    }

    // ── Vaccinations ─────────────────────────────────────
    @PostMapping("/vaccinations")
    public ResponseEntity<VaccinationResponse> addVaccination(
            @Valid @RequestBody PetVaccinationRequest req) {
        return ResponseEntity.status(201).body(petService.addVaccination(req));
    }

    @GetMapping("/vaccinations")
    public ResponseEntity<List<VaccinationResponse>> getVaccinations() {
        return ResponseEntity.ok(petService.getVaccinations());
    }

    @PutMapping("/vaccinations/{vacId}")
    public ResponseEntity<VaccinationResponse> updateVaccination(
            @PathVariable Long vacId,
            @Valid @RequestBody PetVaccinationRequest req) {
        return ResponseEntity.ok(petService.updateVaccination(vacId, req));
    }

    @DeleteMapping("/vaccinations/{vacId}")
    public ResponseEntity<Void> deleteVaccination(@PathVariable Long vacId) {
        petService.deleteVaccination(vacId);
        return ResponseEntity.noContent().build();
    }

    // ── Photos ───────────────────────────────────────────
    @PostMapping("/photos")
    public ResponseEntity<PetPhoto> addPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean setAsAvatar) {
        String photoUrl = cloudinaryService.uploadImage(file, "petmatch/pets");
        return ResponseEntity.status(201)
                .body(petService.addPhoto(photoUrl, setAsAvatar));
    }

    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long photoId) {
        petService.deletePhoto(photoId);
        return ResponseEntity.noContent().build();
    }
}