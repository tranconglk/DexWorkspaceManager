# Android CI

Workflow `Android CI` runs for pushes and pull requests targeting `master`, and can also be
started manually from the GitHub Actions page.

The `unit-tests` job runs `testDebugUnitTest`. When it finishes, including on failure, its HTML
and XML reports are uploaded as `unit-test-reports`. The dependent `build` job runs only after
unit tests pass and builds both debug and release variants.

Build artifacts are retained for seven days:

- `debug-apk`: debug APK for development testing.
- `release-unsigned-apk`: unsigned release APK. It cannot update the manually signed release
  installed on a device and must not be distributed as a production release.

Instrumentation tests are intentionally not run in this basic CI workflow. Run them on a local
connected Android device when required.

Equivalent local commands on Windows are:

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
.\gradlew assembleRelease
.\gradlew connectedDebugAndroidTest # Requires a connected device.
```
