# Roadmap

## MVP v0.1

Các khả năng hiện có trong project:

- Chỉ dựng UI Compose trên màn hình DeX; Activity trên màn hình điện thoại kết thúc trước `setContent`.
- Tạo và chỉnh sửa layout workspace hai hoặc ba vùng.
- Điều chỉnh divider dọc và ngang của layout.
- Chọn ứng dụng đã cài đặt cho từng vùng.
- Lưu workspace và app assignment cục bộ bằng Room.
- Lưu và chỉnh sửa thứ tự launch cùng delay giữa các lần launch.
- Xem, sửa, xóa và khởi chạy workspace đã lưu.
- Khởi chạy tuần tự toàn bộ workspace.
- Mở ứng dụng vào vị trí và kích thước được tính từ zone chuẩn hóa và work area hiện tại.

Danh sách này mô tả chức năng có trong implementation; mức độ xác nhận trên từng thiết bị được ghi riêng trong `TEST_MATRIX.md`.

## Stabilization

- CI unit test/build: implemented, pending verification on GitHub Actions.
- Instrumentation CI: future; hiện chạy local trên thiết bị Android kết nối.
- Release build.
- Dọn log.
- Test cắm/rút DeX.
- Test mở liên tục.
- Test app bị gỡ.
- Test database migration.
- Test phục hồi sau process death.

## Future

- Import/export JSON.
- Backup/restore.
- Workspace yêu thích.
- Automatic launch khi DeX kết nối, chỉ nếu không phát sinh UI trên màn hình điện thoại.
- DLM-100 Launch Engine Refactor.
- Hỗ trợ Android Desktop Mode trong tương lai.

Các mục Future chưa phải tính năng hiện có hoặc cam kết hỗ trợ của MVP v0.1.
