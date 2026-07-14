package com.trancong.dexworkspacemanager.platform.applauncher

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import com.trancong.dexworkspacemanager.domain.model.Workspace
import com.trancong.dexworkspacemanager.domain.model.WorkspaceAppAssignment
import com.trancong.dexworkspacemanager.feature.layouteditor.LayoutTemplates
import com.trancong.dexworkspacemanager.feature.layouteditor.WorkspaceLaunchFailure
import com.trancong.dexworkspacemanager.feature.layouteditor.WorkspaceLaunchResult
import com.trancong.dexworkspacemanager.platform.dex.DexWorkArea
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.math.roundToInt

class WorkspaceLaunchCoordinator(
    private val foregroundAppLauncher: ForegroundAppLauncher
) {
    suspend fun launch(
        activity: Activity,
        workspace: Workspace,
        workArea: DexWorkArea,
        onProgress: (
            completedApps: Int,
            totalApps: Int,
            currentAssignment: WorkspaceAppAssignment?
        ) -> Unit
    ): WorkspaceLaunchResult {
        if (!activity.isRunningOnExternalDisplay()) {
            return WorkspaceLaunchResult.NotRunningOnDex
        }

        val zonesById = LayoutTemplates.zonesFor(
            workspace.template,
            workspace.leftRatio,
            workspace.topRatio
        ).associateBy { it.id }
        val assignments = workspace.appAssignments
            .filter { it.zoneId in zonesById }
            .sortedWith(
                compareBy<WorkspaceAppAssignment> { it.launchOrder }
                    .thenBy { it.zoneId }
            )
        if (assignments.isEmpty()) return WorkspaceLaunchResult.NoAssignedApps

        var launchedCount = 0
        val failures = mutableListOf<WorkspaceLaunchFailure>()
        val marginPx = (BOUNDS_MARGIN_DP * activity.resources.displayMetrics.density).roundToInt()

        return try {
            assignments.forEachIndexed { index, assignment ->
                currentCoroutineContext().ensureActive()
                onProgress(index, assignments.size, assignment)
                val zone = zonesById.getValue(assignment.zoneId)
                val result = try {
                    val bounds = LayoutBoundsCalculator.calculate(
                        zone = zone,
                        availableWidth = workArea.width,
                        availableHeight = workArea.usableHeight,
                        originX = workArea.originX,
                        originY = workArea.originY,
                        marginPx = marginPx
                    )
                    foregroundAppLauncher.launchInZone(
                        activity = activity,
                        packageName = assignment.packageName,
                        activityName = assignment.activityName,
                        bounds = bounds
                    )
                } catch (_: IllegalArgumentException) {
                    AppLaunchResult.InvalidBounds
                }

                if (result == AppLaunchResult.Success) {
                    launchedCount += 1
                } else {
                    failures += WorkspaceLaunchFailure(
                        zoneId = assignment.zoneId,
                        appLabel = assignment.appLabel,
                        reason = result.failureReason()
                    )
                }
                onProgress(index + 1, assignments.size, assignment)
                if (index < assignments.lastIndex) delay(workspace.launchDelayMs)
            }

            if (failures.isEmpty()) {
                WorkspaceLaunchResult.Success(launchedCount)
            } else {
                WorkspaceLaunchResult.PartialSuccess(
                    launchedCount = launchedCount,
                    failedCount = failures.size,
                    failures = failures
                )
            }
        } catch (_: CancellationException) {
            WorkspaceLaunchResult.Cancelled(launchedCount)
        }
    }
}

@Suppress("DEPRECATION")
fun Activity.currentExternalDisplayWorkArea(): DexWorkArea? {
    val currentDisplay = windowManager.defaultDisplay
    if (currentDisplay.displayId == Display.DEFAULT_DISPLAY) return null
    val metrics = DisplayMetrics()
    currentDisplay.getRealMetrics(metrics)
    if (metrics.widthPixels <= 0 || metrics.heightPixels <= 0) return null
    return DexWorkArea(width = metrics.widthPixels, height = metrics.heightPixels)
}

fun Activity.isRunningOnExternalDisplay(): Boolean {
    val displayId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display?.displayId
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.displayId
    }
    return displayId != null && displayId != Display.DEFAULT_DISPLAY
}

private fun AppLaunchResult.failureReason(): String = when (this) {
    AppLaunchResult.Success -> "Không có lỗi"
    AppLaunchResult.AppNotFound -> "Ứng dụng không còn được cài đặt"
    AppLaunchResult.ActivityNotFound -> "Không tìm thấy màn hình khởi chạy"
    AppLaunchResult.SecurityError -> "Không có quyền mở ứng dụng"
    AppLaunchResult.DisplayNotAvailable -> "Màn hình DeX không còn khả dụng"
    AppLaunchResult.LaunchNotAllowedOnDisplay -> "Hệ thống từ chối mở trên màn hình này"
    AppLaunchResult.MultiDisplayNotSupported -> "Thiết bị không hỗ trợ màn hình phụ"
    AppLaunchResult.BoundsNotSupported -> "ROM không hỗ trợ launch bounds"
    AppLaunchResult.InvalidBounds -> "Kích thước vùng không hợp lệ"
    is AppLaunchResult.UnknownError -> "Không thể mở ứng dụng"
}

private const val BOUNDS_MARGIN_DP = 8f
