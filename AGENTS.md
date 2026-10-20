# Hướng dẫn làm việc trong repository

- Đọc `README.md` trước khi sửa code; khi làm BE, đọc thêm `backend/AGENTS.md` và `backend/docs/`.
- Toàn bộ backend nằm trong `backend/`; frontend nằm trong `frontend/`.
- Phát triển BE trên nhánh `backend`, không code tính năng BE trực tiếp trên `main`.
  `main` dành cho bản tích hợp hoàn thiện; chỉ gộp BE khi đã sẵn sàng.
- Backend đang dựng base từng bước để review. Không tự mở rộng sang bước tiếp theo, commit hoặc push khi chưa được yêu cầu.
- Giữ hợp đồng trong `backend/docs/04-hop-dong-api.md`; khi đổi API phải cập nhật tài liệu và thông báo ảnh hưởng đến FE.
- Không sao chép code, cấu hình nội bộ hoặc secret từ dự án tham khảo.
- Không commit `.env`, credential, token, log, build output hoặc dữ liệu upload.
- Chạy kiểm tra phù hợp với thay đổi. Chưa có build/test thì báo rõ, không ghi là đã test.
- Không force-push lên nhánh dùng chung.
