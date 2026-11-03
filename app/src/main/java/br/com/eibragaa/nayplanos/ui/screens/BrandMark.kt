package br.com.eibragaa.nayplanos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
            val paper = Color(0xFFF7FBFF)
            val ink = LightPrimary
            val gold = Color(0xFFF2C777)
            val left = size.width * .23f
            val top = size.height * .2f
            val right = size.width * .77f
            val bottom = size.height * .8f
            val foldX = size.width * .61f
            val foldY = size.height * .34f
            val sheet = Path().apply {
                moveTo(left, top)
                lineTo(foldX, top)
                lineTo(right, foldY)
                lineTo(right, bottom)
                lineTo(left, bottom)
                close()
            }
            drawPath(sheet, paper)
            drawLine(paper, Offset(foldX, top), Offset(foldX, foldY), size.width * .045f)
            drawLine(paper, Offset(foldX, foldY), Offset(right, foldY), size.width * .045f)
            drawLine(ink, Offset(size.width * .35f, size.height * .5f), Offset(size.width * .68f, size.height * .5f), size.width * .045f)
            drawLine(ink, Offset(size.width * .35f, size.height * .63f), Offset(size.width * .58f, size.height * .63f), size.width * .045f)
            val sparkle = Path().apply {
                moveTo(size.width * .79f, size.height * .16f)
                lineTo(size.width * .79f, size.height * .28f)
                moveTo(size.width * .73f, size.height * .22f)
                lineTo(size.width * .85f, size.height * .22f)
            }
            drawPath(sparkle, gold, style = Stroke(width = size.width * .05f))
        }
    }
}
