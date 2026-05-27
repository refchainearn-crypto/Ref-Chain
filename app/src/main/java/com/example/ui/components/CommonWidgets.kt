package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
    elevation: Dp = 6.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(elevation, shape = RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor,
                        backgroundColor.copy(alpha = 0.85f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderColor,
                        borderColor.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "APPROVED", "SUCCESS", "COMPLETED", "ACTIVE" -> Pair(EmeraldPrimary.copy(alpha = 0.12f), EmeraldPrimary)
        "PENDING", "UNDER_REVIEW", "PROCESSING", "OPEN" -> Pair(GoldSecondary.copy(alpha = 0.12f), GoldSecondary)
        "REJECTED", "SUSPENDED", "HIGH" -> Pair(CoralDanger.copy(alpha = 0.12f), CoralDanger)
        else -> Pair(CyanAccent.copy(alpha = 0.12f), CyanAccent)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(width = 0.5.dp, color = textColor.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    trendUp: Boolean? = null,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )

            trendUp?.let {
                val trendColor = if (it) EmeraldPrimary else CoralDanger
                val arrow = if (it) "↑" else "↓"
                Text(
                    text = "$arrow Premium",
                    color = trendColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FinTechGrowthChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    primaryColor: Color = EmeraldPrimary,
    accentColor: Color = CyanAccent
) {
    if (dataPoints.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No Chart Analytics Data Available", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
        return
    }

    val maxVal = (dataPoints.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val minVal = dataPoints.minOrNull() ?: 0f
    val range = (maxVal - minVal).coerceAtLeast(1f)

    val transitionState = remember { MutableTransitionState(0f) }
    LaunchedEffect(dataPoints) {
        transitionState.targetState = 1f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = transitionState.currentState,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "chart_growth_anim"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val totalPoints = dataPoints.size

        val spacing = width / (totalPoints - 1).coerceAtLeast(1)
        val path = Path()
        val fillPath = Path()

        for (i in 0 until totalPoints) {
            val ratio = (dataPoints[i] - minVal) / range
            val x = i * spacing
            // Invert Y coordinate so higher values are drawn higher up
            val y = height - (ratio * (height - 40f) * animatedProgress) - 10f

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (i == totalPoints - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // Draw background gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.25f),
                    accentColor.copy(alpha = 0.01f)
                ),
                startY = 0f,
                endY = height
            )
        )

        // Draw main line path
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw circular dots over values
        for (i in 0 until totalPoints) {
            val ratio = (dataPoints[i] - minVal) / range
            val x = i * spacing
            val y = height - (ratio * (height - 40f) * animatedProgress) - 10f

            drawCircle(
                color = accentColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = primaryColor,
                radius = 2.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
