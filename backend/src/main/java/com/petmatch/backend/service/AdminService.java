package com.petmatch.backend.service;

import com.petmatch.backend.dto.request.AdminHandleReportRequest;
import com.petmatch.backend.dto.response.*;
import com.petmatch.backend.entity.PetPhoto;
import com.petmatch.backend.entity.PetProfile;
import com.petmatch.backend.entity.Report;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.AdminReportAction;
import com.petmatch.backend.enums.ReportStatus;
import com.petmatch.backend.enums.ReportTargetType;
import com.petmatch.backend.enums.Role;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.MatchRequestRepository;
import com.petmatch.backend.repository.CommentRepository;
import com.petmatch.backend.repository.PetPhotoRepository;
import com.petmatch.backend.repository.PetProfileRepository;
import com.petmatch.backend.repository.PetVaccinationRepository;
import com.petmatch.backend.repository.PostRepository;
import com.petmatch.backend.repository.ReportRepository;
import com.petmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final PetProfileRepository petProfileRepository;
    private final PetVaccinationRepository vaccinationRepository;
    private final MatchRequestRepository matchRequestRepository;
    private final ReportRepository reportRepository;
    private final PetPhotoRepository petPhotoRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        return AdminDashboardResponse.builder()
                .totalUsers(userRepository.countByRole(Role.USER))
                .lockedUsers(userRepository.countByIsLockedTrue())
                .totalPets(petProfileRepository.count())
                .hiddenPets(petProfileRepository.countByIsHiddenTrue())
                .pendingReports(reportRepository.countByStatus(ReportStatus.PENDING))
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AdminUserItemResponse> getUsers(String query, Boolean locked, Boolean warned, int page, int size) {
        return userRepository.searchUsersForAdmin(Role.USER, query, locked, warned, PageRequest.of(page, size))
                .map(this::toUserItem);
    }

    @Transactional(readOnly = true)
    public Page<AdminPetItemResponse> getPets(String query, Boolean hidden, int page, int size) {
        return petProfileRepository.searchPetsForAdmin(query, hidden, PageRequest.of(page, size))
                .map(this::toPetItem);
    }

        @Transactional(readOnly = true)
        public AdminUserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException("Không tìm thấy user", HttpStatus.NOT_FOUND));

        List<AdminReportItemResponse> violations = new ArrayList<>(reportRepository
            .findByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTargetType.USER, userId)
            .stream()
            .map(this::toReportItem)
            .toList());

        petProfileRepository.findByOwnerId(userId).ifPresent(pet -> violations.addAll(
            reportRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTargetType.PET_PROFILE, pet.getId())
                .stream()
                .map(this::toReportItem)
                .toList()
        ));

        return AdminUserDetailResponse.builder()
            .user(toUserItem(user))
            .violations(violations)
            .build();
        }

        @Transactional(readOnly = true)
        public AdminPetDetailResponse getPetDetail(Long petId) {
        PetProfile pet = petProfileRepository.findById(petId)
            .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ thú cưng", HttpStatus.NOT_FOUND));

        List<AdminReportItemResponse> violations = reportRepository
            .findByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTargetType.PET_PROFILE, petId)
            .stream()
            .map(this::toReportItem)
            .toList();

        return AdminPetDetailResponse.builder()
            .pet(toPetProfileResponse(pet))
            .violations(violations)
            .build();
        }

    @Transactional(readOnly = true)
    public Page<AdminReportItemResponse> getReports(ReportStatus status, int page, int size) {
        Page<Report> reports = status == null
                ? reportRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                : reportRepository.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, size));

        return reports.map(this::toReportItem);
    }

    public AdminUserItemResponse setUserLocked(Long userId, boolean locked) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("Không tìm thấy user", HttpStatus.NOT_FOUND));

        return toUserItem(lockUserInternal(user, locked));
    }

    public AdminUserItemResponse warnUser(Long userId, String note) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("Không tìm thấy user", HttpStatus.NOT_FOUND));

        if (user.getRole() == Role.ADMIN) {
            throw new AppException("Không thể cảnh cáo tài khoản admin", HttpStatus.BAD_REQUEST);
        }

        user.setIsWarned(true);
        user.setWarningCount((user.getWarningCount() == null ? 0 : user.getWarningCount()) + 1);
        user.setLastWarnedAt(LocalDateTime.now());
        return toUserItem(userRepository.save(user));
    }

    public AdminPetItemResponse setPetHidden(Long petId, boolean hidden) {
        PetProfile pet = petProfileRepository.findById(petId)
                .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ thú cưng", HttpStatus.NOT_FOUND));
        pet.setIsHidden(hidden);
        return toPetItem(petProfileRepository.save(pet));
    }

    public AdminReportItemResponse handleReport(Long reportId, AdminHandleReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException("Không tìm thấy báo cáo", HttpStatus.NOT_FOUND));

        User admin = currentAdmin();
        AdminReportAction action = request.getAction();

        if (action == AdminReportAction.BAN_USER) {
            User targetUser = resolveTargetUser(report);
            lockUserInternal(targetUser, true);
            deleteCommunityContentByUser(targetUser.getId());
            report.setStatus(ReportStatus.RESOLVED);
        } else if (action == AdminReportAction.AUTO_DELETE_PHOTO) {
            if (report.getTargetType() == ReportTargetType.PET_PROFILE) {
                PetProfile pet = petProfileRepository.findById(report.getTargetId())
                        .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ thú cưng", HttpStatus.NOT_FOUND));
                petPhotoRepository.deleteAll(petPhotoRepository.findByPetId(pet.getId()));
            }
            report.setStatus(ReportStatus.RESOLVED);
        } else if (action == AdminReportAction.AUTO_DELETE_PET) {
            if (report.getTargetType() != ReportTargetType.PET_PROFILE) {
                throw new AppException("Chỉ áp dụng xoá hồ sơ thú cưng", HttpStatus.BAD_REQUEST);
            }
            deletePet(report.getTargetId());
            report.setStatus(ReportStatus.RESOLVED);
        } else if (action == AdminReportAction.DISMISS) {
            report.setStatus(ReportStatus.DISMISSED);
        } else {
            User targetUser = resolveTargetUser(report);
            targetUser.setIsWarned(true);
            targetUser.setWarningCount((targetUser.getWarningCount() == null ? 0 : targetUser.getWarningCount()) + 1);
            targetUser.setLastWarnedAt(LocalDateTime.now());
            userRepository.save(targetUser);
            deleteReportedContent(report);
            report.setStatus(ReportStatus.RESOLVED);
        }

        report.setHandledBy(admin);
        report.setAction(action);
        report.setAdminNote(request.getNote());
        report.setHandledAt(LocalDateTime.now());

        return toReportItem(reportRepository.save(report));
    }

    public void deletePet(Long petId) {
        PetProfile pet = petProfileRepository.findById(petId)
                .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ thú cưng", HttpStatus.NOT_FOUND));

        petPhotoRepository.deleteAll(petPhotoRepository.findByPetId(petId));
        vaccinationRepository.deleteAllByPetId(petId);
        matchRequestRepository.deleteBySenderPetIdOrReceiverPetId(petId, petId);
        petProfileRepository.delete(pet);
    }

    private User resolveTargetUser(Report report) {
        if (report.getTargetType() == ReportTargetType.USER) {
            return userRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new AppException("Không tìm thấy user bị báo cáo", HttpStatus.NOT_FOUND));
        }

        if (report.getTargetType() == ReportTargetType.PET_PROFILE) {
            PetProfile pet = petProfileRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ thú cưng", HttpStatus.NOT_FOUND));
            return pet.getOwner();
        }

        if (report.getTargetType() == ReportTargetType.POST) {
            return postRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new AppException("Không tìm thấy bài viết bị báo cáo", HttpStatus.NOT_FOUND))
                    .getUser();
        }

        if (report.getTargetType() == ReportTargetType.COMMENT) {
            return commentRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new AppException("Không tìm thấy bình luận bị báo cáo", HttpStatus.NOT_FOUND))
                    .getUser();
        }

        throw new AppException("Loại báo cáo này chưa hỗ trợ ban user", HttpStatus.BAD_REQUEST);
    }

    private User currentAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Không tìm thấy tài khoản admin", HttpStatus.NOT_FOUND));

        if (admin.getRole() != Role.ADMIN) {
            throw new AppException("Không có quyền admin", HttpStatus.FORBIDDEN);
        }
        return admin;
    }

    private User lockUserInternal(User user, boolean locked) {
        if (user.getRole() == Role.ADMIN) {
            throw new AppException("Không thể khóa tài khoản admin", HttpStatus.BAD_REQUEST);
        }

        user.setIsLocked(locked);
        User saved = userRepository.save(user);

        petProfileRepository.findByOwnerId(saved.getId()).ifPresent(pet -> {
            pet.setIsHidden(locked);
            petProfileRepository.save(pet);
        });
        return saved;
    }

    private void deleteReportedContent(Report report) {
        if (report.getTargetType() == ReportTargetType.POST) {
            postRepository.findById(report.getTargetId()).ifPresent(postRepository::delete);
            return;
        }

        if (report.getTargetType() == ReportTargetType.COMMENT) {
            commentRepository.findById(report.getTargetId()).ifPresent(commentRepository::delete);
        }
    }

    private void deleteCommunityContentByUser(Long userId) {
        commentRepository.deleteByUserId(userId);
        postRepository.deleteByUserId(userId);
    }

    private AdminUserItemResponse toUserItem(User user) {
        return AdminUserItemResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .locked(Boolean.TRUE.equals(user.getIsLocked()))
                .warned(Boolean.TRUE.equals(user.getIsWarned()))
                .warningCount(user.getWarningCount() == null ? 0 : user.getWarningCount())
                .lastWarnedAt(user.getLastWarnedAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AdminPetItemResponse toPetItem(PetProfile pet) {
        String avatarUrl = petPhotoRepository.findByPetIdAndIsAvatarTrue(pet.getId())
                .map(PetPhoto::getPhotoUrl)
                .orElse(null);

        return AdminPetItemResponse.builder()
                .id(pet.getId())
                .ownerId(pet.getOwner().getId())
                .ownerName(pet.getOwner().getFullName())
                .name(pet.getName())
                .species(pet.getSpecies())
                .avatarUrl(avatarUrl)
                .hidden(Boolean.TRUE.equals(pet.getIsHidden()))
                .createdAt(pet.getCreatedAt())
                .build();
    }

            private PetProfileResponse toPetProfileResponse(PetProfile pet) {
            String avatarUrl = petPhotoRepository.findByPetIdAndIsAvatarTrue(pet.getId())
                .map(PetPhoto::getPhotoUrl)
                .orElse(null);
            List<String> photoUrls = petPhotoRepository.findByPetId(pet.getId())
                .stream()
                .map(PetPhoto::getPhotoUrl)
                .toList();

            Integer age = null;
            if (pet.getDateOfBirth() != null) {
                age = Period.between(pet.getDateOfBirth(), LocalDate.now()).getYears();
            }

            return PetProfileResponse.builder()
                .id(pet.getId())
                .ownerId(pet.getOwner().getId())
                .ownerName(pet.getOwner().getFullName())
                .name(pet.getName())
                .species(pet.getSpecies())
                .breed(pet.getBreed())
                .gender(pet.getGender() != null ? pet.getGender().name() : null)
                .dateOfBirth(pet.getDateOfBirth())
                .age(age)
                .weightKg(pet.getWeightKg())
                .color(pet.getColor())
                .size(pet.getSize())
                .reproductiveStatus(pet.getReproductiveStatus() != null ? pet.getReproductiveStatus().name() : null)
                .isVaccinated(pet.getIsVaccinated())
                .lastVaccineDate(pet.getLastVaccineDate())
                .vaccinationCount((int) vaccinationRepository.countByPetId(pet.getId()))
                .healthStatus(pet.getHealthStatus() != null ? pet.getHealthStatus().name() : null)
                .healthNotes(pet.getHealthNotes())
                .personalityTags(pet.getPersonalityTags())
                .lookingFor(pet.getLookingFor() != null ? pet.getLookingFor().name() : null)
                .notes(pet.getNotes())
                .isHidden(pet.getIsHidden())
                .avatarUrl(avatarUrl)
                .photoUrls(photoUrls)
                .createdAt(pet.getCreatedAt())
                .build();
            }

    private AdminReportItemResponse toReportItem(Report report) {
        return AdminReportItemResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getFullName())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .handledById(report.getHandledBy() != null ? report.getHandledBy().getId() : null)
                .handledByName(report.getHandledBy() != null ? report.getHandledBy().getFullName() : null)
                .action(report.getAction())
                .adminNote(report.getAdminNote())
                .handledAt(report.getHandledAt())
                .build();
    }
}
