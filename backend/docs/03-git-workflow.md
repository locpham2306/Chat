# Quy trình Git backend

## Nhánh và commit

- `main` dành cho bản tích hợp hoàn thiện.
- BE phát triển trên nhánh `backend`; source vẫn nằm trong thư mục `backend/`.
  Không code tính năng BE trực tiếp trên `main`; khi hoàn thiện mới gộp vào `main`.
- Nếu cần nhánh công việc nhỏ cho BE, tách `feature/<slug>`, `fix/<slug>`,
  `chore/<slug>` từ `backend` rồi gộp lại `backend` trước.
- Không force-push lên nhánh dùng chung.
- Commit theo Conventional Commits: `feat:`, `fix:`, `refactor:`, `test:`,
  `docs:`, `build:`, `chore:`; có thể thêm scope `backend`, `frontend`, `docs`.
- Mỗi commit có một mục tiêu; không trộn reformat toàn bộ vào thay đổi nghiệp vụ.
- Không commit secret, `.env`, IDE config, build output, log hoặc file upload.

## Pull request

Mô tả vấn đề, hành vi sau thay đổi, ảnh hưởng API/DB và kết quả kiểm tra.
Thay đổi hợp đồng API phải cập nhật docs cùng PR để FE và BE cùng đối chiếu.
Khi có ứng dụng, PR phải qua build/test phù hợp và được reviewer duyệt trước khi merge.

## Checklist

- [ ] Source BE nằm trong `backend/`.
- [ ] Module/layer đúng trách nhiệm.
- [ ] DTO, enum và lỗi khớp hợp đồng.
- [ ] Phân quyền và transaction được kiểm tra.
- [ ] Schema thay đổi có migration.
- [ ] Không có secret hoặc dữ liệu runtime trong diff.
- [ ] Docs đã cập nhật nếu API/flow thay đổi.
- [ ] Ghi rõ kiểm tra đã chạy hoặc lý do chưa chạy.
