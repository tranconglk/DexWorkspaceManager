# Gradle Dependency Locking

## Mục tiêu

Dependency locking cố định version thực tế mà Gradle resolve cho các classpath dùng bởi app và CI.
Version catalog mô tả dependency trực tiếp; BOM, constraints và transitives vẫn tham gia chọn graph.
Lock state ghi lại kết quả cuối cùng. Dependency verification tiếp tục kiểm tra nội dung artifact
bằng SHA-256 và không bị thay thế bởi locking.

## Lock files

Lock state của module app nằm tại `app/gradle.lockfile`. Project khóa có chọn lọc các compile và
runtime classpath của debug, release, unit test, Android test, cùng KSP processor và lint tool
classpath. Plugin versions được pin trong version catalog và `settings.gradle.kts`; plugin
resolution không được bật dependency locking trong baseline này.

## Strict build behavior

Các task Gradle bình thường tự áp dụng lock state đã commit đồng thời với strict dependency
verification. Dependency thiếu trong lock state hoặc version resolve không khớp sẽ làm build fail.
CI không cần write flag và không được tự cập nhật lock state.

Locking cải thiện khả năng tái lập dependency graph khi dùng cùng commit, Gradle Wrapper, JDK major,
lock state và verification metadata. Nó không đảm bảo APK giống nhau byte-for-byte vì APK có thể
chứa timestamp hoặc build metadata khác.

## Cách thêm dependency

1. Khai báo dependency bằng version cố định và review repository nguồn.
2. Chạy các task thực tế với `--write-locks`.
3. Nếu có artifact mới, chạy riêng các task với `--write-verification-metadata sha256`.
4. Review build script, lock diff và verification metadata diff.
5. Chạy lại unit test, lint, debug và release không có write flag.

## Cách nâng dependency

Không sửa lock file thủ công. Sau khi thay version catalog hoặc build script đã được review, chạy:

```powershell
.\gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease --write-locks
.\gradlew --write-verification-metadata sha256 testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Sau đó review hai diff và chạy strict build lại. Hai write operation được tách riêng để dễ phát
hiện thay đổi graph và artifact.

## Quy trình xử lý Dependabot PR

1. Checkout branch Dependabot và review dependency/version catalog diff.
2. Chạy các task CI với `--write-locks`.
3. Chạy lại các task với `--write-verification-metadata sha256` nếu artifact thay đổi.
4. Review lock file và verification metadata diff.
5. Chạy strict build không có write flag.
6. Commit lock state và metadata cùng dependency update.

## Cách xử lý lock mismatch

Kiểm tra dependency và configuration Gradle báo lỗi. Nếu thay đổi dependency là dự kiến, chạy đúng
task với `--write-locks`, review diff rồi chạy strict lại. Nếu không có thay đổi dự kiến, dừng lại
và điều tra graph, repository và môi trường build; không xóa hoặc sửa tay lock entry.

## Điều không được làm

- Không xóa lock file để làm CI xanh.
- Không dùng `--write-locks` trong CI.
- Không sửa lock file thủ công.
- Không dùng version động hoặc snapshot.
- Không commit lock state chưa review.
- Không cập nhật lock state mà bỏ qua verification metadata khi artifact thay đổi.
