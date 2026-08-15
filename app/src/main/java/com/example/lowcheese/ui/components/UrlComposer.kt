package com.example.lowcheese.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.lowcheese.ui.theme.Cheddar
import com.example.lowcheese.ui.theme.Ink

@Composable
fun UrlComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    resolving: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(Cheddar),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            "치지직 링크를 붙여 넣기",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                }
            },
        )
        FilledIconButton(
            onClick = onSubmit,
            enabled = value.isNotBlank() && !resolving,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Cheddar,
                contentColor = Ink,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            shape = CircleShape,
        ) {
            if (resolving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Ink,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(Icons.Filled.ArrowUpward, contentDescription = "추가")
            }
        }
    }
}
