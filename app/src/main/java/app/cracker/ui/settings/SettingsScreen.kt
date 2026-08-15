package app.cracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cracker.download.TransferSettings
import app.cracker.ui.theme.Cheddar
import app.cracker.ui.theme.Ink

@Composable
fun SettingsScreen(
    isLoggedIn: Boolean,
    folderLabel: String,
    customFolder: Boolean,
    vodRetries: Int,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onPickFolder: () -> Unit,
    onResetFolder: () -> Unit,
    onVodRetriesChange: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier.padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로")
            }
            Text("설정", style = MaterialTheme.typography.titleLarge)
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            AccountCard(isLoggedIn, onLogin, onLogout)
            Spacer(Modifier.height(14.dp))
            InfoCard(
                icon = { Icon(Icons.Outlined.Folder, contentDescription = null, tint = Cheddar) },
                title = "저장 위치",
                body = folderLabel,
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onPickFolder,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cheddar, contentColor = Ink),
            ) {
                Text("폴더 선택", fontWeight = FontWeight.Bold)
            }
            if (customFolder) {
                TextButton(
                    onClick = onResetFolder,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("기본 위치로 되돌리기", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(14.dp))
            RetryCard(vodRetries, onVodRetriesChange)
        }
    }
}

@Composable
private fun AccountCard(isLoggedIn: Boolean, onLogin: () -> Unit, onLogout: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = Cheddar)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isLoggedIn) "네이버 연결됨" else "네이버 로그인",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "쿠키는 안전하게 로컬에만 보관됩니다",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        if (isLoggedIn) {
            TextButton(onClick = onLogout, contentPadding = PaddingValues(0.dp)) {
                Text("로그아웃", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cheddar, contentColor = Ink),
            ) {
                Text("네이버 로그인", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RetryCard(count: Int, onChange: (Int) -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Replay, contentDescription = null, tint = Cheddar)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("다시보기 재시도", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (count == 0) "실패하면 바로 끝냅니다" else "실패하면 최대 ${count}번 다시 받습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onChange(count - 1) },
                    enabled = count > 0,
                ) {
                    Icon(Icons.Outlined.Remove, contentDescription = "줄이기")
                }
                Text(
                    "$count",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(
                    onClick = { onChange(count + 1) },
                    enabled = count < TransferSettings.MAX_RETRIES,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "늘리기")
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
