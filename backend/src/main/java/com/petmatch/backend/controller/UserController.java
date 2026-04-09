package com.petmatch.backend.controller;

import com.petmatch.backend.dto.request.ChangePasswordRequest;
import com.petmatch.backend.dto.request.UpdateLocationRequest;
import com.petmatch.backend.dto.request.UpdateUserRequest;
import com.petmatch.backend.dto.response.UserResponse;
import com.petmatch.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Lấy thông tin tài khoản hiện tại */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo() {
        return ResponseEntity.ok(userService.getMyInfo());
    }

    /** Cập nhật thông tin cá nhân (fullName, phone, address) */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyInfo(
            @Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(userService.updateMyInfo(req));
    }

    /** Đổi mật khẩu */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(req);
        return ResponseEntity.noContent().build();
    }

    /** Upload avatar tài khoản (khác avatar pet) */
    @PostMapping("/me/avatar")
    public ResponseEntity<UserResponse> updateAvatar(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.updateAvatar(file));
    }

    /** Cập nhật vị trí GPS của user */
    @PatchMapping("/me/location")
    public ResponseEntity<Void> updateLocation(
            @RequestBody UpdateLocationRequest req) {
        userService.updateLocation(req.getLatitude(), req.getLongitude());
        return ResponseEntity.noContent().build();
    }
}
