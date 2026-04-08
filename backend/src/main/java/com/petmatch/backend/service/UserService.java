package com.petmatch.backend.service;

import com.petmatch.backend.dto.request.ChangePasswordRequest;
import com.petmatch.backend.dto.request.UpdateUserRequest;
import com.petmatch.backend.dto.response.UserResponse;
import com.petmatch.backend.entity.Report;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.AdminReportAction;
import com.petmatch.backend.enums.ReportTargetType;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.PetProfileRepository;
import com.petmatch.backend.repository.ReportRepository;
import com.petmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private static final String DEFAULT_WARNING_MESSAGE =
            "Tài khoản của bạn đã bị người dùng báo cáo. Vui lòng tuân thủ đúng quy định cộng đồng.";

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
    private final ReportRepository reportRepository;
    private final PetProfileRepository petProfileRepository;

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException("User không tồn tại", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserResponse getMyInfo() {
        return toResponse(currentUser());
    }

    public UserResponse updateMyInfo(UpdateUserRequest req) {
        User user = currentUser();
        user.setFullName(req.getFullName());
        user.setPhone(req.getPhone());
        user.setAddress(req.getAddress());
        return toResponse(userRepo.save(user));
    }

    public void changePassword(ChangePasswordRequest req) {
        User user = currentUser();
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
            throw new AppException("Mật khẩu cũ không đúng", HttpStatus.BAD_REQUEST);
        }
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(user);
    }

    public UserResponse updateAvatar(MultipartFile file) {
        User user = currentUser();
        String url = cloudinaryService.uploadImage(file, "petmatch/users");
        user.setAvatarUrl(url);
        return toResponse(userRepo.save(user));
    }

    /** Lưu tọa độ GPS của user hiện tại */
    public void updateLocation(double latitude, double longitude) {
        User user = currentUser();
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        userRepo.save(user);
    }

    private UserResponse toResponse(User u) {
        String warningMessage = resolveWarningMessage(u);
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .address(u.getAddress())
                .avatarUrl(u.getAvatarUrl())
                .warned(Boolean.TRUE.equals(u.getIsWarned()))
                .warningCount(u.getWarningCount() == null ? 0 : u.getWarningCount())
                .lastWarnedAt(u.getLastWarnedAt())
                .warningMessage(warningMessage)
                .createdAt(u.getCreatedAt())
                .build();
    }

    private String resolveWarningMessage(User user) {
        if (!Boolean.TRUE.equals(user.getIsWarned())) {
            return null;
        }

        Report latestUserWarning = latestWarningReport(ReportTargetType.USER, user.getId());
        Report latestPetWarning = petProfileRepository.findByOwnerId(user.getId())
                .map(pet -> latestWarningReport(ReportTargetType.PET_PROFILE, pet.getId()))
                .orElse(null);

        Report latest = newest(latestUserWarning, latestPetWarning);
        if (latest == null || latest.getAdminNote() == null || latest.getAdminNote().isBlank()) {
            return DEFAULT_WARNING_MESSAGE;
        }
        return latest.getAdminNote();
    }

    private Report latestWarningReport(ReportTargetType type, Long targetId) {
        return reportRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(type, targetId)
                .stream()
                .filter(report -> report.getAction() == AdminReportAction.WARN_USER)
                .findFirst()
                .orElse(null);
    }

    private Report newest(Report first, Report second) {
        if (first == null) return second;
        if (second == null) return first;

        LocalDateTime firstTime = first.getHandledAt() != null ? first.getHandledAt() : first.getCreatedAt();
        LocalDateTime secondTime = second.getHandledAt() != null ? second.getHandledAt() : second.getCreatedAt();

        if (firstTime == null) return second;
        if (secondTime == null) return first;
        return firstTime.isAfter(secondTime) ? first : second;
    }
}
