package com.petmatch.backend.service.impl;

import com.petmatch.backend.dto.request.PetProfileRequest;
import com.petmatch.backend.dto.request.PetVaccinationRequest;
import com.petmatch.backend.dto.response.PetProfileResponse;
import com.petmatch.backend.entity.PetPhoto;
import com.petmatch.backend.entity.PetProfile;
import com.petmatch.backend.entity.PetVaccination;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.Gender;
import com.petmatch.backend.enums.LookingFor;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.PetPhotoRepository;
import com.petmatch.backend.repository.PetProfileRepository;
import com.petmatch.backend.repository.PetVaccinationRepository;
import com.petmatch.backend.repository.UserRepository;
import com.petmatch.backend.service.CloudinaryService;
import com.petmatch.backend.service.PetProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PetProfileServiceImpl implements PetProfileService {

    private final PetProfileRepository petProfileRepo;
    private final PetPhotoRepository petPhotoRepo;
    private final PetVaccinationRepository vaccinationRepo;
    private final UserRepository userRepo;
    private final CloudinaryService cloudinaryService;

    // Helper lấy user hiện tại từ SecurityContext
    private User currentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException("User không tồn tại", NOT_FOUND));
    }

    @Override
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

    @Override
    public PetProfileResponse updateProfile(PetProfileRequest req) {
        User user = currentUser();
        PetProfile pet = petProfileRepo.findByOwnerId(user.getId())
                .orElseThrow(() -> new AppException("Chưa có hồ sơ thú cưng", NOT_FOUND));

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

    @Override
    public void toggleHidden() {
        User user = currentUser();
        PetProfile pet = petProfileRepo.findByOwnerId(user.getId())
                .orElseThrow(() -> new AppException("Chưa có hồ sơ thú cưng", NOT_FOUND));
        pet.setIsHidden(!pet.getIsHidden());
        petProfileRepo.save(pet);
    }

    @Override
    public PetProfileResponse getMyProfile() {
        return toResponse(petProfileRepo.findByOwnerId(currentUser().getId())
                .orElseThrow(() -> new AppException("Chưa có hồ sơ thú cưng", NOT_FOUND)));
    }

    @Override
    public PetProfileResponse getById(UUID petId) {
        return toResponse(petProfileRepo.findById(petId)
                .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ", NOT_FOUND)));
    }

    @Override
    public Page<PetProfileResponse> getSuggestions(int page, int size) {
        User user = currentUser();
        PetProfile myPet = petProfileRepo.findByOwnerId(user.getId())
                .orElseThrow(() -> new AppException("Bạn chưa tạo hồ sơ thú cưng", NOT_FOUND));

        return petProfileRepo.findSuggestions(
                        user.getId(), myPet.getSpecies(),
                        PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Override
    public Page<PetProfileResponse> search(String species, String breed,
                                           Gender gender, LookingFor lookingFor,
                                           int page, int size) {
        return petProfileRepo.search(
                        currentUser().getId(),
                        species, breed, gender, lookingFor,
                        PageRequest.of(page, size))
                .map(this::toResponse);
    }

    // ── Vaccination ──────────────────────────────────────
    @Override
    public PetVaccination addVaccination(PetVaccinationRequest req) {
        PetProfile pet = petProfileRepo.findByOwnerId(currentUser().getId())
                .orElseThrow(() -> new AppException("Chưa có hồ sơ", NOT_FOUND));
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
        return vaccinationRepo.save(v);
    }

    @Override
    public List<PetVaccination> getVaccinations() {
        PetProfile pet = petProfileRepo.findByOwnerId(currentUser().getId())
                .orElseThrow(() -> new AppException("Chưa có hồ sơ", NOT_FOUND));
        return vaccinationRepo.findByPetIdOrderByVaccinatedDateDesc(pet.getId());
    }

    // ── Photos ───────────────────────────────────────────
    @Override
    public PetPhoto addPhoto(String photoUrl, boolean setAsAvatar) {
        PetProfile pet = petProfileRepo.findByOwnerId(currentUser().getId())
                .orElseThrow(() -> new AppException("Chưa có hồ sơ", NOT_FOUND));

        if (petPhotoRepo.countByPetId(pet.getId()) >= 10)
            throw new AppException("Tối đa 10 ảnh", BAD_REQUEST);

        // Nếu set làm avatar → bỏ avatar cũ
        if (setAsAvatar) {
            petPhotoRepo.findByPetIdAndIsAvatarTrue(pet.getId())
                    .ifPresent(old -> { old.setIsAvatar(false); petPhotoRepo.save(old); });
        }

        PetPhoto photo = PetPhoto.builder()
                .pet(pet).photoUrl(photoUrl).isAvatar(setAsAvatar).build();
        return petPhotoRepo.save(photo);
    }

    @Override
    public void deletePhoto(UUID photoId) {
        PetPhoto photo = petPhotoRepo.findById(photoId)
                .orElseThrow(() -> new AppException("Không tìm thấy ảnh", NOT_FOUND));

        if (!photo.getPet().getOwner().getId().equals(currentUser().getId()))
            throw new AppException("Không có quyền xóa ảnh này", FORBIDDEN);

        // Xóa trên Cloudinary trước
        cloudinaryService.deleteImage(photo.getPhotoUrl());

        // Xóa trong DB
        petPhotoRepo.delete(photo);
    }

    // ── Mapper ───────────────────────────────────────────
    private PetProfileResponse toResponse(PetProfile p) {
        String avatarUrl = petPhotoRepo.findByPetIdAndIsAvatarTrue(p.getId())
                .map(PetPhoto::getPhotoUrl).orElse(null);
        List<String> photoUrls = petPhotoRepo.findByPetId(p.getId())
                .stream().map(PetPhoto::getPhotoUrl).toList();

        return PetProfileResponse.builder()
                .id(p.getId())
                .ownerId(p.getOwner().getId())
                .ownerName(p.getOwner().getFullName())
                .name(p.getName())
                .species(p.getSpecies())
                .breed(p.getBreed())
                .gender(p.getGender() != null ? p.getGender().name() : null)
                .dateOfBirth(p.getDateOfBirth())
                .weightKg(p.getWeightKg())
                .color(p.getColor())
                .size(p.getSize())
                .reproductiveStatus(p.getReproductiveStatus() != null ? p.getReproductiveStatus().name() : null)
                .isVaccinated(p.getIsVaccinated())
                .lastVaccineDate(p.getLastVaccineDate())
                .healthStatus(p.getHealthStatus() != null ? p.getHealthStatus().name() : null)
                .healthNotes(p.getHealthNotes())
                .personalityTags(p.getPersonalityTags())
                .lookingFor(p.getLookingFor() != null ? p.getLookingFor().name() : null)
                .notes(p.getNotes())
                .isHidden(p.getIsHidden())
                .avatarUrl(avatarUrl)
                .photoUrls(photoUrls)
                .createdAt(p.getCreatedAt())
                .build();
    }
}