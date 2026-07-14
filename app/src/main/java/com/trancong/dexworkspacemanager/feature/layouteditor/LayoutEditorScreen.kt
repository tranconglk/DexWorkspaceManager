package com.trancong.dexworkspacemanager.feature.layouteditor

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trancong.dexworkspacemanager.DexWorkspaceManagerApplication
import com.trancong.dexworkspacemanager.ui.theme.DexWorkspaceManagerTheme

@Composable
fun LayoutEditorRoute(
    onBackClick: () -> Unit
) {
    val application = LocalContext.current.applicationContext as DexWorkspaceManagerApplication
    val viewModelFactory = remember(application) {
        LayoutEditorViewModelFactory(application.container.workspaceRepository)
    }
    val viewModel: LayoutEditorViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    LayoutEditorScreen(
        selectedTemplate = uiState.selectedTemplate,
        onTemplateSelected = viewModel::selectTemplate,
        onResetLayout = viewModel::resetLayout,
        leftRatio = uiState.leftRatio,
        onLeftRatioChange = viewModel::updateLeftRatio,
        topRatio = uiState.topRatio,
        onTopRatioChange = viewModel::updateTopRatio,
        onBackClick = onBackClick,
        workspaceName = uiState.workspaceName,
        onWorkspaceNameChange = viewModel::updateWorkspaceName,
        isNameDialogVisible = uiState.isNameDialogVisible,
        isSaving = uiState.isSaving,
        saveMessage = uiState.saveMessage,
        saveError = uiState.saveError,
        onSaveClick = viewModel::showSaveDialog,
        onConfirmSave = viewModel::saveWorkspace,
        onDismissSaveDialog = viewModel::hideSaveDialog,
        onSaveMessageShown = viewModel::consumeSaveMessage,
        onSaveErrorShown = viewModel::consumeSaveError
    )
}

@Composable
fun LayoutEditorScreen(
    selectedTemplate: LayoutTemplate,
    onTemplateSelected: (LayoutTemplate) -> Unit,
    onResetLayout: () -> Unit,
    leftRatio: Float,
    onLeftRatioChange: (Float) -> Unit,
    topRatio: Float,
    onTopRatioChange: (Float) -> Unit,
    onBackClick: () -> Unit,
    workspaceName: String,
    onWorkspaceNameChange: (String) -> Unit,
    isNameDialogVisible: Boolean,
    isSaving: Boolean,
    saveMessage: String?,
    saveError: String?,
    onSaveClick: () -> Unit,
    onConfirmSave: () -> Unit,
    onDismissSaveDialog: () -> Unit,
    onSaveMessageShown: () -> Unit,
    onSaveErrorShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val zones = LayoutTemplates.zonesFor(
        template = selectedTemplate,
        leftRatio = leftRatio,
        topRatio = topRatio
    )

    LaunchedEffect(saveMessage) {
        saveMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onSaveMessageShown()
        }
    }

    LaunchedEffect(saveError) {
        saveError?.let { error ->
            snackbarHostState.showSnackbar(error)
            onSaveErrorShown()
        }
    }

    if (isNameDialogVisible) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) onDismissSaveDialog()
            },
            title = { Text(text = "Lưu workspace") },
            text = {
                OutlinedTextField(
                    value = workspaceName,
                    onValueChange = onWorkspaceNameChange,
                    label = { Text(text = "Tên workspace") },
                    singleLine = true,
                    enabled = !isSaving
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmSave,
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(20.dp).height(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = "Lưu")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissSaveDialog,
                    enabled = !isSaving
                ) {
                    Text(text = "Hủy")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            EditorTopBar(
                onBackClick = onBackClick,
                onSaveClick = onSaveClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                LayoutPreview(
                    zones = zones,
                    selectedTemplate = selectedTemplate,
                    leftRatio = leftRatio,
                    onLeftRatioChange = onLeftRatioChange,
                    topRatio = topRatio,
                    onTopRatioChange = onTopRatioChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onTemplateSelected(LayoutTemplate.TWO_ZONES) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Mẫu 2 vùng")
                }
                Button(
                    onClick = { onTemplateSelected(LayoutTemplate.THREE_ZONES) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Mẫu 3 vùng")
                }
                Button(
                    onClick = onResetLayout,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Xóa bố cục")
                }
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Surface(shadowElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại"
                )
            }
            Text(
                text = "Trình chỉnh sửa bố cục",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(onClick = onSaveClick) {
                Text(text = "Lưu")
            }
        }
    }
}

@Composable
private fun LayoutPreview(
    zones: List<LayoutZone>,
    selectedTemplate: LayoutTemplate,
    leftRatio: Float,
    onLeftRatioChange: (Float) -> Unit,
    topRatio: Float,
    onTopRatioChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(8.dp)

    Surface(
        modifier = modifier
            .border(1.dp, borderColor, shape)
            .clip(shape),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (zones.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Chưa có vùng nào")
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val previewWidthPx = constraints.maxWidth.toFloat()
                val previewHeightPx = constraints.maxHeight.toFloat()

                zones.forEach { zone ->
                    PreviewZone(
                        zone = zone,
                        modifier = Modifier
                            .offset(
                                x = maxWidth * zone.x,
                                y = maxHeight * zone.y
                            )
                            .width(maxWidth * zone.width)
                            .height(maxHeight * zone.height)
                    )
                }

                if (selectedTemplate == LayoutTemplate.THREE_ZONES) {
                    val verticalDividerState = rememberDraggableState { deltaPx ->
                        val ratioDelta = deltaPx / previewWidthPx
                        val newRatio = (leftRatio + ratioDelta).coerceIn(0.4f, 0.8f)
                        onLeftRatioChange(newRatio)
                    }
                    val horizontalDividerState = rememberDraggableState { deltaPx ->
                        val ratioDelta = deltaPx / previewHeightPx
                        val newRatio = (topRatio + ratioDelta).coerceIn(0.25f, 0.75f)
                        onTopRatioChange(newRatio)
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = maxWidth * leftRatio - 12.dp)
                            .width(24.dp)
                            .fillMaxHeight()
                            .zIndex(1f)
                            .draggable(
                                state = verticalDividerState,
                                orientation = Orientation.Horizontal
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(
                                x = maxWidth * leftRatio,
                                y = maxHeight * topRatio - 12.dp
                            )
                            .width(maxWidth * (1f - leftRatio))
                            .height(24.dp)
                            .zIndex(1f)
                            .draggable(
                                state = horizontalDividerState,
                                orientation = Orientation.Vertical
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .padding(horizontal = 8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewZone(
    zone: LayoutZone,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.border(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = zone.label,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LayoutEditorScreenPreview() {
    DexWorkspaceManagerTheme {
        LayoutEditorScreen(
            selectedTemplate = LayoutTemplate.THREE_ZONES,
            onTemplateSelected = {},
            onResetLayout = {},
            leftRatio = 0.65f,
            onLeftRatioChange = {},
            topRatio = 0.5f,
            onTopRatioChange = {},
            onBackClick = {},
            workspaceName = "Workspace của tôi",
            onWorkspaceNameChange = {},
            isNameDialogVisible = false,
            isSaving = false,
            saveMessage = null,
            saveError = null,
            onSaveClick = {},
            onConfirmSave = {},
            onDismissSaveDialog = {},
            onSaveMessageShown = {},
            onSaveErrorShown = {}
        )
    }
}
