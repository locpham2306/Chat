package com.chat.integration.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

// Interface quy định các thao tác tối thiểu của nơi lưu file.
// FileService chỉ gọi interface này nên không cần biết file đang nằm trên ổ đĩa, S3 hay MinIO.
public interface FileStorage {

    // Lưu toàn bộ dữ liệu đọc từ content vào storage.
    // storageKey là tên nội bộ do backend tạo, không phải tên file người dùng upload.
    void store(String storageKey, InputStream content);

    // Tìm file theo storageKey và trả về Resource để backend đọc rồi gửi xuống cho FE.
    // Hàm này chỉ đọc file vật lý, việc kiểm tra quyền tải nằm ở FileService.
    Resource load(String storageKey);

    // Xóa file vật lý theo storageKey, thường dùng để dọn file khi lưu metadata DB thất bại.
    void delete(String storageKey);
}
