package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 📸 PetPhotoDto - DTO trả về thông tin ảnh thú cưng
 * 
 * Dùng trong:
 * - PetProfileResponse.photos[] để liệt kê chi tiết ảnh
 * - Response upload ảnh
 * 
 * Cấu trúc:
 * {
 *   "id": 123,
 *   "url": "https://cloudinary.com/..."
 * }
 * 
 * @author PetMatch Backend Team
 * @version 1.0
 * @since 2024-05-27
 */
@Data
@Builder
public class PetPhotoDto {
    /**
     * ID ảnh duy nhất trong database
     */
    private Long id;
    
    /**
     * URL ảnh trên Cloudinary
     * Định dạng: https://res.cloudinary.com/...
     */
    private String url;
}
