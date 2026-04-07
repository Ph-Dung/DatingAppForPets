package com.petmatch.backend.service;

import com.petmatch.backend.dto.request.ChangePasswordRequest;
import com.petmatch.backend.dto.request.UpdateUserRequest;
import com.petmatch.backend.dto.response.UserResponse;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

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
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .address(u.getAddress())
                .avatarUrl(u.getAvatarUrl())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
