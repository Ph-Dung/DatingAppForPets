package com.petmatch.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petmatch.backend.entity.PetProfile;
import com.petmatch.backend.entity.SwipePreference;
import com.petmatch.backend.enums.Gender;
import com.petmatch.backend.enums.HealthStatus;
import com.petmatch.backend.enums.MatchStatus;
import com.petmatch.backend.repository.MatchRequestRepository;
import com.petmatch.backend.repository.SwipePreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMatchingService {

    private final MatchRequestRepository matchRequestRepo;
    private final SwipePreferenceRepository swipePrefRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Cập nhật SwipePreference của pet dựa trên lịch sử like.
     * Gọi sau mỗi lần user swipe LIKE.
     */
    @Transactional
    public void updatePreferences(Long myPetId) {
        List<PetProfile> likedPets = matchRequestRepo.findLikedPetsBySenderPetId(myPetId);
        if (likedPets.isEmpty()) return;

        long totalLikes = likedPets.size();

        // ── Breed preference ──────────────────────────────────
        Map<String, Long> breedCounts = likedPets.stream()
                .filter(p -> p.getBreed() != null)
                .collect(Collectors.groupingBy(PetProfile::getBreed, Collectors.counting()));

        // Chỉ giữ các breed xuất hiện ≥ 20% lần
        List<String> preferredBreeds = breedCounts.entrySet().stream()
                .filter(e -> e.getValue() * 5 >= totalLikes)   // ≥ 20%
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        String breedsJson = null;
        if (!preferredBreeds.isEmpty()) {
            try { breedsJson = objectMapper.writeValueAsString(preferredBreeds); }
            catch (Exception ignore) {}
        }

        // ── Gender preference ─────────────────────────────────
        Map<Gender, Long> genderCounts = likedPets.stream()
                .filter(p -> p.getGender() != null)
                .collect(Collectors.groupingBy(PetProfile::getGender, Collectors.counting()));

        String preferredGender = null;
        if (!genderCounts.isEmpty()) {
            Optional<Map.Entry<Gender, Long>> topGender = genderCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue());
            // Chỉ đặt preference nếu ≥ 70% like cùng giới
            if (topGender.isPresent() && topGender.get().getValue() * 10 >= totalLikes * 7) {
                preferredGender = topGender.get().getKey().name();
            }
        }

        // ── Weight preference ─────────────────────────────────
        OptionalDouble avgWeightOpt = likedPets.stream()
                .filter(p -> p.getWeightKg() != null)
                .mapToDouble(p -> p.getWeightKg().doubleValue())
                .average();

        // ── Age preference ────────────────────────────────────
        OptionalDouble avgAgeOpt = likedPets.stream()
                .filter(p -> p.getDateOfBirth() != null)
                .mapToDouble(p -> Period.between(p.getDateOfBirth(), LocalDate.now()).getYears())
                .average();

        // ── Health preference ─────────────────────────────────
        long healthyCount = likedPets.stream()
                .filter(p -> p.getHealthStatus() == HealthStatus.HEALTHY)
                .count();
        Boolean preferHealthy = (healthyCount * 10 >= totalLikes * 8); // ≥ 80% like HEALTHY

        // ── LookingFor preference ─────────────────────────────
        Map<String, Long> lookingForCounts = likedPets.stream()
                .filter(p -> p.getLookingFor() != null)
                .collect(Collectors.groupingBy(p -> p.getLookingFor().name(), Collectors.counting()));

        String preferredLookingFor = null;
        Optional<Map.Entry<String, Long>> topLf = lookingForCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue());
        if (topLf.isPresent() && topLf.get().getValue() * 10 >= totalLikes * 6) { // ≥ 60%
            preferredLookingFor = topLf.get().getKey();
        }

        // ── Lưu / cập nhật SwipePreference ───────────────────
        SwipePreference pref = swipePrefRepo.findById(myPetId)
                .orElse(SwipePreference.builder().petId(myPetId).build());

        pref.setPreferredBreeds(breedsJson);
        pref.setPreferredGender(preferredGender);
        pref.setAvgWeight(avgWeightOpt.isPresent() ? avgWeightOpt.getAsDouble() : null);
        pref.setAvgAge(avgAgeOpt.isPresent() ? avgAgeOpt.getAsDouble() : null);
        pref.setPreferHealthy(preferHealthy);
        pref.setPreferredLookingFor(preferredLookingFor);
        pref.setTotalLikes((int) totalLikes);

        swipePrefRepo.save(pref);
    }

    /**
     * Kiểm tra pet đã đủ dữ liệu để dùng smart scoring (≥ 5 likes).
     */
    @Transactional(readOnly = true)
    public boolean isSmartModeReady(Long myPetId) {
        long likes = matchRequestRepo.countBySenderPetIdAndStatus(myPetId, MatchStatus.ACCEPTED);
        return likes >= 5;
    }

    /**
     * Tính điểm phù hợp giữa preference và candidate pet.
     * Tổng điểm tối đa = 100
     */
    @Transactional(readOnly = true)
    public int scorePet(Long myPetId, PetProfile candidate) {
        SwipePreference pref = swipePrefRepo.findById(myPetId).orElse(null);
        if (pref == null) return 50; // default neutral score

        int score = 0;

        // ── Breed match (0-30) ────────────────────────────────
        if (pref.getPreferredBreeds() != null && candidate.getBreed() != null) {
            try {
                List<String> preferred = objectMapper.readValue(
                        pref.getPreferredBreeds(), new TypeReference<>() {});
                boolean matched = preferred.stream()
                        .anyMatch(b -> b.equalsIgnoreCase(candidate.getBreed()));
                score += matched ? 30 : 0;
            } catch (Exception ignore) {}
        } else {
            score += 15; // neutral khi không có preference
        }

        // ── Gender match (0-15) ───────────────────────────────
        if (pref.getPreferredGender() != null && candidate.getGender() != null) {
            score += pref.getPreferredGender().equalsIgnoreCase(candidate.getGender().name()) ? 15 : 0;
        } else {
            score += 7; // neutral
        }

        // ── Weight proximity (0-15) ───────────────────────────
        if (pref.getAvgWeight() != null && candidate.getWeightKg() != null) {
            double diff = Math.abs(pref.getAvgWeight() - candidate.getWeightKg().doubleValue());
            double normalizedDiff = diff / Math.max(pref.getAvgWeight(), 1.0);
            score += (int) Math.max(0, 15 - normalizedDiff * 15);
        } else {
            score += 7;
        }

        // ── Age proximity (0-15) ──────────────────────────────
        if (pref.getAvgAge() != null && candidate.getDateOfBirth() != null) {
            int candidateAge = Period.between(candidate.getDateOfBirth(), LocalDate.now()).getYears();
            double ageDiff = Math.abs(pref.getAvgAge() - candidateAge);
            score += (int) Math.max(0, 15 - ageDiff * 2);
        } else {
            score += 7;
        }

        // ── Health status (0-10) ──────────────────────────────
        if (candidate.getHealthStatus() != null) {
            score += switch (candidate.getHealthStatus()) {
                case HEALTHY    -> 10;
                case RECOVERING -> pref.getPreferHealthy() != null && pref.getPreferHealthy() ? 3 : 7;
                case SICK       -> pref.getPreferHealthy() != null && pref.getPreferHealthy() ? 0 : 4;
                case CHRONIC    -> pref.getPreferHealthy() != null && pref.getPreferHealthy() ? 0 : 2;
                default         -> 5;
            };
        } else {
            score += 5;
        }

        // ── LookingFor match (0-10) ───────────────────────────
        if (pref.getPreferredLookingFor() != null && candidate.getLookingFor() != null) {
            score += pref.getPreferredLookingFor().equalsIgnoreCase(candidate.getLookingFor().name()) ? 10 : 0;
        } else {
            score += 5;
        }

        // ── Vaccination bonus (0-5) ───────────────────────────
        if (Boolean.TRUE.equals(candidate.getIsVaccinated())) score += 5;

        return Math.min(score, 100);
    }
}
