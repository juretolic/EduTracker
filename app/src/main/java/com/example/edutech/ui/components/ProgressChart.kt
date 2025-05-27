package com.example.edutech.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.edutech.model.AssessmentType

@Composable
fun ProgressChart(
    progressByType: Map<AssessmentType, Double>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Progress by Assessment Type",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            progressByType.forEach { (type, progress) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Draw background circle
                            drawCircle(
                                color = Color.LightGray,
                                style = Stroke(width = 8f)
                            )
                            
                            // Draw progress arc
                            drawArc(
                                color = when {
                                    progress >= 90 -> Color(0xFF2E7D32)
                                    progress >= 70 -> Color(0xFF1976D2)
                                    else -> Color(0xFFC62828)
                                },
                                startAngle = -90f,
                                sweepAngle = ((progress / 100.0) * 360.0).toFloat(),
                                useCenter = false,
                                style = Stroke(width = 8f)
                            )
                        }
                        Text(
                            text = "%.1f%%".format(progress),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = type.name.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw background
            drawRect(
                color = Color.LightGray,
                size = Size(size.width, size.height)
            )
            
            // Draw progress
            drawRect(
                color = when {
                    progress >= 0.9f -> Color(0xFF2E7D32)
                    progress >= 0.7f -> Color(0xFF1976D2)
                    else -> Color(0xFFC62828)
                },
                size = Size(size.width * progress, size.height)
            )
        }
    }
} 