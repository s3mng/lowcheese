package com.example.lowcheese.ui.settings

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
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
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
import com.example.lowcheese.ui.theme.Cheddar
import com.example.lowcheese.ui.theme.Ink

@Composable
fun SettingsScreen(
    isLoggedIn: Boolean,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
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
                icon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Cheddar) },
                title = "쿠키는 이 기기에만",
                body = "성인 영상용 네이버 쿠키(NID_AUT, NID_SES)는 암호화해서 저장하고, 클라우드 백업에는 넣지 않습니다. 로그아웃하면 바로 지워요.",
            )
            Spacer(Modifier.height(14.dp))
            InfoCard(
                icon = { Icon(Icons.Outlined.Folder, contentDescription = null, tint = Cheddar) },
                title = "저장 위치",
                body = "완료된 파일은 Movies/lowcheese 폴더에 저장됩니다.",
            )
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
                    if (isLoggedIn) "성인 콘텐츠를 이 계정으로 받을 수 있어요" else "성인 방송·다시보기에만 필요해요",
                    style = MaterialTheme.typography.bodyMedium,
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
