package com.chat.integration.storage;

import com.chat.config.FileProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// Bản cài đặt FileStorage đang được dự án sử dụng.
// File thật được lưu trong folder trên máy chạy backend, còn DB chỉ lưu metadata và storageKey.
@Component
public class LocalFileStorage implements FileStorage {
    // Đường dẫn tuyệt đối tới folder gốc chứa toàn bộ file upload.
    private final Path root;

    // Lấy FILE_STORAGE_ROOT từ config rồi đổi thành đường dẫn tuyệt đối, chuẩn hóa dấu . và ..
    // Nếu folder chưa tồn tại thì tạo luôn; không tạo được thì backend dừng khởi động.
    public LocalFileStorage(FileProperties fileProperties) {
        root = fileProperties.storageRoot().toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot initialize file storage", exception);
        }
    }

    // Lưu nội dung file vào ổ đĩa theo storageKey.
    // File được ghi vào file tạm trước, chỉ khi ghi xong mới chuyển sang tên thật.
    // Cách này tránh trường hợp file lỗi hoặc chưa ghi xong nhưng đã xuất hiện với tên chính thức.
    @Override
    public void store(String storageKey, InputStream content) {
        // Chuyển storageKey thành đường dẫn an toàn nằm bên trong folder root.
        Path target = resolve(storageKey);
        Path temporary = null;
        try {
            // Tạo file tạm ngay trong root để việc chuyển file sau đó không phải đi qua ổ đĩa khác.
            temporary = Files.createTempFile(root, ".upload-", ".tmp");

            // Đọc toàn bộ dữ liệu upload từ InputStream và ghi vào file tạm.
            Files.copy(content, temporary, StandardCopyOption.REPLACE_EXISTING);
            try {
                // Đổi file tạm thành file thật trong một thao tác để client không đọc phải file dở dang.
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                // Một số hệ điều hành/filesystem không hỗ trợ atomic move nên phải chuyển theo cách thường.
                Files.move(temporary, target);
            }
        } catch (IOException exception) {
            // Lưu thất bại thì dọn file tạm trước khi báo lỗi lên service.
            deleteQuietly(temporary);
            throw new IllegalStateException("Cannot store file", exception);
        }
    }

    // Đọc file vật lý theo storageKey.
    // Chỉ trả Resource nếu đường dẫn là file bình thường và tiến trình backend có quyền đọc.
    @Override
    public Resource load(String storageKey) {
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalStateException("Stored file is unavailable");
        }
        return new FileSystemResource(path);
    }

    // Xóa file vật lý theo storageKey.
    // deleteIfExists giúp thao tác có tính idempotent: file không tồn tại cũng không bị coi là lỗi.
    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot delete stored file", exception);
        }
    }

    // Ghép storageKey với root để tạo đường dẫn tới file thật.
    // Sau khi normalize, đường dẫn bắt buộc vẫn phải bắt đầu bằng root.
    // Kiểm tra này chặn key như ../../secret.txt đọc hoặc ghi file ra ngoài folder upload.
    private Path resolve(String storageKey) {
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }

    // Cố gắng dọn file tạm khi store() thất bại.
    // Hàm không ném thêm exception vì lỗi cần báo ra ngoài vẫn phải là lỗi lưu file ban đầu.
    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Lỗi chính đã được xử lý ở store(), lỗi xóa file tạm có thể bỏ qua.
        }
    }
}
