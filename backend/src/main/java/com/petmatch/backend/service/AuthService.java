package com.petmatch.backend.service;


import com.petmatch.backend.dto.request.LoginRequest;
import com.petmatch.backend.dto.request.RegisterRequest;
import com.petmatch.backend.dto.response.AuthResponse;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.Role;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.PetProfileRepository;
import com.petmatch.backend.repository.UserRepository;
import com.petmatch.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepo;
    private final PetProfileRepository petProfileRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;

    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new AppException("Email đã được sử dụng", HttpStatus.CONFLICT);

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .passwordHash(encoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(Role.USER)
                .isLocked(false)
                .build();
        userRepo.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userId(user.getId())
                .hasPetProfile(false)
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (LockedException e) {
            throw new AppException("Tài khoản đã bị khoá vui lòng liên hệ admin", HttpStatus.FORBIDDEN);
        } catch (BadCredentialsException e) {
            throw new AppException("Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new AppException("Không tìm thấy user", HttpStatus.NOT_FOUND));

        if (user.getIsLocked())
            throw new AppException("Tài khoản đã bị khoá vui lòng liên hệ admin", HttpStatus.FORBIDDEN);

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userId(user.getId())
                .hasPetProfile(petProfileRepo.existsByOwnerId(user.getId()))
                .build();
    }

    public AuthResponse loginAdmin(LoginRequest req) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (LockedException e) {
            throw new AppException("Tài khoản đã bị khoá vui lòng liên hệ admin", HttpStatus.FORBIDDEN);
        } catch (BadCredentialsException e) {
            throw new AppException("Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new AppException("Không tìm thấy user", HttpStatus.NOT_FOUND));

        if (user.getIsLocked())
            throw new AppException("Tài khoản đã bị khoá vui lòng liên hệ admin", HttpStatus.FORBIDDEN);

        if (user.getRole() != Role.ADMIN)
            throw new AppException("Tài khoản không có quyền admin", HttpStatus.FORBIDDEN);

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userId(user.getId())
                .hasPetProfile(true)
                .build();
    }
}