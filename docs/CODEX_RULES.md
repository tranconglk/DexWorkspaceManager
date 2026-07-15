# Codex Rules

Các quy tắc này áp dụng cho mọi task Codex tiếp theo trong project DeX Workspace Manager:

1. Đọc project trước khi sửa.
2. Trước khi thay đổi, liệt kê các file sẽ tạo, sửa hoặc xóa.
3. Không tạo project mới.
4. Không thay đổi phần đã đóng băng nếu task không yêu cầu rõ ràng.
5. Không thêm dependency khi chưa thông báo.
6. Không nâng Kotlin, Gradle, AGP, `compileSdk` hoặc `minSdk`.
7. Không tự commit Git.
8. Luôn hiển thị diff sau khi thay đổi.
9. Luôn chạy `./gradlew assembleDebug` (PowerShell/Windows: `.\gradlew assembleDebug`).
10. Một task tương ứng một commit; Codex chuẩn bị thay đổi nhưng không tự tạo commit.
11. Không tạo abstraction khi chưa có nhu cầu thực.
12. Không copy hoặc tạo implementation trùng logic của `WorkspaceLaunchCoordinator`.
13. Không làm UI riêng cho Note8 và S23 Ultra.
14. UI phải thích ứng theo kích thước cửa sổ, không theo `Build.MODEL`.
15. Nếu thay đổi Room schema, phải có migration và kiểm thử migration.
16. Nếu thay đổi launch engine, phải kiểm thử lại trên cả Samsung Note8 + Hades ROM v3 và Samsung S23 Ultra.
