package com.trancong.dexworkspacemanager.feature.layouteditor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private enum class LayoutTemplate {
    TwoZones,
    ThreeZones,
    Empty
}

@Composable
fun LayoutEditorScreen(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTemplate by remember { mutableStateOf(LayoutTemplate.ThreeZones) }

    Scaffold(
        modifier = modifier,
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
                    template = selectedTemplate,
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
                    onClick = { selectedTemplate = LayoutTemplate.TwoZones },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Mẫu 2 vùng")
                }
                Button(
                    onClick = { selectedTemplate = LayoutTemplate.ThreeZones },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Mẫu 3 vùng")
                }
                Button(
                    onClick = { selectedTemplate = LayoutTemplate.Empty },
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
    template: LayoutTemplate,
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
        when (template) {
            LayoutTemplate.TwoZones -> Row(Modifier.fillMaxSize()) {
                PreviewZone(
                    label = "Vùng 1",
                    modifier = Modifier.weight(1f)
                )
                PreviewZone(
                    label = "Vùng 2",
                    modifier = Modifier
                        .weight(1f)
                        .border(0.5.dp, borderColor)
                )
            }

            LayoutTemplate.ThreeZones -> Row(Modifier.fillMaxSize()) {
                PreviewZone(
                    label = "Vùng 1",
                    modifier = Modifier.weight(0.65f)
                )
                Column(
                    modifier = Modifier
                        .weight(0.35f)
                        .border(0.5.dp, borderColor)
                ) {
                    PreviewZone(
                        label = "Vùng 2",
                        modifier = Modifier.weight(1f)
                    )
                    PreviewZone(
                        label = "Vùng 3",
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, borderColor)
                    )
                }
            }

            LayoutTemplate.Empty -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Chưa có vùng nào")
            }
        }
    }
}

@Composable
private fun PreviewZone(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
