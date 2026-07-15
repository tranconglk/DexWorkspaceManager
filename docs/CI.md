# Android CI

Workflow `Android CI` runs for pushes and pull requests targeting `master`, and can also be
started manually from the GitHub Actions page.

The `unit-tests` job runs `testDebugUnitTest`. When it finishes, including on failure, its HTML
and XML reports are uploaded as `unit-test-reports`. The `lint` job runs `lintDebug` and always
uploads the generated reports as `android-lint-reports`. The dependent `build` job runs only
after both unit tests and lint pass, then builds the debug and release variants.

Build artifacts are retained for seven days:

- `debug-apk`: debug APK for development testing.
- `release-unsigned-apk`: unsigned release APK. It cannot update the manually signed release
  installed on a device and must not be distributed as a production release.

Instrumentation tests are intentionally not run in this basic CI workflow. Run them on a local
connected Android device when required.

## Supply-chain checks

- The Gradle distribution is pinned by SHA-256 in `gradle-wrapper.properties`.
- `gradle/actions/setup-gradle@v6` validates Gradle Wrapper JARs in every Android CI job.
- The `Dependency Review` workflow runs only on pull requests and rejects newly introduced
  dependencies with high or critical known vulnerabilities.
- Dependabot checks Gradle and GitHub Actions dependencies monthly, with at most five open pull
  requests per ecosystem. Updates require normal review; no automatic merge or signing secret is
  configured.
- Dependency verification metadata and dependency locking are deferred to DLM-041 so the full
  plugin and transitive dependency graph can be reviewed before enforcement is enabled.

Equivalent local commands on Windows are:

```powershell
.\gradlew testDebugUnitTest
.\gradlew lintDebug
.\gradlew assembleDebug
.\gradlew assembleRelease
.\gradlew connectedDebugAndroidTest # Requires a connected device.
```
