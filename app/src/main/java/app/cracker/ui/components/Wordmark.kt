package app.cracker.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cracker.ui.theme.Cheddar

@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CheeseMark()
        Spacer(Modifier.width(10.dp))
        Text(
            text = "crack",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            letterSpacing = (-0.8).sp,
        )
        Text(
            text = "er",
            color = Cheddar,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            letterSpacing = (-0.8).sp,
        )
    }
}
