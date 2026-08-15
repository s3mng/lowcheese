package app.cracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cracker.ui.theme.Cheddar
import app.cracker.ui.theme.CheddarSoft
import app.cracker.ui.theme.Ink

@Composable
fun CheeseMark(size: Dp = 28.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.minDimension
        val wedge = Path().apply {
            moveTo(w * 0.12f, w * 0.82f)
            quadraticTo(w * 0.08f, w * 0.68f, w * 0.48f, w * 0.1f)
            quadraticTo(w * 0.58f, w * 0.22f, w * 0.9f, w * 0.82f)
            quadraticTo(w * 0.5f, w * 0.92f, w * 0.12f, w * 0.82f)
            close()
        }
        drawPath(wedge, Cheddar)
        drawCircle(CheddarSoft, w * 0.07f, Offset(w * 0.42f, w * 0.48f))
        drawCircle(Ink.copy(alpha = 0.18f), w * 0.045f, Offset(w * 0.62f, w * 0.62f))
        drawCircle(CheddarSoft, w * 0.055f, Offset(w * 0.38f, w * 0.68f))
    }
}
