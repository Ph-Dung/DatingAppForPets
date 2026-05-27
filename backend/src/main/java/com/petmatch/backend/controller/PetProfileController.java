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

/**
 * Controller: Pet Profile CRUD, suggestions, search, vaccine, photos
 * 
 * Endpoints:
 * - POST/PUT /api/pets: create/update pet profile
 * - GET /api/pets/me: lấy hồ sơ của chính mình
 * - GET /api/pets/suggestions: gợi ý (basic hoặc smart AI)
 * - GET /api/pets/search: tìm kiếm nâng cao
 * - POST/GET/PUT/DELETE /api/pets/vaccinations: vaccine CRUD
 * - POST/DELETE /api/pets/photos: photo CRUD + upload Cloudinary
 */
@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetProfileController {
    private final PetProfileService petService;
    private final CloudinaryService cloudinaryService;

    // ════════════════════════════════════════════════════════════════
    // PROFILE CRUD
    // ════════════════════════════════════════════════════════════════
    
    /** POST /api/pets: Tạo hồ sơ pet mới (1 user = 1 pet) */
    @PostMapping
    public ResponseEntity<PetProfileResponse> create(
            @Valid @RequestBody PetProfileRequest req) {
        return ResponseEntity.status(201).body(petService.createProfile(req));
    }

    /** PUT /api/pets: Cập nhật hồ sơ (owner/createdAt không thay đổi) */
    @PutMapping
    public ResponseEntity<PetProfileResponse> update(
            @Valid @RequestBody PetProfileRequest req) {
        return ResponseEntity.ok(petService.updateProfile(req));
    }

    /** GET /api/pets/me: Lấy hồ sơ của chính mình */
    @GetMapping("/me")
    public ResponseEntity<PetProfileResponse> getMyProfile() {
        return ResponseEntity.ok(petService.getMyProfile());
    }

    /** GET /api/pets/{petId}: Lấy hồ sơ by petId */
    @GetMapping("/{petId}")
    public ResponseEntity<PetProfileResponse> getById(@PathVariable Long petId) {
        return ResponseEntity.ok(petService.getById(petId));
    }

    /** GET /api/pets/user/{userId}: Lấy hồ sơ of user */
    @GetMapping("/user/{userId}")
    public ResponseEntity<PetProfileResponse> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(petService.getByUserId(userId));
    }

    /** PATCH /api/pets/toggle-hidden: Ẩn/hiện hồ sơ từ gợi ý */
    @PatchMapping("/toggle-hidden")
    public ResponseEntity<Void> toggleHidden() {
        petService.toggleHidden();
        return ResponseEntity.noContent().build();
    }

    // ════════════════════════════════════════════════════════════════
    // SUGGESTIONS & SEARCH
    // ════════════════════════════════════════════════════════════════
    
    /** 
     * GET /api/pets/suggestions: Gợi ý cùng loài
     * 
     * Params:
     * - smart=true: AI sorting (nếu ≥5 likes); false: random
     * - maxDistanceKm: filter by location (haversine)
     */
    @GetMapping("/suggestions")
    public ResponseEntity<?> suggestions(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean smart,
            @RequestParam(required = false) Double maxDistanceKm) {
        if (smart) {
            return ResponseEntity.ok(petService.getSmartSuggestions(page, size, maxDistanceKm));
        }
        return ResponseEntity.ok(petService.getSuggestions(page, size, maxDistanceKm));
    }

    /** 
     * GET /api/pets/search: Tìm kiếm nâng cao
     * 
     * Filters (tất cả optional):
     * - species, breed, gender, lookingFor, healthStatus
     * - minWeight, maxWeight, minAge, maxAge
     * - maxDistanceKm (location)
     */
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
            @RequestParam(required = false) Double maxDistanceKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                petService.search(species, breed, gender, lookingFor,
                        healthStatus, minWeight, maxWeight, minAge, maxAge,
                        maxDistanceKm, page, size));
    }

    // ════════════════════════════════════════════════════════════════
    // VACCINE CRUD
    // ════════════════════════════════════════════════════════════════
    
    /** POST /api/pets/vaccinations: Thêm vaccine record */
    @PostMapping("/vaccinations")
    public ResponseEntity<VaccinationResponse> addVaccination(
            @Valid @RequestBody PetVaccinationRequest req) {
        return ResponseEntity.status(201).body(petService.addVaccination(req));
    }

    /** GET /api/pets/vaccinations: Danh sách vaccine (DESC by date) */
    @GetMapping("/vaccinations")
    public ResponseEntity<List<VaccinationResponse>> getVaccinations() {
        return ResponseEntity.ok(petService.getVaccinations());
    }

    /** PUT /api/pets/vaccinations/{vacId}: Cập nhật vaccine */
    @PutMapping("/vaccinations/{vacId}")
    public ResponseEntity<VaccinationResponse> updateVaccination(
            @PathVariable Long vacId,
            @Valid @RequestBody PetVaccinationRequest req) {
        return ResponseEntity.ok(petService.updateVaccination(vacId, req));
    }

    /** DELETE /api/pets/vaccinations/{vacId}: Xóa vaccine */
    @DeleteMapping("/vaccinations/{vacId}")
    public ResponseEntity<Void> deleteVaccination(@PathVariable Long vacId) {
        petService.deleteVaccination(vacId);
        return ResponseEntity.noContent().build();
    }

    // ════════════════════════════════════════════════════════════════
    // PHOTO CRUD
    // ════════════════════════════════════════════════════════════════
    
    /** 
     * POST /api/pets/photos: Upload ảnh + set as avatar
     * 
     * Multipart upload to Cloudinary
     * Params:
     * - file: MultipartFile
     * - setAsAvatar: true → replace old avatar
     */
    @PostMapping("/photos")
    public ResponseEntity<PetPhoto> addPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean setAsAvatar) {
        String photoUrl = cloudinaryService.uploadImage(file, "petmatch/pets");
        return ResponseEntity.status(201)
                .body(petService.addPhoto(photoUrl, setAsAvatar));
    }

    /** DELETE /api/pets/photos/{photoId}: Xóa ảnh (từ Cloudinary + DB) */
    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long photoId) {
        petService.deletePhoto(photoId);
        return ResponseEntity.noContent().build();
    }
}