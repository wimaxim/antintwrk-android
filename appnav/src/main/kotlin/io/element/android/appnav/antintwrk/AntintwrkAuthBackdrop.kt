/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Local, non-interactive artwork adapted from the public antintwrk footer-growth.svg. */
@Composable
internal fun AntintwrkAuthBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(AntintwrkBackground).clipToBounds()) {
        Canvas(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(260.dp)) {
            val center = Offset(size.width / 2, size.height * 0.68f)
            drawRect(Brush.radialGradient(listOf(Color(0xFF57C9B8).copy(alpha = 0.13f), Color.Transparent), center, size.width * 0.6f))
            drawRect(Brush.radialGradient(
                listOf(Color(0xFFF06B55).copy(alpha = 0.055f), Color.Transparent),
                Offset(center.x, size.height * 0.86f),
                size.width * 0.8f,
            ))
            // Keep strokes in density-independent pixels rather than stretching a bitmap on phones/tablets.
            fun point(x: Float, y: Float) = Offset(x / 1600 * size.width, y / 210 * size.height)
            fun branch(points: List<Offset>, color: Color) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, color, style = Stroke(1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            branch(listOf(point(800f, 210f), point(800f, 134f)), Color(0xFF69C8BD).copy(alpha = 0.34f))
            val branches = listOf(
                listOf(800f to 134f, 706f to 134f, 642f to 107f, 448f to 107f, 366f to 58f, 176f to 58f, 112f to 30f, 0f to 30f),
                listOf(800f to 155f, 658f to 155f, 578f to 181f, 372f to 181f, 274f to 133f, 104f to 133f),
                listOf(800f to 142f, 744f to 142f, 656f to 178f, 500f to 178f, 414f to 148f, 288f to 148f),
            )
            branches.forEachIndexed { index, points ->
                val color = when (index) {
                    0 -> Color(0xFF7FAAA8).copy(alpha = 0.28f)
                    1 -> Color(0xFF7FAAA8).copy(alpha = 0.2f)
                    else -> Color(0xFFF06B55).copy(alpha = 0.18f)
                }
                branch(points.map { (x, y) -> point(x, y) }, color)
                branch(points.map { (x, y) -> point(1600f - x, y) }, color)
            }
            listOf(366f to 58f, 1152f to 107f, 578f to 181f, 1022f to 181f).forEach { (x, y) ->
                drawCircle(Color(0xFF8AC9C0).copy(alpha = 0.34f), radius = 2.dp.toPx(), center = point(x, y))
            }
            // The website uses a vertical mask; fade both edges into the same native canvas colour.
            drawRect(Brush.verticalGradient(
                0f to AntintwrkBackground,
                0.28f to Color.Transparent,
                0.88f to Color.Transparent,
                1f to AntintwrkBackground,
            ))
        }
        content()
    }
}
