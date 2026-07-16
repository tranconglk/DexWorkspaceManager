# Software Bill of Materials

## SBOM là gì

SBOM là danh sách có cấu trúc của các component phần mềm và quan hệ dependency trong một bản build. Project tạo SBOM cho runtime dependency của release variant.

## Vì sao project dùng CycloneDX

CycloneDX là chuẩn mở dành cho software supply chain. Plugin Gradle chính thức của dự án CycloneDX đọc dependency graph trực tiếp từ Gradle, tạo PURL và quan hệ direct/transitive mà không gửi SBOM lên dịch vụ ngoài.

Project dùng plugin `org.cyclonedx.bom` version cố định `3.2.4` và chỉ sinh JSON theo CycloneDX 1.6.

## SBOM chứa gì

- Metadata ứng dụng `DexWorkspaceManager`, version và application ID/group.
- Direct và transitive dependency từ `releaseRuntimeClasspath`.
- Tên, group, version, hash và Package URL khi plugin xác định được.
- Quan hệ dependency và metadata của công cụ sinh SBOM.

SBOM không chứa keystore, password, username máy, local SDK path hoặc absolute Windows path.

## SBOM không bảo đảm điều gì

SBOM là danh sách component, không phải bằng chứng mật mã rằng APK chắc chắn được build từ đúng các component đó. Nó không phải vulnerability scan, VEX, provenance hay artifact attestation. Chứng thực provenance/SBOM là công việc tương lai.

## Cách tạo SBOM

```powershell
.\gradlew :app:generateReleaseSbom
```

Output tạm:

```text
app/build/reports/sbom/dex-workspace-manager-v{versionName}.cdx.json
```

Quy trình release xuất bản file cuối vào `releases/v{versionName}/`:

```powershell
.\scripts\release\New-ReleaseManifest.ps1 `
    -ApkPath ".\app\release\app-release.apk" `
    -GenerateSbom `
    -RunVerification
```

Có thể truyền `-SbomPath` thay `-GenerateSbom`, nhưng file phải được sinh từ đúng dependency state và Git commit của release.

## Cách xác minh checksum

```powershell
Get-FileHash .\dex-workspace-manager-v0.1.1.cdx.json -Algorithm SHA256
```

So sánh với dòng SBOM trong `CHECKSUMS.sha256`. Script cũng kiểm tra `bomFormat`, `specVersion`, component/dependency và `metadata.component` trước khi xuất bản.

## Cách xử lý khi dependency thay đổi

1. Cập nhật dependency bằng version cố định.
2. Cập nhật và review lock state nếu graph runtime thay đổi.
3. Cập nhật và review verification metadata cho artifact mới.
4. Chạy lại strict build và `generateReleaseSbom`.
5. Không chỉnh JSON thủ công; thay SBOM cũ bằng output mới đã review.

## Quan hệ với verification metadata và lock files

Dependency locking cố định version đã resolve. Dependency verification kiểm tra nội dung artifact/POM bằng checksum. SBOM mô tả graph đã resolve nhưng không thay thế hai cơ chế này. Task SBOM phải chạy với strict verification; CI bình thường không tự ghi metadata hoặc lock state.

## Chính sách lưu trữ SBOM

Không commit SBOM tạm dưới `build/`. Có thể commit SBOM nằm trong hồ sơ `releases/v{versionName}/` cùng manifest, checksum và device test report. APK và keystore vẫn được lưu ngoài Git.
