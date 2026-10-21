# Course Frontend

Frontend của hệ thống khóa học, được khởi tạo bằng React, Vite và TypeScript.

## Yêu cầu môi trường

- Node.js 24 trở lên
- pnpm 12 trở lên

## Khởi động dự án

```bash
pnpm install
Copy-Item .env.example .env
pnpm dev
```

Ứng dụng chạy mặc định tại `http://localhost:3001`.

## Các lệnh chính

| Lệnh                | Mục đích                                    |
| ------------------- | ------------------------------------------- |
| `pnpm dev`          | Chạy môi trường phát triển                  |
| `pnpm build`        | Kiểm tra TypeScript và tạo production build |
| `pnpm lint`         | Kiểm tra source bằng Oxlint                 |
| `pnpm lint:fix`     | Tự động sửa các lỗi Oxlint có thể xử lý     |
| `pnpm prettier`     | Kiểm tra định dạng source                   |
| `pnpm prettier:fix` | Tự động định dạng source                    |
| `pnpm preview`      | Xem thử production build                    |

## Biến môi trường

Sao chép `.env.example` thành `.env` và thay đổi giá trị theo môi trường chạy.

```env
VITE_API_URL=http://localhost:8080
VITE_TIMEOUT=10000
```

Chỉ các biến có tiền tố `VITE_` được Vite đưa vào frontend. Không commit token, mật khẩu hoặc khóa bí mật vào file `.env`.

## Cấu trúc dự án

```text
frontend/
├── public/                       # Tài nguyên được phục vụ nguyên trạng
├── src/
│   ├── apis/                    # Hàm gọi API, tách theo module
│   │   └── auth.api.ts
│   ├── assets/                  # Ảnh, font và tài nguyên tĩnh được import
│   ├── components/              # Component tái sử dụng
│   │   ├── common/              # Component dùng chung theo nghiệp vụ
│   │   └── ui/                  # Component nền từ shadcn/ui
│   ├── hooks/                   # TanStack Query và custom hooks
│   │   └── use-auth.ts
│   ├── layouts/                 # Khung giao diện dùng chung
│   │   └── main-layout.tsx
│   ├── lib/                     # Cấu hình và tiện ích lõi
│   │   ├── config.ts            # Vị trí dành cho cấu hình môi trường
│   │   ├── http.ts              # Axios instance dùng chung khi được triển khai
│   │   └── utils.ts             # Hàm tiện ích dùng chung
│   ├── pages/                   # Màn hình gắn với route
│   │   └── login-page.tsx
│   ├── routes/                  # Router và route guards
│   │   └── app-router.tsx
│   ├── stores/                  # Zustand client/shared state
│   │   └── use-auth-store.ts
│   ├── types/                   # Type và interface theo module
│   │   └── auth.type.ts
│   ├── app.tsx                  # Component gốc của ứng dụng
│   ├── app.css                  # Style của component gốc
│   ├── index.css                # Style toàn cục và Tailwind
│   └── main.tsx                 # Điểm khởi động React
├── .env.example                 # Mẫu biến môi trường
├── .oxlintrc.json               # Cấu hình Oxlint
├── .prettierrc                  # Cấu hình Prettier
├── tsconfig.json                # Cấu hình TypeScript và alias `~/`
└── vite.config.ts               # Cấu hình Vite
```

Các file ví dụ trong cây trên mô tả vị trí và quy tắc đặt tên. Chỉ tạo file khi module tương ứng được triển khai.

## Quy tắc đặt tên

- Tất cả file và thư mục tự tạo dùng kebab-case theo mẫu `a-b-c.ext`.
- Component export bên trong file dùng PascalCase.
- Hook dùng tiền tố `use-`, ví dụ `use-auth.ts`.
- Zustand store dùng mẫu `use-<module>-store.ts`.
- API module dùng mẫu `<module>.api.ts`.
- Type module dùng mẫu `<module>.type.ts`.

## Luồng gọi API

```text
Page hoặc component TSX
        ↓
Hook trong src/hooks
        ↓
Module API trong src/apis
        ↓
Axios instance trong src/lib/http.ts
        ↓
Backend
```

- Component và page không gọi Axios, `http.ts` hoặc module API trực tiếp.
- Server state và API cache được quản lý bằng TanStack Query.
- Zustand chỉ lưu client/shared state, không lưu trùng server state.
- Mọi API module dùng chung một Axios instance tại `src/lib/http.ts` khi tầng HTTP được triển khai.

## Import alias

Alias `~/` trỏ tới thư mục `src/`.

Ví dụ: `~/lib/config` trỏ tới `src/lib/config.ts`.

## Kiểm tra trước khi mở pull request

```bash
pnpm build
pnpm lint
pnpm prettier
```

Pull request cần ghi Task ID, cách kiểm thử và ảnh minh chứng nếu có thay đổi giao diện.
