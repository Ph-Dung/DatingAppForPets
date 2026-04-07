package com.petmatch.backend.config;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.petmatch.backend.entity.PetPhoto;
import com.petmatch.backend.entity.PetProfile;
import com.petmatch.backend.entity.Post;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.Gender;
import com.petmatch.backend.enums.HealthStatus;
import com.petmatch.backend.enums.LookingFor;
import com.petmatch.backend.enums.ReproductiveStatus;
import com.petmatch.backend.enums.Role;
import com.petmatch.backend.repository.PetPhotoRepository;
import com.petmatch.backend.repository.PetProfileRepository;
import com.petmatch.backend.repository.PostRepository;
import com.petmatch.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PetProfileRepository petProfileRepo;
    private final PetPhotoRepository petPhotoRepo;
    private final PostRepository postRepo;
    private final PasswordEncoder passwordEncoder;
    private final Cloudinary cloudinary;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.users-count:30}")
    private int usersSeedCount;

    @Value("${app.seed.skip-when-users-gte:5}")
    private long skipWhenUsersGte;

    @Value("${app.seed.upload-photos:false}")
    private boolean uploadPhotos;

    @Value("${app.seed.community-posts.enabled:true}")
    private boolean communityPostsEnabled;

    private static final Random RNG = new Random(42);

    // ── Data pools ────────────────────────────────────────────
    private static final String[] DOG_BREEDS = {
            "Poodle", "Golden Retriever", "Husky", "Corgi", "Shiba Inu",
            "Phú Quốc", "Chow Chow", "Labrador", "Chihuahua", "Bulldog Pháp",
            "Bichon", "Pomeranian", "Dachshund", "Beagle", "Lai"
    };
    private static final String[] CAT_BREEDS = {
            "Anh lông ngắn", "Ba Tư", "Munchkin", "Ragdoll", "Maine Coon",
            "Scottish Fold", "Siamese", "Bengal", "Mèo ta", "Lai"
    };
    private static final String[] RABBIT_BREEDS = {
            "Holland Lop", "Mini Rex", "Angora", "Flemish Giant", "Lai"
    };
    private static final String[] HAMSTER_BREEDS = {
            "Syrian", "Dwarf Roborovski", "Djungarian", "Chinese"
    };
    private static final String[] COLORS = {
            "Trắng", "Đen", "Nâu", "Vàng", "Xám", "Nâu đen", "Trắng đen", "Kem"
    };
    private static final String[] PERSONALITY_OPTIONS = {
            "Năng động", "Thân thiện", "Lười biếng", "Tinh nghịch", "Ngoan ngoãn",
            "Nhút nhát", "Thích ôm", "Độc lập", "Ham ăn", "Thích chơi",
            "Yên lặng", "Thông minh", "Tò mò", "Bảo vệ chủ"
    };
    private static final String[] MALE_NAMES = {
            "Max", "Buddy", "Charlie", "Rocky", "Bear", "Duke", "Milo",
            "Simba", "Leo", "Coco", "Tuấn", "Kobe", "Shadow", "Titan", "Thor",
            "Bông", "Bi", "Tom", "Jerry", "Chip"
    };
    private static final String[] FEMALE_NAMES = {
            "Bella", "Luna", "Daisy", "Molly", "Lola", "Chloe", "Sophie",
            "Lily", "Nala", "Sakura", "Mi", "Bé", "Trúc", "Hoa", "Tuyết",
            "Nemo", "Mochi", "Peach", "Pearl", "Ruby"
    };

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("DataSeeder: app.seed.enabled=false, bỏ qua toàn bộ seeding.");
            return;
        }

        if (communityPostsEnabled) {
            seedCommunityPostsIfNeeded();
        }

        long count = userRepo.count();
        if (count >= skipWhenUsersGte) {
            log.info("DataSeeder: DB đã có {} users, bỏ qua seeding.", count);
            return;
        }

        int totalUsersToSeed = Math.max(0, usersSeedCount);
        if (totalUsersToSeed == 0) {
            log.info("DataSeeder: app.seed.users-count=0, bỏ qua seeding user.");
            if (communityPostsEnabled) {
                seedCommunityPostsIfNeeded();
            }
            return;
        }

        log.info("DataSeeder: Đang tạo {} tài khoản test...", totalUsersToSeed);
        String encodedPass = passwordEncoder.encode("12345678");

        for (int i = 1; i <= totalUsersToSeed; i++) {
            try {
                // 1. Tạo User
                User user = userRepo.save(User.builder()
                        .fullName(randomFullName())
                        .email("user" + i + "@gmail.com")
                        .passwordHash(encodedPass)
                        .phone("09" + String.format("%08d", RNG.nextInt(100000000)))
                        .role(Role.USER)
                        .isLocked(false)
                        .build());

                // 2. Chọn species theo tỷ lệ: Chó 50%, Mèo 30%, Thỏ 10%, Hamster 10%
                String species;
                String[] breeds;
                int roll = RNG.nextInt(100);
                if (roll < 50) {
                    species = "Chó";
                    breeds = DOG_BREEDS;
                } else if (roll < 80) {
                    species = "Mèo";
                    breeds = CAT_BREEDS;
                } else if (roll < 90) {
                    species = "Thỏ";
                    breeds = RABBIT_BREEDS;
                } else {
                    species = "Hamster";
                    breeds = HAMSTER_BREEDS;
                }

                Gender gender = RNG.nextBoolean() ? Gender.MALE : Gender.FEMALE;
                String petName = randomPetName(gender, i);
                int ageYears = 1 + RNG.nextInt(9); // 1-9 tuổi
                LocalDate dob = LocalDate.now().minusYears(ageYears).minusDays(RNG.nextInt(365));
                BigDecimal weight = randomWeight(species);

                // HealthStatus phân bổ: 80% HEALTHY
                HealthStatus health;
                int hr = RNG.nextInt(100);
                if (hr < 80)
                    health = HealthStatus.HEALTHY;
                else if (hr < 90)
                    health = HealthStatus.RECOVERING;
                else if (hr < 95)
                    health = HealthStatus.SICK;
                else
                    health = HealthStatus.CHRONIC;

                // LookingFor: 40% BREEDING, 40% FRIENDSHIP, 20% PLAY
                LookingFor lookingFor;
                int lr = RNG.nextInt(100);
                if (lr < 40)
                    lookingFor = LookingFor.BREEDING;
                else if (lr < 80)
                    lookingFor = LookingFor.FRIENDSHIP;
                else
                    lookingFor = LookingFor.PLAY;

                ReproductiveStatus repro = RNG.nextBoolean()
                        ? ReproductiveStatus.INTACT
                        : (gender == Gender.MALE ? ReproductiveStatus.NEUTERED : ReproductiveStatus.SPAYED);

                // Personality tags (random 2-5)
                List<String> tags = pickRandom(PERSONALITY_OPTIONS, 2 + RNG.nextInt(4));
                String tagsJson = "[" + tags.stream().map(t -> "\"" + t + "\"")
                        .reduce((a, b) -> a + "," + b).orElse("") + "]";

                // 3. Tạo PetProfile
                PetProfile pet = petProfileRepo.save(PetProfile.builder()
                        .owner(user)
                        .name(petName)
                        .species(species)
                        .breed(breeds[RNG.nextInt(breeds.length)])
                        .gender(gender)
                        .dateOfBirth(dob)
                        .weightKg(weight)
                        .color(COLORS[RNG.nextInt(COLORS.length)])
                        .size(randomSize(weight))
                        .reproductiveStatus(repro)
                        .isVaccinated(RNG.nextBoolean())
                        .healthStatus(health)
                        .personalityTags(tagsJson)
                        .lookingFor(lookingFor)
                        .notes("Thú cưng dễ thương cần tìm bạn đôi")
                        .isHidden(false)
                        .build());

                // 4. Tạo 1-2 ảnh mẫu
                int photoCount = 1 + RNG.nextInt(2);
                boolean firstPhoto = true;
                for (int p = 0; p < photoCount; p++) {
                    String photoUrl = uploadRandomPhoto(species, i * 100 + p);
                    if (photoUrl != null) {
                        petPhotoRepo.save(PetPhoto.builder()
                                .pet(pet)
                                .photoUrl(photoUrl)
                                .isAvatar(firstPhoto)
                                .build());
                        firstPhoto = false;
                    }
                }

                if (i % 10 == 0)
                    log.info("DataSeeder: Đã tạo {}/{} tài khoản...", i, totalUsersToSeed);

            } catch (Exception e) {
                log.warn("DataSeeder: Lỗi khi tạo user{}: {}", i, e.getMessage());
            }
        }
        log.info("DataSeeder: Hoàn thành! Đã tạo {} tài khoản test.", totalUsersToSeed);
        if (communityPostsEnabled) {
            seedCommunityPostsIfNeeded();
        }
    }

    private void seedCommunityPostsIfNeeded() {
        if (!communityPostsEnabled) {
            return;
        }

        if (postRepo.count() > 0) {
            return;
        }

        List<User> users = userRepo.findAll();
        if (users.isEmpty()) {
            log.info("DataSeeder: Chưa có user để seed community posts.");
            return;
        }

        List<String> seedContents = List.of(
                "Mới dắt boss đi dạo công viên, bạn nào quanh khu Hà Đông muốn giao lưu không?",
                "Hôm nay mèo nhà mình ăn ngoan lắm, chia sẻ chút năng lượng tích cực cho mọi người.",
                "Cuối tuần này có ai cho pet đi cafe không, cùng hẹn một buổi nhé!"
        );

        List<String> seedImages = List.of(
                "https://images.unsplash.com/photo-1537151608828-ea2b11777ee8",
                "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba",
                "https://images.unsplash.com/photo-1495360010541-f48722b34f7d"
        );

        List<String> seedLocations = List.of("Hà Đông, Hà Nội", "Cầu Giấy, Hà Nội", "Đống Đa, Hà Nội");

        users.sort(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        for (int i = 0; i < 3; i++) {
            User owner = users.get(i % users.size());
            postRepo.save(Post.builder()
                    .user(owner)
                    .content(seedContents.get(i))
                    .imageUrl(seedImages.get(i))
                    .location(seedLocations.get(i))
                    .build());
        }

        log.info("DataSeeder: Đã seed 3 community posts mẫu.");
    }

    private String uploadRandomPhoto(String species, int seed) {
        String imageUrl = randomImageUrl(species, seed);
        if (!uploadPhotos) {
            return imageUrl;
        }

        try {
            // Download ảnh
            byte[] imageBytes;
            try (InputStream is = java.net.URI.create(imageUrl).toURL().openStream()) {
                imageBytes = is.readAllBytes();
            }

            // Upload lên Cloudinary
            Map<?, ?> result = cloudinary.uploader().upload(imageBytes,
                    ObjectUtils.asMap(
                            "folder", "petmatch/pets",
                            "resource_type", "image"));
            return result.get("secure_url").toString();

        } catch (IOException e) {
            log.warn("DataSeeder: Không thể upload ảnh: {}", e.getMessage());
            // Fallback: trả về URL trực tiếp không qua Cloudinary
            return imageUrl;
        }
    }

    private String randomImageUrl(String species, int seed) {
        String safeSpecies = species == null ? "" : species;
        return switch (safeSpecies) {
            case "Chó" -> "https://loremflickr.com/400/500/dog?lock=" + (seed % 50);
            case "Mèo" -> "https://loremflickr.com/400/500/cat?lock=" + seed;
            case "Thỏ" -> "https://picsum.photos/seed/rabbit" + seed + "/400/500";
            case "Hamster" -> "https://picsum.photos/seed/hamster" + seed + "/400/500";
            default -> "https://picsum.photos/seed/pet" + seed + "/400/500";
        };
    }

    private String randomPetName(Gender gender, int seed) {
        String[] pool = gender == Gender.MALE ? MALE_NAMES : FEMALE_NAMES;
        return pool[seed % pool.length] + " " + (seed + 1);
    }

    private BigDecimal randomWeight(String species) {
        double w = switch (species) {
            case "Chó" -> 2.0 + RNG.nextDouble() * 28.0; // 2-30kg
            case "Mèo" -> 2.0 + RNG.nextDouble() * 6.0; // 2-8kg
            case "Thỏ" -> 0.8 + RNG.nextDouble() * 3.2; // 0.8-4kg
            case "Hamster" -> 0.03 + RNG.nextDouble() * 0.12; // 30-150g
            default -> 1.0 + RNG.nextDouble() * 5.0;
        };
        return BigDecimal.valueOf(w).setScale(2, RoundingMode.HALF_UP);
    }

    private String randomSize(BigDecimal weight) {
        double w = weight.doubleValue();
        if (w < 5.0)
            return "small";
        if (w < 15.0)
            return "medium";
        return "large";
    }

    private String randomFullName() {
        String[] lastNames = { "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Vũ", "Phan", "Đặng", "Bùi" };
        String[] midNames = { "Văn", "Thị", "Hoàng", "Minh", "Quốc", "Thành", "Ngọc", "Như" };
        String[] firstNames = { "An", "Bình", "Chi", "Dung", "Em", "Giang", "Hà", "Hùng",
                "Khoa", "Lan", "Linh", "Mai", "Nam", "Nga", "Phi", "Quân",
                "Sơn", "Thảo", "Trung", "Uyên", "Việt", "Yến" };
        return lastNames[RNG.nextInt(lastNames.length)] + " "
                + midNames[RNG.nextInt(midNames.length)] + " "
                + firstNames[RNG.nextInt(firstNames.length)];
    }

    private List<String> pickRandom(String[] arr, int count) {
        List<String> list = new ArrayList<>(Arrays.asList(arr));
        Collections.shuffle(list, RNG);
        return list.subList(0, Math.min(count, list.size()));
    }
}
