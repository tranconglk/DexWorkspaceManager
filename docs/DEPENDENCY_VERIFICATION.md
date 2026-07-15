# Gradle Dependency Verification

## Mục tiêu

Dependency verification dùng SHA-256 để phát hiện plugin hoặc dependency đã tải về có nội dung
khác với artifact đã được review. Việc tạo metadata lần đầu xác nhận artifact đang được resolve
trong môi trường hiện tại; mức tin cậy ban đầu vẫn phụ thuộc repository và máy tạo metadata.

## File metadata

Metadata nằm tại `gradle/verification-metadata.xml`. File bật kiểm tra metadata, tắt signature
verification và chỉ chứa SHA-256. Dự án hiện chỉ resolve từ Google Maven, Maven Central và Gradle
Plugin Portal.

## Strict mode

Gradle tự bật dependency verification khi metadata tồn tại. Strict mode mặc định được dùng ở local
và CI; không cần thêm flag vào lệnh build. Artifact thiếu checksum hoặc có checksum không khớp sẽ
làm task thất bại.

## Cách thêm dependency mới

1. Thêm dependency với version cố định và review repository cung cấp artifact.
2. Chạy các task cần thiết với `--write-verification-metadata sha256`, tối thiểu:

   ```powershell
   .\gradlew --write-verification-metadata sha256 testDebugUnitTest lintDebug assembleDebug assembleRelease
   ```

3. Review metadata diff, component, artifact và checksum mới.
4. Chạy lại các task không có `--write-verification-metadata` để xác nhận strict mode.
5. Commit metadata cùng thay đổi dependency sau khi review.

## Cách xử lý Dependabot PR

1. Review dependency update và checkout branch của Dependabot.
2. Chạy các task thực tế với `--write-verification-metadata sha256`.
3. Review metadata diff; không chấp nhận checksum chỉ vì Gradle tự sinh.
4. Commit metadata cùng dependency update.
5. Để CI chạy strict mode. CI không được tự cập nhật metadata.

## Cách xử lý verification failure

Xác định component và artifact trong verification report. Nếu artifact là thay đổi dependency dự
kiến, chạy lại đúng task với `--write-verification-metadata sha256`, review entry mới rồi chạy strict
lại. Nếu không có thay đổi dependency dự kiến, dừng lại và điều tra repository, cache và nguồn
artifact; không chấp nhận checksum mới một cách tự động.

## Những gì không được làm

- Không dùng verification mode `off`.
- Không commit cấu hình `lenient`.
- Không trust wildcard rộng cho group hoặc repository.
- Không tự động sinh metadata trong CI.
- Không sửa hoặc chấp nhận checksum khi chưa hiểu dependency thay đổi.
- Không xóa metadata để làm build xanh.
- Không bật PGP signature verification hoặc thêm ignored keys trong baseline SHA-256 này.
