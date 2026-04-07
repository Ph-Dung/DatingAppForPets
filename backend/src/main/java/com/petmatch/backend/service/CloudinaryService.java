package com.petmatch.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.petmatch.backend.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload ảnh từ MultipartFile
     * folder: "petmatch/pets" hoặc "petmatch/users"
     * Trả về URL của ảnh sau khi upload
     */
    public String uploadImage(MultipartFile file, String folder) {
        // Validate file
        if (file.isEmpty())
            throw new AppException("File không được rỗng", HttpStatus.BAD_REQUEST);

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            throw new AppException("Chỉ chấp nhận file ảnh", HttpStatus.BAD_REQUEST);

        if (file.getSize() > 5 * 1024 * 1024)   // 5MB
            throw new AppException("File không được vượt quá 5MB", HttpStatus.BAD_REQUEST);

        try {
            // Chuyển MultipartFile → byte[]
            byte[] fileBytes = file.getBytes();

            Map<?, ?> result = cloudinary.uploader().upload(fileBytes,
                    ObjectUtils.asMap(
                            "folder",          folder,
                            "resource_type",   "image",
                            // Tự resize về tối đa 800x800 để tiết kiệm dung lượng
                            "transformation",  new Transformation()
                                    .width(800).height(800)
                                    .crop("limit")
                                    .quality("auto")
                                    .fetchFormat("auto")
                    ));

            return result.get("secure_url").toString();

        } catch (IOException e) {
            throw new AppException("Upload ảnh thất bại: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Upload bất kỳ file nào (audio, video, raw) – dùng cho voice messages.
     * folder: "chat/images" hoặc "chat/voices"
     */
    public String uploadFile(MultipartFile file, String folder) {
        if (file.isEmpty())
            throw new AppException("File không được rỗng", HttpStatus.BAD_REQUEST);
        if (file.getSize() > 20 * 1024 * 1024)
            throw new AppException("File không được vượt quá 20MB", HttpStatus.BAD_REQUEST);
        try {
            byte[] fileBytes = file.getBytes();
            String contentType = file.getContentType() != null ? file.getContentType() : "";
            String resourceType = contentType.startsWith("image/") ? "image" : "raw";
            Map<?, ?> result = cloudinary.uploader().upload(fileBytes,
                    ObjectUtils.asMap("folder", folder, "resource_type", resourceType));
            return result.get("secure_url").toString();
        } catch (IOException e) {
            throw new AppException("Upload file thất bại: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Xóa ảnh trên Cloudinary theo publicId
     * publicId lấy từ URL: .../petmatch/pets/abc123  → publicId = "petmatch/pets/abc123"
     */
    public void deleteImage(String imageUrl) {
        try {
            String publicId = extractPublicId(imageUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            // Log lỗi nhưng không throw — ảnh xóa trên DB vẫn thành công
            System.err.println("Không thể xóa ảnh Cloudinary: " + e.getMessage());
        }
    }

    /**
     * Tách publicId từ URL Cloudinary
     * VD: https://res.cloudinary.com/demo/image/upload/v123/petmatch/pets/abc.jpg
     *  →  petmatch/pets/abc
     */
    private String extractPublicId(String url) {
        // Bỏ phần mở rộng file (.jpg, .png,...)
        String withoutExtension = url.substring(0, url.lastIndexOf('.'));
        // Lấy phần sau "/upload/"
        int uploadIndex = withoutExtension.indexOf("/upload/");
        if (uploadIndex == -1) return url;
        String afterUpload = withoutExtension.substring(uploadIndex + 8);
        // Bỏ version nếu có (v1234567/)
        if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
            afterUpload = afterUpload.substring(afterUpload.indexOf('/') + 1);
        }
        return afterUpload;
    }
}