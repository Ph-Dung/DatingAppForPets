package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 🤖 ChatbotResponse - DTO trả về từ chatbot AI
 * 
 * Dùng trong:
 * - Endpoint: POST /api/chatbot/message → interactive chat
 * 
 * Flow tương tác:
 * 
 * 1️⃣ User: "Tôi muốn tìm chó cái"
 *    Response:
 *    {
 *      "reply": "Bạn muốn tìm loại chó nào? (VD: Poodle, Husky, Mèo Anh...)",
 *      "isReadyToSuggest": false,
 *      "suggestions": []
 *    }
 * 
 * 2️⃣ User: "Poodle từ 1-3 tuổi"
 *    Response:
 *    {
 *      "reply": "Đây là những chó Poodle cái từ 1-3 tuổi phù hợp với bạn:",
 *      "isReadyToSuggest": true,
 *      "suggestions": [
 *        {PetProfileResponse 1},
 *        {PetProfileResponse 2},
 *        {PetProfileResponse 3}
 *      ]
 *    }
 * 
 * 🛠️ Cách hoạt động:
 * 1. Client gửi tin nhắn (+ chat history) tới POST /api/chatbot/message
 * 2. Backend gọi OpenAI API (GPT-4o-mini) với system prompt tiếng Việt
 * 3. AI trích xuất thông tin (loài, giống, tuổi, cân nặng, sức khỏe...)
 * 4. Backend parse JSON từ AI:
 *    - Nếu action="SEARCH" → thực hiện search database
 *    - Nếu chỉ là reply → trả về plain text
 * 5. Trả lại ChatbotResponse với suggestions (nếu có)
 * 
 * @author PetMatch Backend Team
 * @version 1.0
 * @since 2024-05-27
 */
@Data
@Builder
public class ChatbotResponse {
    // ═════════════════════════════════════════════════════════════
    // 💬 REPLY
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Tin nhắn trả lời từ chatbot (tiếng Việt)
     * 
     * Ví dụ:
     * - "Bạn muốn tìm loại chó nào? (VD: Poodle, Husky)"
     * - "Đây là những chó phù hợp với bạn:"
     * - "Vui lòng cho biết cân nặng mong muốn (VD: 3-7kg)"
     * 
     * Luôn luôn có giá trị, không bao giờ null
     */
    private String reply;
    
    // ═════════════════════════════════════════════════════════════
    // 🎯 SUGGESTION STATUS
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Có đủ thông tin để suggest pet không?
     * 
     * - true: Đủ thông tin (có loài + 1-2 filter khác)
     *         → suggestions[] sẽ có dữ liệu
     *         → Client có thể hiển thị danh sách
     * 
     * - false: Chưa đủ thông tin
     *          → Chatbot cần hỏi thêm
     *          → suggestions[] sẽ empty
     *          → Client hiển thị reply và chờ user nhập tiếp
     * 
     * Ghi chú: Tên field là "isReadyToSuggest" nhưng
     *         @JsonProperty("isReadyToSuggest") vì JSON convention
     */
    @com.fasterxml.jackson.annotation.JsonProperty("isReadyToSuggest")
    private boolean isReadyToSuggest;
    
    // ═════════════════════════════════════════════════════════════
    // 🐾 SUGGESTIONS
    // ═════════════════════════════════════════════════════════════
    
    /**
     * Danh sách thú cưng gợi ý dựa trên yêu cầu của user
     * 
     * - Nếu isReadyToSuggest = false → [](empty)
     * - Nếu isReadyToSuggest = true → danh sách PetProfileResponse
     * 
     * Các pet trong danh sách được sắp xếp theo:
     * 1. Độ phù hợp (match score từ AI nếu dùng smart suggestions)
     * 2. Khoảng cách (gần nhất trước)
     * 3. Ngày tạo hồ sơ (mới nhất trước)
     * 
     * Mỗi item chứa đầy đủ thông tin: name, photos, age, distance, v.v...
     */
    private List<PetProfileResponse> suggestions;
}
