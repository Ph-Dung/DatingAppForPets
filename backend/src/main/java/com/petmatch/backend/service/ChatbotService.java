package com.petmatch.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petmatch.backend.dto.request.ChatbotMessageRequest;
import com.petmatch.backend.dto.response.ChatbotResponse;
import com.petmatch.backend.dto.response.PetProfileResponse;
import com.petmatch.backend.entity.PetPhoto;
import com.petmatch.backend.entity.PetProfile;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.Gender;
import com.petmatch.backend.enums.HealthStatus;
import com.petmatch.backend.enums.LookingFor;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.PetPhotoRepository;
import com.petmatch.backend.repository.PetProfileRepository;
import com.petmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatbotService {

    private final PetProfileRepository petProfileRepo;
    private final PetPhotoRepository petPhotoRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api-key}")
    private String openAiApiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String openAiModel;

    private static final String SYSTEM_PROMPT = """
        Bạn là trợ lý AI của PetMatch — ứng dụng ghép đôi thú cưng.
        Nhiệm vụ: Hỏi người dùng về yêu cầu của thú cưng đối phương để tìm kiếm bạn phù hợp.
        
        CÁC THÔNG TIN CÓ THỂ TÌM KIẾM:
        - Loài (chó/mèo/thỏ/hamster...) - BẮT BUỘC PHẢI CÓ.
        - Giống/breed (ví dụ: Poodle, Husky, Mèo...) - Tuỳ chọn.
        - Giới tính (đực/cái) - Tuỳ chọn.
        - Độ tuổi (ví dụ: 1-3 tuổi) - Tuỳ chọn.
        - Cân nặng (ví dụ: 3-7kg) - Tuỳ chọn.
        - Tình trạng sức khỏe (khỏe mạnh/đang hồi phục...) - Tuỳ chọn.
        - Mục đích ghép đôi (phối giống/kết bạn/vui chơi) - Tuỳ chọn.
        - Khoảng cách (ví dụ: dưới 5km, xa gần) - Tuỳ chọn.
        
        QUAN TRỌNG:
        1. KHÔNG bắt buộc người dùng nhập hết mọi thông tin. Chỉ cần người dùng cung cấp Loài (và có thể 1-2 thông tin khác), hãy thực hiện SEARCH luôn để kết quả phong phú, không hỏi dồn dập.
        2. Nếu người dùng bảo "sao cũng được", "bất kỳ", "không quan trọng", hoặc không nhắc đến, hãy để giá trị trường đó là null trong JSON.
        3. Các trường số như "minAge", "maxDistanceKm" phải là số (Double/Integer) hoặc null. KHÔNG TRẢ VỀ CHUỖI.
        4. Tuyệt đối không trả về JSON SEARCH nếu người dùng chưa cung cấp LOÀI (species). Phải hỏi loài trước tiên.
        
        Khi trả về JSON:
        {
          "action": "SEARCH",
          "species": "chó",  // "chó", "mèo", "thỏ", "hamster"
          "breed": "Poodle", // hoặc null
          "gender": "FEMALE", // "MALE", "FEMALE" hoặc null
          "minAge": 1,        // hoặc null
          "maxAge": 3,        // hoặc null
          "minWeight": 3.0,   // hoặc null
          "maxWeight": 7.0,   // hoặc null
          "healthStatus": "HEALTHY", // "HEALTHY", "SICK", "RECOVERING", "CHRONIC" hoặc null
          "lookingFor": "BREEDING",  // "BREEDING", "FRIENDSHIP", "PLAY" hoặc null
          "maxDistanceKm": 5.0 // hoặc null
        }
        
        Trả lời thân thiện bằng tiếng Việt. Khi trả về JSON SEARCH, không kèm thêm text nào khác.
        """;

    public ChatbotResponse processMessage(List<ChatbotMessageRequest.ChatMessageDto> messages) {
        try {
            // Build messages cho OpenAI
            List<java.util.Map<String, String>> apiMessages = new ArrayList<>();
            apiMessages.add(java.util.Map.of("role", "system", "content", SYSTEM_PROMPT));
            for (var msg : messages) {
                apiMessages.add(java.util.Map.of("role", msg.getRole(), "content", msg.getContent()));
            }

            // Gọi OpenAI API
            String aiReply = callOpenAi(apiMessages);

            // Sử dụng Regex để "bóc" riêng phần JSON (đề phòng AI nói thừa văn bản kĩ thuật)
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{.*\\}", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher matcher = pattern.matcher(aiReply);

            if (matcher.find()) {
                String jsonPart = matcher.group();
                if (jsonPart.contains("\"action\"") && jsonPart.contains("SEARCH")) {
                    return handleSearch(jsonPart);
                }
            }

            return ChatbotResponse.builder()
                    .reply(aiReply)
                    .isReadyToSuggest(false)
                    .suggestions(List.of())
                    .build();

        } catch (Exception e) {
            throw new AppException("Lỗi AI: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ChatbotResponse handleSearch(String jsonReply) throws IOException {
        JsonNode node = objectMapper.readTree(jsonReply);

        // Parse tham số từ JSON AI trả về
        String species     = getTextOrNull(node, "species");
        String breed       = getTextOrNull(node, "breed");
        String genderStr   = getTextOrNull(node, "gender");
        Integer minAge     = (node.has("minAge") && node.get("minAge").isNumber()) ? node.get("minAge").asInt() : null;
        Integer maxAge     = (node.has("maxAge") && node.get("maxAge").isNumber()) ? node.get("maxAge").asInt() : null;
        Double minWeightD  = (node.has("minWeight") && node.get("minWeight").isNumber()) ? node.get("minWeight").asDouble() : null;
        Double maxWeightD  = (node.has("maxWeight") && node.get("maxWeight").isNumber()) ? node.get("maxWeight").asDouble() : null;
        String healthStr   = getTextOrNull(node, "healthStatus");
        String lookingStr  = getTextOrNull(node, "lookingFor");
        Double maxDistanceKm = (node.has("maxDistanceKm") && node.get("maxDistanceKm").isNumber()) ? node.get("maxDistanceKm").asDouble() : null;

        // Map species tiếng Việt → giá trị DB
        if (species != null) species = mapSpecies(species);

        // Map enums
        Gender gender = null;
        if (genderStr != null) {
            try { gender = Gender.valueOf(genderStr.toUpperCase()); } catch (Exception ignore) {
                if (genderStr.equalsIgnoreCase("đực") || genderStr.equalsIgnoreCase("male")) gender = Gender.MALE;
                else if (genderStr.equalsIgnoreCase("cái") || genderStr.equalsIgnoreCase("female")) gender = Gender.FEMALE;
            }
        }

        HealthStatus healthStatus = null;
        if (healthStr != null) {
            try { healthStatus = HealthStatus.valueOf(healthStr.toUpperCase()); } catch (Exception ignore) {}
        }

        LookingFor lookingFor = null;
        if (lookingStr != null) {
            try { lookingFor = LookingFor.valueOf(lookingStr.toUpperCase()); } catch (Exception ignore) {}
        }

        BigDecimal minWeight = minWeightD != null ? BigDecimal.valueOf(minWeightD) : null;
        BigDecimal maxWeight = maxWeightD != null ? BigDecimal.valueOf(maxWeightD) : null;

        // Convert age → dateOfBirth range
        LocalDate minDob = maxAge != null ? LocalDate.now().minusYears(maxAge) : null;
        LocalDate maxDob = minAge != null ? LocalDate.now().minusYears(minAge) : null;

        // Lấy userId hiện tại
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Long userId = userRepo.findByEmail(email).map(User::getId).orElse(-1L);

        String safeBreed = breed == null ? "" : breed;
        // Tìm kiếm
        int expandedSize = maxDistanceKm != null ? 30 : 3;
        var results = petProfileRepo.search(
                userId,
                species != null, species,
                safeBreed != null && !safeBreed.trim().isEmpty(), safeBreed,
                gender != null, gender,
                lookingFor != null, lookingFor,
                healthStatus != null, healthStatus,
                minWeight != null, minWeight,
                maxWeight != null, maxWeight,
                minDob != null, minDob,
                maxDob != null, maxDob,
                PageRequest.of(0, expandedSize)
        );

        User currentUser = userRepo.findByEmail(email).orElse(null);
        List<PetProfileResponse> suggestions;

        if (maxDistanceKm != null && currentUser != null && currentUser.getLatitude() != null) {
            suggestions = results.getContent().stream()
                    .filter(p -> p.getOwner().getLatitude() != null &&
                            haversineKm(currentUser.getLatitude(), currentUser.getLongitude(),
                                    p.getOwner().getLatitude(), p.getOwner().getLongitude()) <= maxDistanceKm)
                    .limit(3)
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } else {
            suggestions = results.getContent().stream()
                    .limit(3)
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        String replyText;
        if (suggestions.isEmpty()) {
            replyText = "Mình đã tìm kiếm nhưng hiện chưa có hồ sơ nào phù hợp với yêu cầu của bạn. Bạn có muốn thay đổi tiêu phí tìm kiếm không?";
        } else {
            replyText = "Mình tìm được " + suggestions.size() + " hồ sơ phù hợp! Bạn có thể xem và chọn bên dưới 👇";
        }

        return ChatbotResponse.builder()
                .reply(replyText)
                .isReadyToSuggest(!suggestions.isEmpty())
                .suggestions(suggestions)
                .build();
    }

    private String callOpenAi(List<java.util.Map<String, String>> messages) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "model", openAiModel,
                "messages", messages,
                "temperature", 0.7,
                "max_tokens", 500
        ));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(openAiBaseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openAiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new AppException("OpenAI API lỗi: " + response.body(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        JsonNode json = objectMapper.readTree(response.body());
        return json.get("choices").get(0).get("message").get("content").asText().trim();
    }

    private String getTextOrNull(JsonNode node, String field) {
        return (node.has(field) && !node.get(field).isNull()) ? node.get(field).asText() : null;
    }

    private String mapSpecies(String s) {
        return switch (s.toLowerCase().trim()) {
            case "chó", "dog"     -> "Chó";
            case "mèo", "cat"     -> "Mèo";
            case "thỏ", "rabbit"  -> "Thỏ";
            case "hamster"        -> "Hamster";
            default -> s;
        };
    }

    private PetProfileResponse toResponse(PetProfile p) {
        User currentUser = null;
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            currentUser = userRepo.findByEmail(email).orElse(null);
        } catch (Exception ignored) {}

        String avatarUrl = petPhotoRepo.findByPetIdAndIsAvatarTrue(p.getId())
                .map(PetPhoto::getPhotoUrl).orElse(null);
        List<String> photoUrls = petPhotoRepo.findByPetId(p.getId())
                .stream().map(PetPhoto::getPhotoUrl).toList();
        int age = p.getDateOfBirth() != null ? Period.between(p.getDateOfBirth(), LocalDate.now()).getYears() : 0;

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
                .healthStatus(p.getHealthStatus() != null ? p.getHealthStatus().name() : null)
                .healthNotes(p.getHealthNotes())
                .personalityTags(p.getPersonalityTags())
                .lookingFor(p.getLookingFor() != null ? p.getLookingFor().name() : null)
                .notes(p.getNotes())
                .isHidden(p.getIsHidden())
                .avatarUrl(avatarUrl)
                .photoUrls(photoUrls)
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
}
