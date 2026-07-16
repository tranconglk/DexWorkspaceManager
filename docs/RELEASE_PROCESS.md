# Quy trình phát hành DeX Workspace Manager

## Chuẩn bị

- Working tree sạch; unit test, lint và build pass; version đúng.
- Keystore release được sao lưu an toàn ngoài repository.

## Tạo APK đã ký

Dùng Android Studio **Generate Signed Bundle / APK > APK** với đúng keystore và alias. Không lưu keystore, password hoặc đường dẫn keystore trong project.

## Tạo SBOM

Sinh CycloneDX JSON từ dependency graph của `releaseRuntimeClasspath` đã được verification và locking bảo vệ:

```powershell
.\gradlew :app:generateReleaseSbom
```

File tạm nằm trong `app/build/reports/sbom/`. Không chỉnh SBOM thủ công và không dùng SBOM sinh từ commit khác.

## Tạo manifest

```powershell
.\scripts\release\New-ReleaseManifest.ps1 `
    -ApkPath ".\app\release\app-release.apk" `
    -VersionName "0.1.1" `
    -GenerateSbom `
    -RunVerification
```

Nếu bỏ `-VersionName`, script đọc bằng Gradle task `:app:printReleaseInfo`. `-GenerateSbom` chạy task SBOM và xuất bản JSON vào `releases/v{versionName}`; có thể dùng `-SbomPath` để cung cấp file đã sinh từ cùng commit. Script không ký hoặc upload APK. `-AllowDirtyWorkingTree` chỉ dành cho chẩn đoán đã review.

## Kiểm tra certificate

Script tìm build-tools mới nhất có `apksigner.bat` trong `ANDROID_HOME`, `ANDROID_SDK_ROOT`, rồi `%LOCALAPPDATA%\Android\Sdk`; APK unsigned, verify lỗi hoặc fingerprint sai đều làm script dừng.

```powershell
apksigner verify --print-certs .\app-release.apk
```

## Kiểm tra checksum

```powershell
Get-FileHash .\app-release.apk -Algorithm SHA256
```

So sánh checksum APK và SBOM với hai dòng trong `CHECKSUMS.sha256`. Manifest chỉ ghi tên artifact và không tuyên bố reproducible byte-for-byte.

## Kiểm thử thiết bị

Điền thủ công `TEST_REPORT.md` cho Note8 Hades ROM v3 và S23 Ultra. Không đánh dấu `PASS` nếu chưa test. Script không chạy connected test và không ghi đè report đã có.

## Commit tài liệu

Review rồi commit manifest, checksum, SBOM release và test report. Không commit APK, AAB, keystore hoặc password.

## Tạo Git tag

```powershell
git tag -a vX.Y.Z -m "DeX Workspace Manager vX.Y.Z"
```

Script không tự tạo tag.

## Lưu trữ APK ngoài Git

Lưu APK đã ký trong kho artifact an toàn ngoài repository.
