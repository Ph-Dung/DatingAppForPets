package com.petmatch.backend.dto.request;

import lombok.Data;

/**
 * 🗺️ UpdateLocationRequest - DTO request cập nhật vị trí GPS thú cưng
 * 
 * Dùng trong:
 * - Endpoint: PUT /api/pets/location (cập nhật GPS hồ sơ)
 * - Mobile: User cho phép truy cập vị trí → gửi tọa độ
 * 
 * Logic GPS:
 * - Lưu latitude, longitude vào PetProfile.locationLatitude/Longitude
 * - Dùng để tính khoảng cách: Haversine formula (km)
 * - Suggestions filter: chỉ show pets trong radius (mặc định: 50km)
 * - User có thể ẩn vị trí: PetProfile.isHidden = true
 * 
 * Ví dụ:
 * PUT /api/pets/location
 * { "latitude": 10.7769, "longitude": 106.6869 } // TP.HCM
 * 
 * @author PetMatch Backend Team
 * @version 1.0
 */
@Data
public class UpdateLocationRequest {
    /** Vĩ độ (latitude): từ -90 đến 90 */
    private Double latitude;
    
    /** Kinh độ (longitude): từ -180 đến 180 */
    private Double longitude;
}
