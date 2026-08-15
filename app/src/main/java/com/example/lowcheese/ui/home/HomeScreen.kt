package com.example.lowcheese.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lowcheese.ui.components.CheeseMark
import com.example.lowcheese.ui.components.JobCard
import com.example.lowcheese.ui.components.UrlComposer
import com.example.lowcheese.ui.components.Wordmark
import com.example.lowcheese.ui.theme.CheddarSoft
import com.example.lowcheese.ui.theme.CheddarSoftLight
import com.example.lowcheese.ui.theme.LowcheeseTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSettings: () -> Unit,
    onLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val notifyPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.confirmPending()
        else viewModel.onNotificationDenied()
    }

    LaunchedEffect(state.snackbar) {
        val message = state.snackbar ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.dismissSnackbar()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 22.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Wordmark(Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "설정",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        bottomBar = {
            Box(
                Modifier
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            ) {
                UrlComposer(
                    value = state.url,
                    onValueChange = viewModel::onUrlChange,
                    onSubmit = viewModel::submitUrl,
                    resolving = state.isResolving,
                )
            }
        },
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (isSystemInDarkTheme()) {
                                CheddarSoft.copy(alpha = 0.55f)
                            } else {
                                CheddarSoftLight.copy(alpha = 0.7f)
                            },
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            if (state.jobs.isEmpty()) {
                EmptyQueue(Modifier.padding(inner))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = inner.calculateTopPadding() + 8.dp,
                        bottom = inner.calculateBottomPadding() + 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            "큐",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(state.jobs, key = { it.id }) { job ->
                        JobCard(
                            job = job,
                            onCancel = { viewModel.cancelJob(job.id) },
                            onTogglePause = { viewModel.togglePause(job.id) },
                        )
                    }
                }
            }
        }
    }

    state.pendingMeta?.let { meta ->
        QualityPickerSheet(
            meta = meta,
            selectedQualityId = state.selectedQualityId,
            isLoggedIn = state.isLoggedIn,
            onSelectQuality = viewModel::selectQuality,
            onConfirm = {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) viewModel.confirmPending()
                else notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = viewModel::dismissSheet,
            onLogin = onLogin,
        )
    }
}

@Composable
private fun EmptyQueue(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CheeseMark(72.dp)
        Spacer(Modifier.height(20.dp))
        Text("아직 아무것도 없어요", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "라이브는 녹화하고, 다시보기는 파일로 받아요.\n아래 칸에 치지직 링크만 넣으면 됩니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyDarkPreview() {
    LowcheeseTheme(darkTheme = true) {
        EmptyQueue()
    }
}
