package com.petmatch.backend.controller;

import com.petmatch.backend.dto.request.AdminHandleReportRequest;
import com.petmatch.backend.dto.response.AdminDashboardResponse;
import com.petmatch.backend.dto.response.AdminPetDetailResponse;
import com.petmatch.backend.dto.response.AdminPetItemResponse;
import com.petmatch.backend.dto.response.AdminReportItemResponse;
import com.petmatch.backend.dto.response.AdminUserDetailResponse;
import com.petmatch.backend.dto.response.AdminUserItemResponse;
import com.petmatch.backend.enums.ReportStatus;
import com.petmatch.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
        /**
         * Controller quản lý chức năng dành cho admin.
         * Bao gồm: dashboard, quản lý user, quản lý hồ sơ thú cưng,
         * xử lý báo cáo (reports) và các hành động quản trị khác.
         * Tất cả endpoint trả về các DTO chuẩn hoá do `AdminService` cung cấp.
         */
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> dashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserItemResponse>> users(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean locked,
            @RequestParam(required = false) Boolean warned,
            @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                // Trả về trang danh sách user theo điều kiện tìm kiếm/lọc
                return ResponseEntity.ok(adminService.getUsers(query, locked, warned, page, size));
    }

    @PatchMapping("/users/{userId}/lock")
    public ResponseEntity<AdminUserItemResponse> lockUser(
            @PathVariable Long userId,
                        @RequestParam boolean locked) {
                // Khoá hoặc mở khoá tài khoản user
                return ResponseEntity.ok(adminService.setUserLocked(userId, locked));
    }

    @PostMapping("/users/{userId}/warn")
    public ResponseEntity<AdminUserItemResponse> warnUser(
            @PathVariable Long userId,
                        @RequestParam(required = false) String note) {
                // Cảnh cáo user (tăng warning count và đánh dấu)
                return ResponseEntity.ok(adminService.warnUser(userId, note));
    }

    @GetMapping("/users/{userId}")
        public ResponseEntity<AdminUserDetailResponse> userDetail(@PathVariable Long userId) {
                // Lấy chi tiết user và các vi phạm liên quan
                return ResponseEntity.ok(adminService.getUserDetail(userId));
    }

    @GetMapping("/pets")
    public ResponseEntity<Page<AdminPetItemResponse>> pets(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean hidden,
            @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                // Danh sách hồ sơ thú cưng cho admin (có thể lọc theo ẩn/hiện)
                return ResponseEntity.ok(adminService.getPets(query, hidden, page, size));
    }

    @PatchMapping("/pets/{petId}/hidden")
    public ResponseEntity<AdminPetItemResponse> hidePet(
            @PathVariable Long petId,
                        @RequestParam boolean hidden) {
                // Ẩn/hiện hồ sơ thú cưng
                return ResponseEntity.ok(adminService.setPetHidden(petId, hidden));
    }

    @GetMapping("/pets/{petId}")
        public ResponseEntity<AdminPetDetailResponse> petDetail(@PathVariable Long petId) {
                // Chi tiết hồ sơ thú cưng và các vi phạm liên quan
                return ResponseEntity.ok(adminService.getPetDetail(petId));
    }

    @DeleteMapping("/pets/{petId}")
    public ResponseEntity<Void> deletePet(@PathVariable Long petId) {
        adminService.deletePet(petId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports")
    public ResponseEntity<Page<AdminReportItemResponse>> reports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                // Lấy danh sách báo cáo để admin xử lý
                return ResponseEntity.ok(adminService.getReports(status, page, size));
    }

    @PostMapping("/reports/{reportId}/handle")
    public ResponseEntity<AdminReportItemResponse> handleReport(
            @PathVariable Long reportId,
                        @Valid @RequestBody AdminHandleReportRequest request) {
                // Xử lý báo cáo với hành động do admin chọn
                return ResponseEntity.ok(adminService.handleReport(reportId, request));
    }
}
