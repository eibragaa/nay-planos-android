package br.com.eibragaa.nayplanos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.eibragaa.nayplanos.ui.design.LightPrimary

@Composable
fun NayBrandMark(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(LightPrimary)
            .then(
                if (contentDescription == null) Modifier
                else Modifier.semantics { this.contentDescription = contentDescription },
            ),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val page = Color(0xFFFFF9F5)
            val center = size.width / 2f
            val top = size.height * .28f
            val bottom = size.height * .72f
            val left = size.width * .2f
            val right = size.width * .8f
            val fold = Path().apply {
                moveTo(center, top)
                lineTo(center, bottom)
            }
            drawPath(fold, page, style = Stroke(width = size.width * .055f))
            drawLine(page, Offset(left, top), Offset(center, top + size.height * .07f), size.width * .055f)
            drawLine(page, Offset(center, top + size.height * .07f), Offset(right, top), size.width * .055f)
            drawLine(page, Offset(left, bottom), Offset(center, bottom - size.height * .07f), size.width * .055f)
            drawLine(page, Offset(center, bottom - size.height * .07f), Offset(right, bottom), size.width * .055f)
            drawCircle(page, radius = size.width * .055f, center = Offset(center, size.height * .18f))
        }
    }
}
