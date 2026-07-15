# Architecture Freeze — MVP v0.1

Task: DLM-000

Tài liệu này ghi nhận kiến trúc và hành vi đã được đóng băng cho MVP v0.1. Đây là mô tả của implementation hiện tại, không phải đề xuất refactor.

## Product Scope

DeX Workspace Manager cho phép người dùng tạo và chỉnh sửa một workspace gồm các vùng cửa sổ, gán ứng dụng cho từng vùng, lưu cấu hình bằng Room, cấu hình thứ tự và độ trễ khởi chạy, rồi mở tuần tự toàn bộ workspace trên màn hình DeX với vị trí và kích thước đã xác định.

Phạm vi MVP v0.1 chỉ bao gồm UI chạy trên màn hình DeX, quản lý workspace cục bộ, chọn ứng dụng đã cài đặt và khởi chạy các ứng dụng vào bounds tương ứng. MVP không tuyên bố hỗ trợ mọi thiết bị Samsung DeX hay Android Desktop Mode nói chung.

## Supported Devices

Đã xác nhận thực tế:

- Samsung Note8 + Hades ROM v3.
- Samsung S23 Ultra.

Các thiết bị khác chưa được xác nhận. Không suy diễn phiên bản Android hoặc One UI từ tên thiết bị.

## DeX-only Policy

- `MainActivity` chỉ được dựng UI trên external display.
- Activity chạy trên `Display.DEFAULT_DISPLAY` phải gọi `finish()` và thoát khỏi `onCreate()` trước `setContent`.
- Luồng chính không được gọi API hệ thống có thể tạo UI trên màn hình điện thoại.
- Dialog nội bộ phải được hiển thị bên trong Compose Activity đang chạy trên DeX.
- Không đưa lại `requestPinShortcut` vào luồng DeX-only; trên S23 Ultra, hộp thoại của API này đã xuất hiện trên màn hình điện thoại.

## Architecture Layers

- **UI Screen:** các composable thuần trình bày dữ liệu và phát callback từ thao tác người dùng; không sở hữu nghiệp vụ hay truy cập persistence/platform trực tiếp.
- **Route:** composable tích hợp Screen với ViewModel, navigation và tài nguyên gắn với lifecycle UI như Activity, snackbar, coroutine scope và `Job` khởi chạy.
- **ViewModel:** sở hữu `UiState`, xử lý sự kiện và điều phối nghiệp vụ/persistence qua interface; không nhận Activity hoặc Context.
- **Domain:** chứa `Workspace`, `WorkspaceAppAssignment` và contract repository dùng bởi consumer nghiệp vụ.
- **Repository:** là ranh giới giữa domain và data; implementation thực hiện transaction, gọi DAO và mapper, không biết Compose.
- **Room:** gồm database, DAO, entity, relation và migration; chịu trách nhiệm lưu trữ cục bộ.
- **Platform:** bao bọc thao tác Android như đọc display, liệt kê ứng dụng, tính launch bounds và tạo/khởi chạy Intent.
- **Coordinator:** `WorkspaceLaunchCoordinator` sắp xếp assignment, lấy zone, tính bounds, launch tuần tự, áp dụng delay và tổng hợp kết quả.

## Launch Flow

Luồng hiện tại:

```text
Workspace
→ LayoutTemplates
→ LayoutZone
→ LayoutBoundsCalculator
→ WorkspaceLaunchCoordinator
→ ForegroundAppLauncher
→ Android Intent
→ DeX window
```

`Workspace` xác định template, divider ratio, assignment, thứ tự và delay. `LayoutTemplates` tạo các `LayoutZone` chuẩn hóa. `LayoutBoundsCalculator` chuyển zone sang pixel theo work area hiện tại. Coordinator gọi `ForegroundAppLauncher`, implementation Android tạo Intent và Activity đang ở foreground trên DeX gọi `startActivity()` để tạo cửa sổ.

## Frozen Launch Behavior

Các hành vi sau đã hoạt động trên thiết bị được hỗ trợ và không được tự ý thay đổi nếu không có task, lý do và kiểm thử lại rõ ràng:

- Intent có `FLAG_ACTIVITY_NEW_TASK`.
- Intent có `FLAG_ACTIVITY_MULTIPLE_TASK`.
- Bounds được truyền bằng `ActivityOptions.setLaunchBounds()`.
- Nguồn launch là Activity hiện tại đang chạy trên màn hình DeX và gọi `Activity.startActivity()`.
- Các ứng dụng được launch tuần tự, không parallel.
- Thứ tự được xác định bởi `launchOrder` (sau đó ổn định theo zone khi cần).
- Khoảng nghỉ giữa hai lần launch dùng `launchDelayMs`.
- Không dùng `setLaunchDisplayId` trong luồng chính.

## State Ownership

- Screen là stateless: nhận state và callback qua tham số.
- ViewModel giữ và cập nhật `UiState`.
- Route giữ Activity và coroutine `Job` phụ thuộc lifecycle UI.
- ViewModel không nhận Activity hoặc Context.
- Repository không biết Compose.

State tạm thời thuần UI có thể được Route/Compose giữ bằng API `remember` phù hợp; điều này không chuyển quyền sở hữu business state khỏi ViewModel.

## Persistence

- `Workspace` là model domain của một bố cục đã lưu, gồm tên, template, divider ratio, thời điểm cập nhật, assignments và `launchDelayMs`.
- `WorkspaceAppAssignment` là model domain gán một package/activity vào zone với `launchOrder`.
- `WorkspaceEntity` và `WorkspaceAppAssignmentEntity` là Room entities; assignment tham chiếu workspace và được cascade khi workspace bị xóa.
- `WorkspaceWithAssignments` dùng Room `@Relation` để đọc workspace cùng assignments.
- Mapper chuyển đổi hai chiều giữa entity/relation và domain; Room entity không được expose tới UI hoặc domain consumer.
- Repository lưu workspace và thay toàn bộ assignments trong một Room transaction.
- Database hiện tại là version 3, có `MIGRATION_1_2` và `MIGRATION_2_3`. Mọi thay đổi schema tiếp theo phải có migration tương ứng và được kiểm thử; việc migration tồn tại trong code không đồng nghĩa đã được xác nhận trên thiết bị.

## Display and Bounds

- Không hard-code `displayId`.
- Không hard-code độ phân giải `1920x1200` hay bất kỳ độ phân giải màn hình cụ thể nào.
- Dùng `Activity.display` và work area của Activity hiện tại trên external display.
- `LayoutZone` lưu tọa độ chuẩn hóa trong khoảng `0f..1f`.
- Chỉ chuyển tọa độ chuẩn hóa thành pixel tại thời điểm launch.
- Chỉ `LayoutBoundsCalculator` được chịu trách nhiệm tính launch bounds.

## Coroutines

- Không dùng `GlobalScope`.
- Không dùng `Thread.sleep`.
- Dùng `viewModelScope`, `lifecycleScope` hoặc `rememberCoroutineScope` theo đúng chủ sở hữu lifecycle.
- Workspace launch phải tuần tự, không parallel.
- Phải tôn trọng và truyền tiếp `CancellationException`; không được biến cancellation thành lỗi launch thông thường.

## Logging

Các tag hợp lệ:

- `DexOnlyMode`: quyết định chấp nhận/từ chối Activity theo display và vi phạm DeX-only.
- `WorkspaceLaunch`: tiến trình/kết quả điều phối một workspace.
- `DexLaunch`: chi tiết cần thiết của thao tác launch Android lên cửa sổ DeX.

Không log mỗi lần recomposition. Không log đầy đủ Intent extras trong release.

## Forbidden Changes

Không được thêm hoặc đưa vào luồng chính các mục sau nếu chưa có task và lý do rõ ràng:

- root;
- ADB;
- Accessibility;
- reflection;
- hidden API;
- Samsung private API;
- `QUERY_ALL_PACKAGES`;
- `requestPinShortcut` trong luồng DeX;
- `setLaunchDisplayId` trong luồng chính;
- Activity riêng trên màn hình điện thoại;
- implementation khởi chạy workspace trùng lặp.

## Refactor Backlog

### DLM-100 Launch Engine Refactor

Trạng thái: **Deferred until after MVP v0.1 stabilization.**

Kiến trúc dự kiến:

```text
Workspace
→ WorkspaceLaunchPlanner
→ LaunchPlan
→ BoundsResolver
→ ResolvedLaunchPlan
→ WorkspaceLaunchExecutor
→ LaunchEngine
→ AndroidLaunchEngine
```

Đây chỉ là hướng kiến trúc tương lai, chưa phải kiến trúc hiện tại và không được mô tả như implementation đang tồn tại.
