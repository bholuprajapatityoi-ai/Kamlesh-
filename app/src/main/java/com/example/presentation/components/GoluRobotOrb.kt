package com.example.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.VoiceState
import com.example.domain.model.WakeWordState
import com.example.ui.theme.GoluAmber
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluNeonCyan
import kotlin.math.PI
import kotlin.math.sin

/**
 * Custom animated futuristic GOLU Robot Orb rendered cleanly via Jetpack Compose Canvas.
 * Reacts visually to Idle, Listening, Processing, Speaking, Paused, Error, and Wake-Word states.
 */
@Composable
fun GoluRobotOrb(
    voiceState: VoiceState,
    wakeWordState: WakeWordState,
    audioLevel: Float,
    size: Dp = 200.dp,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "golu_robot_anim")

    val pulse by
        transition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "pulse",
        )

    val wavePhase by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "wave",
        )

    val blink by
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        androidx.compose.animation.core.keyframes {
                            durationMillis = 3600
                            1f at 0
                            1f at 3200
                            0.12f at 3350
                            1f at 3520
                        },
                    repeatMode = RepeatMode.Restart,
                ),
            label = "blink",
        )

    val primaryGlow =
        when {
            voiceState == VoiceState.ERROR -> GoluCoralRed
            voiceState == VoiceState.LISTENING -> GoluNeonCyan
            voiceState == VoiceState.SPEAKING -> GoluElectricBlue
            voiceState == VoiceState.PROCESSING -> GoluAmber
            wakeWordState == WakeWordState.MONITORING -> Color(0xFF38BDF8)
            else -> GoluElectricBlue
        }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            modifier
                .size(size)
                .semantics { contentDescription = "GOLU animated robot assistant. Tap to activate voice." }
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false, color = primaryGlow),
                    onClick = onTap,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)
            val baseRadius = (minOf(w, h) / 2f) * 0.72f

            // Outer neon aura
            val dynamicBoost = if (voiceState == VoiceState.LISTENING) 1f + (audioLevel * 0.22f) else pulse
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                primaryGlow.copy(alpha = 0.34f),
                                primaryGlow.copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                        center = center,
                        radius = baseRadius * 1.38f * dynamicBoost,
                    ),
                radius = baseRadius * 1.38f * dynamicBoost,
                center = center,
            )

            // Rotating / pulsing orbital ring
            drawCircle(
                color = primaryGlow.copy(alpha = 0.5f),
                radius = baseRadius * 1.12f * dynamicBoost,
                center = center,
                style = Stroke(width = 3.dp.toPx()),
            )

            // Robot antenna
            val antennaTop = Offset(center.x, center.y - baseRadius * 0.92f)
            val headTop = Offset(center.x, center.y - baseRadius * 0.68f)
            drawLine(
                color = Color(0xFF94A3B8),
                start = headTop,
                end = antennaTop,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = if (wakeWordState == WakeWordState.MONITORING || voiceState == VoiceState.LISTENING) GoluNeonCyan else primaryGlow,
                radius = 6.dp.toPx() * pulse,
                center = antennaTop,
            )

            // Side ear pods
            val earW = baseRadius * 0.16f
            val earH = baseRadius * 0.44f
            drawRoundRect(
                color = Color(0xFF1E3A5F),
                topLeft = Offset(center.x - baseRadius * 0.95f, center.y - earH / 2f),
                size = Size(earW, earH),
                cornerRadius = CornerRadius(12f, 12f),
            )
            drawRoundRect(
                color = Color(0xFF1E3A5F),
                topLeft = Offset(center.x + baseRadius * 0.79f, center.y - earH / 2f),
                size = Size(earW, earH),
                cornerRadius = CornerRadius(12f, 12f),
            )

            // Robot head chassis
            val headW = baseRadius * 1.62f
            val headH = baseRadius * 1.32f
            val headTopLeft = Offset(center.x - headW / 2f, center.y - headH / 2f)
            drawRoundRect(
                brush =
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF182A4D), Color(0xFF0A1428)),
                    ),
                topLeft = headTopLeft,
                size = Size(headW, headH),
                cornerRadius = CornerRadius(baseRadius * 0.48f, baseRadius * 0.48f),
            )
            drawRoundRect(
                color = primaryGlow.copy(alpha = 0.75f),
                topLeft = headTopLeft,
                size = Size(headW, headH),
                cornerRadius = CornerRadius(baseRadius * 0.48f, baseRadius * 0.48f),
                style = Stroke(width = 2.5.dp.toPx()),
            )

            // Inner visor screen
            val visorW = headW * 0.78f
            val visorH = headH * 0.62f
            val visorTopLeft = Offset(center.x - visorW / 2f, center.y - visorH / 2f)
            drawRoundRect(
                color = Color(0xFF040914),
                topLeft = visorTopLeft,
                size = Size(visorW, visorH),
                cornerRadius = CornerRadius(baseRadius * 0.30f, baseRadius * 0.30f),
            )

            // Expressive Eyes
            val eyeOffsetX = visorW * 0.22f
            val eyeCenterY = center.y - visorH * 0.08f
            val eyeW = visorW * 0.20f
            val eyeH = (visorH * 0.24f) * blink

            val eyeColor =
                when (voiceState) {
                    VoiceState.ERROR -> GoluCoralRed
                    VoiceState.PROCESSING -> GoluAmber
                    else -> GoluNeonCyan
                }

            // Left & right playful eyes
            drawRoundRect(
                color = eyeColor,
                topLeft = Offset(center.x - eyeOffsetX - eyeW / 2f, eyeCenterY - eyeH / 2f),
                size = Size(eyeW, eyeH),
                cornerRadius = CornerRadius(eyeW / 2f, eyeW / 2f),
            )
            drawRoundRect(
                color = eyeColor,
                topLeft = Offset(center.x + eyeOffsetX - eyeW / 2f, eyeCenterY - eyeH / 2f),
                size = Size(eyeW, eyeH),
                cornerRadius = CornerRadius(eyeW / 2f, eyeW / 2f),
            )

            // Mouth: animated speaking/listening waveform OR playful smile
            val mouthY = center.y + visorH * 0.24f
            if (voiceState == VoiceState.SPEAKING || voiceState == VoiceState.LISTENING) {
                val mouthPath = Path()
                val mouthHalfW = visorW * 0.26f
                val amp =
                    if (voiceState == VoiceState.LISTENING) {
                        (6f + audioLevel * 14f).dp.toPx()
                    } else {
                        10.dp.toPx()
                    }
                val steps = 20
                for (i in 0..steps) {
                    val frac = i / steps.toFloat()
                    val x = (center.x - mouthHalfW) + (frac * mouthHalfW * 2f)
                    val envelope = sin(frac * PI).toFloat()
                    val y = mouthY + sin(wavePhase + frac * 10f) * amp * envelope
                    if (i == 0) mouthPath.moveTo(x, y) else mouthPath.lineTo(x, y)
                }
                drawPath(
                    path = mouthPath,
                    color = primaryGlow,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            } else {
                // Playful smile arc
                val smilePath =
                    Path().apply {
                        val half = visorW * 0.16f
                        moveTo(center.x - half, mouthY - 2.dp.toPx())
                        quadraticTo(
                            center.x,
                            mouthY + 11.dp.toPx(),
                            center.x + half,
                            mouthY - 2.dp.toPx(),
                        )
                    }
                drawPath(
                    path = smilePath,
                    color = eyeColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}
