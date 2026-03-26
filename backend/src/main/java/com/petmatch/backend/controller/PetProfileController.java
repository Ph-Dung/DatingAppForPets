package com.petmatch.backend.controller;

import com.petmatch.backend.dto.request.PetProfileRequest;
import com.petmatch.backend.dto.request.PetVaccinationRequest;
import com.petmatch.backend.dto.response.PetProfileResponse;
import com.petmatch.backend.entity.PetPhoto;
import com.petmatch.backend.entity.PetVaccination;
import com.petmatch.backend.enums.Gender;
import com.petmatch.backend.enums.LookingFor;
import com.petmatch.backend.service.CloudinaryService;
import com.petmatch.backend.service.PetProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

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
    public ResponseEntity<PetProfileResponse> getById(@PathVariable UUID petId) {
        return ResponseEntity.ok(petService.getById(petId));
    }

    @PatchMapping("/toggle-hidden")
    public ResponseEntity<Void> toggleHidden() {
        petService.toggleHidden();
        return ResponseEntity.noContent().build();
    }

    // ── Suggestions & Search ─────────────────────────────
    @GetMapping("/suggestions")
    public ResponseEntity<Page<PetProfileResponse>> suggestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(petService.getSuggestions(page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PetProfileResponse>> search(
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) LookingFor lookingFor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                petService.search(species, breed, gender, lookingFor, page, size));
    }

    // ── Vaccinations ─────────────────────────────────────
    @PostMapping("/vaccinations")
    public ResponseEntity<PetVaccination> addVaccination(
            @Valid @RequestBody PetVaccinationRequest req) {
        return ResponseEntity.status(201).body(petService.addVaccination(req));
    }

    @GetMapping("/vaccinations")
    public ResponseEntity<List<PetVaccination>> getVaccinations() {
        return ResponseEntity.ok(petService.getVaccinations());
    }

    @PostMapping("/photos")
    public ResponseEntity<PetPhoto> addPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean setAsAvatar) {

        // 1. Upload lên Cloudinary → lấy URL
        String photoUrl = cloudinaryService.uploadImage(file, "petmatch/pets");

        // 2. Lưu URL vào DB
        return ResponseEntity.status(201)
                .body(petService.addPhoto(photoUrl, setAsAvatar));
    }

    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable UUID photoId) {
        petService.deletePhoto(photoId);
        return ResponseEntity.noContent().build();
    }
}