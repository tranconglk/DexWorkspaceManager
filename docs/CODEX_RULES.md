# Codex Rules

## Release rules

- Không phát hành APK nếu certificate fingerprint không khớp certificate release đã đăng ký.
- Không tự đánh dấu device test là `PASS` khi chưa có kết quả kiểm thử thực tế.
- Không commit APK, keystore hoặc password.
- Mỗi release phải có `RELEASE_MANIFEST.md` và `CHECKSUMS.sha256` đã được review.

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
17. Mọi task phải giữ workflow Android CI xanh.
18. Trước khi commit, chạy unit test và build debug/release tương ứng ở local.
19. Không commit GitHub workflow chứa secrets, keystore hoặc mật khẩu.
20. Mọi thay đổi phải giữ `lintDebug` không có error; không suppress rộng hoặc dùng baseline để che lỗi nghiêm trọng.
21. Không bỏ qua Gradle Wrapper checksum, Dependency Review hoặc supply-chain checks khi sửa build và CI.
22. Mọi thay đổi dependency phải cập nhật và review `gradle/verification-metadata.xml`.
23. Không dùng `--dependency-verification off` hoặc xóa verification metadata để làm CI xanh.
24. Không tự động sinh verification metadata trong GitHub Actions.
25. Không nâng dependency khi metadata mới chưa được review.
26. Mọi thay đổi dependency phải cập nhật và review cả lock state lẫn verification metadata.
27. Không chạy `--write-locks` trong GitHub Actions hoặc xóa lock file để né lỗi.
28. Không thêm version động; lock file là artifact bắt buộc phải được review.
