# Test Matrix — MVP v0.1

Task: DLM-000

`PASS` chỉ thể hiện kết quả đã được xác nhận thực tế trong phạm vi bằng chứng hiện có. `NOT TESTED` không có nghĩa tính năng không tồn tại hoặc không hoạt động; trạng thái này chỉ cho biết chưa có bằng chứng kiểm thử riêng đủ để khẳng định.

| Tính năng | Note8 Hades v3 | S23 Ultra | Ghi chú |
| --------- | -------------- | --------- | ------- |
| DeX-only Activity | PASS | PASS | UI đã được xác nhận chỉ hoạt động trên màn hình DeX. |
| Tạo layout 2 vùng | NOT TESTED | NOT TESTED | Có implementation nhưng chưa có bằng chứng test riêng theo số vùng. |
| Tạo layout 3 vùng | NOT TESTED | NOT TESTED | Có implementation nhưng chưa có bằng chứng test riêng theo số vùng. |
| Kéo divider dọc | NOT TESTED | NOT TESTED | Chưa có bằng chứng test riêng. |
| Kéo divider ngang | NOT TESTED | NOT TESTED | Chưa có bằng chứng test riêng. |
| Chọn ứng dụng | PASS | PASS | Khả năng chọn ứng dụng cho từng vùng đã được xác nhận. |
| Lưu workspace | PASS | PASS | Workspace lưu bằng Room đã được xác nhận. |
| Khôi phục workspace | NOT TESTED | NOT TESTED | Chưa có bằng chứng test khôi phục riêng. |
| Lưu app assignment | PASS | PASS | Gán ứng dụng theo vùng và lưu bằng Room đã được xác nhận. |
| Lưu launch order | PASS | PASS | Lưu thứ tự launch đã được xác nhận. |
| Lưu launch delay | PASS | PASS | Lưu delay đã được xác nhận. |
| Mở từng app đúng vùng | PASS | PASS | Mở ứng dụng đúng vị trí và kích thước đã được xác nhận. |
| Khởi chạy toàn workspace | PASS | PASS | Khởi chạy toàn bộ workspace đã được xác nhận. |
| Khởi chạy từ Saved Layouts | NOT TESTED | NOT TESTED | Chưa có bằng chứng test riêng cho entry point này. |
| Cắm/rút DeX | NOT TESTED | NOT TESTED | Cần kiểm thử trong giai đoạn Stabilization. |
| Khởi động lại ứng dụng | NOT TESTED | NOT TESTED | Chưa có bằng chứng test riêng. |
| Migration Room | NOT TESTED | NOT TESTED | Migration có trong code nhưng chưa có bằng chứng chạy test trên thiết bị. |
| Ứng dụng không xuất UI trên điện thoại | PASS | PASS | Chính sách DeX-only đã được xác nhận trên hai thiết bị. |
