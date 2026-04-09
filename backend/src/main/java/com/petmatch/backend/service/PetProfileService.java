package com.petmatch.backend.service;

import com.petmatch.backend.dto.request.PetProfileRequest;
import com.petmatch.backend.dto.request.PetVaccinationRequest;
import com.petmatch.backend.dto.response.PetPhotoDto;
import com.petmatch.backend.dto.response.PetProfileResponse;
import com.petmatch.backend.dto.response.VaccinationResponse;
import com.petmatch.backend.entity.PetPhoto;
import com.petmatch.backend.entity.PetProfile;
import com.petmatch.backend.entity.PetVaccination;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.Gender;
import com.petmatch.backend.enums.HealthStatus;
import com.petmatch.backend.enums.LookingFor;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.PetPhotoRepository;
import com.petmatch.backend.repository.PetProfileRepository;
import com.petmatch.backend.repository.PetVaccinationRepository;
import com.petmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PetProfileService {

    private final PetProfileRepository petProfileRepo;
    private final PetPhotoRepository petPhotoRepo;
    private final PetVaccinationRepository vaccinationRepo;
    private final UserRepository userRepo;
    private final CloudinaryService cloudinaryService;
    private final AiMatchingService aiMatchingService;

    // Helper lấy user hiện tại từ SecurityContext
    private User currentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException("User không tồn tại", NOT_FOUND));
    }

    private PetProfile myPet() {
        return petProfileRepo.findByOwnerId(currentUser().getId())
                .orElseThrow(() -> new AppException("Chưa có hồ sơ thú cưng", NOT_FOUND));
    }

    // ── Profile CRUD ─────────────────────────────────────
    public PetProfileResponse createProfile(PetProfileRequest req) {
        User user = currentUser();
        if (petProfileRepo.existsByOwnerId(user.getId()))
            throw new AppException("Bạn đã có hồ sơ thú cưng", CONFLICT);

        PetProfile pet = PetProfile.builder()
                .owner(user)
                .name(req.getName())
                .species(req.getSpecies())
                .breed(req.getBreed())
                .gender(req.getGender())
                .dateOfBirth(req.getDateOfBirth())
                .weightKg(req.getWeightKg())
                .color(req.getColor())
                .size(req.getSize())
                .reproductiveStatus(req.getReproductiveStatus())
                .isVaccinated(req.getIsVaccinated())
                .lastVaccineDate(req.getLastVaccineDate())
                .healthStatus(req.getHealthStatus())
                .healthNotes(req.getHealthNotes())
                .personalityTags(req.getPersonalityTags())
                .lookingFor(req.getLookingFor())
                .notes(req.getNotes())
                .isHidden(false)
                .build();

        return toResponse(petProfileRepo.save(pet));
    }

    public PetProfileResponse updateProfile(PetProfileRequest req) {
        PetProfile pet = myPet();

        pet.setName(req.getName());
        pet.setSpecies(req.getSpecies());
        pet.setBreed(req.getBreed());
        pet.setGender(req.getGender());
        pet.setDateOfBirth(req.getDateOfBirth());
        pet.setWeightKg(req.getWeightKg());
        pet.setColor(req.getColor());
        pet.setSize(req.getSize());
        pet.setReproductiveStatus(req.getReproductiveStatus());
        pet.setIsVaccinated(req.getIsVaccinated());
        pet.setLastVaccineDate(req.getLastVaccineDate());
        pet.setHealthStatus(req.getHealthStatus());
        pet.setHealthNotes(req.getHealthNotes());
        pet.setPersonalityTags(req.getPersonalityTags());
        pet.setLookingFor(req.getLookingFor());
        pet.setNotes(req.getNotes());

        return toResponse(petProfileRepo.save(pet));
    }

    public void toggleHidden() {
        PetProfile pet = myPet();
        pet.setIsHidden(!pet.getIsHidden());
        petProfileRepo.save(pet);
    }

    @Transactional(readOnly = true)
    public PetProfileResponse getMyProfile() {
        return toResponse(myPet());
    }

    @Transactional(readOnly = true)
    public PetProfileResponse getById(Long petId) {
        return toResponse(petProfileRepo.findById(petId)
                .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ", NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public PetProfileResponse getByUserId(Long userId) {
        return toResponse(petProfileRepo.findByOwnerId(userId)
                .orElseThrow(() -> new AppException("Người dùng chưa có thú cưng", NOT_FOUND)));
    }

    // ── Suggestions & Search ─────────────────────────────
    @Transactional(readOnly = true)
    public Page<PetProfileResponse> getSuggestions(int page, int size, Double maxDistanceKm) {
        User user = currentUser();
        PetProfile myPet = petProfileRepo.findByOwnerId(user.getId())
                .orElseThrow(() -> new AppException("Bạn chưa tạo hồ sơ thú cưng", NOT_FOUND));

        if (maxDistanceKm != null && user.getLatitude() != null) {
            List<PetProfile> pool = petProfileRepo.findSuggestions(
                    user.getId(), myPet.getId(), myPet.getSpecies(),
                    com.petmatch.backend.enums.MatchStatus.PENDING,
                    PageRequest.of(page, size * 4)).getContent();
            List<PetProfileResponse> filtered = pool.stream()
                    .filter(p -> p.getOwner().getLatitude() != null &&
                            haversineKm(user.getLatitude(), user.getLongitude(),
                                    p.getOwner().getLatitude(), p.getOwner().getLongitude()) <= maxDistanceKm)
                    .limit(size)
                    .map(p -> toResponseWithUser(p, user))
                    .collect(Collectors.toList());
            return new org.springframework.data.domain.PageImpl<>(filtered);
        }

        return petProfileRepo.findSuggestions(
                        user.getId(), myPet.getId(), myPet.getSpecies(),
                        com.petmatch.backend.enums.MatchStatus.PENDING,
                        PageRequest.of(page, size))
                .map(p -> toResponseWithUser(p, user));
    }

    /**
     * Smart suggestions: khi user đã có đủ 5 likes, sort theo AI score.
     * Nếu chưa đủ (smart = false), fallback về suggestions bình thường.
     */
    @Transactional(readOnly = true)
    public List<PetProfileResponse> getSmartSuggestions(int page, int size, Double maxDistanceKm) {
        User user = currentUser();
        PetProfile myPet = petProfileRepo.findByOwnerId(user.getId())
                .orElseThrow(() -> new AppException("Bạn chưa tạo hồ sơ thú cưng", NOT_FOUND));

        List<com.petmatch.backend.entity.PetProfile> candidates =
                petProfileRepo.findSuggestions(
                        user.getId(), myPet.getId(), myPet.getSpecies(),
                        com.petmatch.backend.enums.MatchStatus.PENDING,
                        PageRequest.of(page, Math.max(size * 3, 30))).getContent();

        return candidates.stream()
                .filter(c -> maxDistanceKm == null || user.getLatitude() == null ||
                        c.getOwner().getLatitude() == null ||
                        haversineKm(user.getLatitude(), user.getLongitude(),
                                c.getOwner().getLatitude(), c.getOwner().getLongitude()) <= maxDistanceKm)
                .map(c -> new Object[]{c, aiMatchingService.scorePet(myPet.getId(), c)})
                .sorted((a, b) -> Integer.compare((int) b[1], (int) a[1]))
                .limit(size)
                .map(pair -> toResponseWithUser((com.petmatch.backend.entity.PetProfile) pair[0], user))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PetProfileResponse> search(String species, String breed,
                                           Gender gender, LookingFor lookingFor,
                                           HealthStatus healthStatus,
                                           BigDecimal minWeight, BigDecimal maxWeight,
                                           Integer minAge, Integer maxAge,
                                           Double maxDistanceKm,
                                           int page, int size) {
        LocalDate minDob = maxAge != null ? LocalDate.now().minusYears(maxAge) : null;
        LocalDate maxDob = minAge != null ? LocalDate.now().minusYears(minAge) : null;
        String safeBreed = breed == null ? "" : breed;
        User user = currentUser();

        if (maxDistanceKm != null && user.getLatitude() != null) {
            List<PetProfile> pool = petProfileRepo.search(
                    user.getId(),
                    species != null, species,
                    safeBreed != null && !safeBreed.trim().isEmpty(), safeBreed,
                    gender != null, gender,
                    lookingFor != null, lookingFor,
                    healthStatus != null, healthStatus,
                    minWeight != null, minWeight,
                    maxWeight != null, maxWeight,
                    minDob != null, minDob,
                    maxDob != null, maxDob,
                    PageRequest.of(page, size * 4)).getContent();
            List<PetProfileResponse> filtered = pool.stream()
                    .filter(p -> p.getOwner().getLatitude() != null &&
                            haversineKm(user.getLatitude(), user.getLongitude(),
                                    p.getOwner().getLatitude(), p.getOwner().getLongitude()) <= maxDistanceKm)
                    .limit(size)
                    .map(p -> toResponseWithUser(p, user))
                    .collect(Collectors.toList());
            return new org.springframework.data.domain.PageImpl<>(filtered);
        }

        return petProfileRepo.search(
                        user.getId(),
                        species != null, species,
                        safeBreed != null && !safeBreed.trim().isEmpty(), safeBreed,
                        gender != null, gender,
                        lookingFor != null, lookingFor,
                        healthStatus != null, healthStatus,
                        minWeight != null, minWeight,
                        maxWeight != null, maxWeight,
                        minDob != null, minDob,
                        maxDob != null, maxDob,
                        PageRequest.of(page, size))
                .map(p -> toResponseWithUser(p, user));
    }

    // ── Vaccination CRUD ──────────────────────────────────
    public VaccinationResponse addVaccination(PetVaccinationRequest req) {
        PetProfile pet = myPet();
        PetVaccination v = PetVaccination.builder()
                .pet(pet)
                .vaccineName(req.getVaccineName())
                .vaccinatedDate(req.getVaccinatedDate())
                .nextDueDate(req.getNextDueDate())
                .clinicName(req.getClinicName())
                .notes(req.getNotes())
                .build();

        // Cập nhật is_vaccinated + ngày tiêm gần nhất
        pet.setIsVaccinated(true);
        if (pet.getLastVaccineDate() == null ||
                req.getVaccinatedDate().isAfter(pet.getLastVaccineDate())) {
            pet.setLastVaccineDate(req.getVaccinatedDate());
        }
        petProfileRepo.save(pet);
        return toVaccinationResponse(vaccinationRepo.save(v));
    }

    public VaccinationResponse updateVaccination(Long vacId, PetVaccinationRequest req) {
        PetProfile pet = myPet();
        PetVaccination v = vaccinationRepo.findByIdAndPetId(vacId, pet.getId())
                .orElseThrow(() -> new AppException("Không tìm thấy bản ghi vaccine", NOT_FOUND));

        v.setVaccineName(req.getVaccineName());
        v.setVaccinatedDate(req.getVaccinatedDate());
        v.setNextDueDate(req.getNextDueDate());
        v.setClinicName(req.getClinicName());
        v.setNotes(req.getNotes());

        // Recalculate lastVaccineDate
        vaccinationRepo.findByPetIdOrderByVaccinatedDateDesc(pet.getId()).stream()
                .findFirst()
                .ifPresent(latest -> pet.setLastVaccineDate(latest.getVaccinatedDate()));
        petProfileRepo.save(pet);

        return toVaccinationResponse(vaccinationRepo.save(v));
    }

    public void deleteVaccination(Long vacId) {
        PetProfile pet = myPet();
        PetVaccination v = vaccinationRepo.findByIdAndPetId(vacId, pet.getId())
                .orElseThrow(() -> new AppException("Không tìm thấy bản ghi vaccine", NOT_FOUND));
        vaccinationRepo.delete(v);

        // Recalculate isVaccinated + lastVaccineDate
        List<PetVaccination> remaining = vaccinationRepo.findByPetIdOrderByVaccinatedDateDesc(pet.getId());
        if (remaining.isEmpty()) {
            pet.setIsVaccinated(false);
            pet.setLastVaccineDate(null);
        } else {
            pet.setLastVaccineDate(remaining.get(0).getVaccinatedDate());
        }
        petProfileRepo.save(pet);
    }

    @Transactional(readOnly = true)
    public List<VaccinationResponse> getVaccinations() {
        PetProfile pet = myPet();
        return vaccinationRepo.findByPetIdOrderByVaccinatedDateDesc(pet.getId())
                .stream().map(this::toVaccinationResponse).toList();
    }

    // ── Photos ───────────────────────────────────────────
    public PetPhoto addPhoto(String photoUrl, boolean setAsAvatar) {
        PetProfile pet = myPet();

        if (petPhotoRepo.countByPetId(pet.getId()) >= 10)
            throw new AppException("Tối đa 10 ảnh", BAD_REQUEST);

        if (setAsAvatar) {
            petPhotoRepo.findByPetIdAndIsAvatarTrue(pet.getId())
                    .ifPresent(old -> { old.setIsAvatar(false); petPhotoRepo.save(old); });
        }

        PetPhoto photo = PetPhoto.builder()
                .pet(pet).photoUrl(photoUrl).isAvatar(setAsAvatar).build();
        return petPhotoRepo.save(photo);
    }

    public void deletePhoto(Long photoId) {
        PetPhoto photo = petPhotoRepo.findById(photoId)
                .orElseThrow(() -> new AppException("Không tìm thấy ảnh", NOT_FOUND));

        if (!photo.getPet().getOwner().getId().equals(currentUser().getId()))
            throw new AppException("Không có quyền xóa ảnh này", FORBIDDEN);

        cloudinaryService.deleteImage(photo.getPhotoUrl());
        petPhotoRepo.delete(photo);
    }

    // ── Mappers ───────────────────────────────────────────
    private PetProfileResponse toResponse(PetProfile p) {
        User currentUser = null;
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            currentUser = userRepo.findByEmail(email).orElse(null);
        } catch (Exception ignored) {}
        return toResponseWithUser(p, currentUser);
    }

    private PetProfileResponse toResponseWithUser(PetProfile p, User currentUser) {
        String avatarUrl = petPhotoRepo.findByPetIdAndIsAvatarTrue(p.getId())
                .map(PetPhoto::getPhotoUrl).orElse(null);
        List<String> photoUrls = petPhotoRepo.findByPetId(p.getId())
                .stream().map(PetPhoto::getPhotoUrl).toList();
        List<PetPhotoDto> photos = petPhotoRepo.findByPetId(p.getId())
                .stream().map(ph -> PetPhotoDto.builder().id(ph.getId()).url(ph.getPhotoUrl()).build()).toList();
        int vacCount = (int) vaccinationRepo.countByPetId(p.getId());

        Integer age = null;
        if (p.getDateOfBirth() != null) {
            age = Period.between(p.getDateOfBirth(), LocalDate.now()).getYears();
        }

        Double distanceKm = null;
        if (currentUser != null && currentUser.getLatitude() != null
                && p.getOwner().getLatitude() != null) {
            distanceKm = Math.round(haversineKm(
                    currentUser.getLatitude(), currentUser.getLongitude(),
                    p.getOwner().getLatitude(), p.getOwner().getLongitude()) * 10.0) / 10.0;
        }

        return PetProfileResponse.builder()
                .id(p.getId())
                .ownerId(p.getOwner().getId())
                .ownerName(p.getOwner().getFullName())
                .name(p.getName())
                .species(p.getSpecies())
                .breed(p.getBreed())
                .gender(p.getGender() != null ? p.getGender().name() : null)
                .dateOfBirth(p.getDateOfBirth())
                .age(age)
                .weightKg(p.getWeightKg())
                .color(p.getColor())
                .size(p.getSize())
                .reproductiveStatus(p.getReproductiveStatus() != null ? p.getReproductiveStatus().name() : null)
                .isVaccinated(p.getIsVaccinated())
                .lastVaccineDate(p.getLastVaccineDate())
                .vaccinationCount(vacCount)
                .healthStatus(p.getHealthStatus() != null ? p.getHealthStatus().name() : null)
                .healthNotes(p.getHealthNotes())
                .personalityTags(p.getPersonalityTags())
                .lookingFor(p.getLookingFor() != null ? p.getLookingFor().name() : null)
                .notes(p.getNotes())
                .isHidden(p.getIsHidden())
                .avatarUrl(avatarUrl)
                .photoUrls(photoUrls)
                .photos(photos)
                .createdAt(p.getCreatedAt())
                .distanceKm(distanceKm)
                .ownerAddress(p.getOwner().getAddress())
                .build();
    }

    /** Haversine formula: trả về khoảng cách km giữa 2 điểm lat/lon */
    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private VaccinationResponse toVaccinationResponse(PetVaccination v) {
        return VaccinationResponse.builder()
                .id(v.getId())
                .vaccineName(v.getVaccineName())
                .vaccinatedDate(v.getVaccinatedDate())
                .nextDueDate(v.getNextDueDate())
                .clinicName(v.getClinicName())
                .notes(v.getNotes())
                .createdAt(v.getCreatedAt())
                .build();
    }
}