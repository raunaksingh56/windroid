/*
 * WinDroid - Progress & Done Screens
 * Created by Raunak Singh
 */

package com.raunaksingh.windroid.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.raunaksingh.windroid.core.*
import com.raunaksingh.windroid.ui.theme.*

// ── Progress Screen ────────────────────────────────────────────────────────────

@Composable
fun ProgressScreen(state: FlashState, vm: WinDroidViewModel) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(ElectricBlue.copy(alpha = 0.07f * pulseAlpha), Color.Transparent),
                    center = Offset(size.width / 2, size.height / 3),
                    radius = size.width * 0.7f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            // Title
            Text(
                "Flashing Windows...",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Do not remove the USB drive",
                style = MaterialTheme.typography.bodyMedium,
                color = WarningAmber
            )

            Spacer(Modifier.height(48.dp))

            // Big progress circle
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { state.flashProgress / 100f },
                    modifier = Modifier.size(160.dp),
                    color    = ElectricBlue,
                    trackColor = NavyBorder,
                    strokeWidth = 10.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${state.flashProgress}%",
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Black
                    )
                    Text("complete", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Current action
            Text(
                state.flashMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = CyanAccent,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { state.flashProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color     = ElectricBlue,
                trackColor = NavyBorder
            )

            Spacer(Modifier.height(28.dp))

            // Log output
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                border = BorderStroke(1.dp, NavyBorder)
            ) {
                val scrollState = rememberScrollState()
                LaunchedEffect(state.logLines.size) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(14.dp)
                ) {
                    state.logLines.forEach { line ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                "›",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ElectricBlue,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                    }
                }
            }

            // Error state
            if (state.errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape  = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Error, null, tint = ErrorRed)
                        Spacer(Modifier.width(10.dp))
                        Text(state.errorMessage, style = MaterialTheme.typography.bodyMedium, color = ErrorRed)
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { vm.navigateTo(Screen.Home) },
                    modifier = Modifier.fillMaxWidth(),
                    border   = BorderStroke(1.dp, ElectricBlue),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text("Go Back", color = ElectricBlue)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Done Screen ────────────────────────────────────────────────────────────────

@Composable
fun DoneScreen(state: FlashState, vm: WinDroidViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(SuccessGreen.copy(alpha = 0.1f), Color.Transparent),
                    center = Offset(size.width / 2, size.height * 0.35f),
                    radius = size.width * 0.65f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(listOf(SuccessGreen.copy(alpha = 0.3f), Color.Transparent)),
                        CircleShape
                    )
                    .border(2.dp, SuccessGreen.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "USB Ready!",
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Your bootable Windows USB is ready.\nPlug it into your PC and boot!",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(36.dp))

            // Boot level badge
            if (state.bootLevel != null) {
                BootLevelBadge(state.bootLevel, state.bootLevelMessage)
                Spacer(Modifier.height(20.dp))
            }

            // Boot instructions card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "How to boot",
                        style = MaterialTheme.typography.titleMedium,
                        color = SuccessGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    BootStep("1", "Plug USB into target PC")
                    BootStep("2", "Power on → press F12, F9, or ESC for boot menu")
                    BootStep("3", "Select your USB drive")
                    BootStep("4", "Windows setup starts automatically!")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ElectricBlue.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Info, null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Works on both UEFI and Legacy BIOS — no BIOS changes needed!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyanAccent
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick  = { vm.reset() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Flash Another USB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))

            // Author credit at bottom
            Text(
                "WinDroid • Created by Raunak Singh",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BootLevelBadge(level: com.raunaksingh.windroid.core.BootLevel, message: String) {
    val (icon, label, color, bg) = when (level) {
        com.raunaksingh.windroid.core.BootLevel.ROOTLESS ->
            Quadruple(Icons.Rounded.Verified, "Standard Hybrid Boot", CyanAccent, CyanAccent.copy(alpha = 0.1f))
        com.raunaksingh.windroid.core.BootLevel.HYBRID_MBR ->
            Quadruple(Icons.Rounded.Shield, "Enhanced Hybrid Boot", ElectricBlue, ElectricBlue.copy(alpha = 0.1f))
        com.raunaksingh.windroid.core.BootLevel.FULL_HYBRID ->
            Quadruple(Icons.Rounded.Stars, "Full Hybrid Boot", SuccessGreen, SuccessGreen.copy(alpha = 0.1f))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = bg),
        border   = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}

data class Quadruple<A,B,C,D>(val first: A, val second: B, val third: C, val fourth: D)
operator fun <A,B,C,D> Quadruple<A,B,C,D>.component1() = first
operator fun <A,B,C,D> Quadruple<A,B,C,D>.component2() = second
operator fun <A,B,C,D> Quadruple<A,B,C,D>.component3() = third
operator fun <A,B,C,D> Quadruple<A,B,C,D>.component4() = fourth

@Composable
fun BootStep(number: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(ElectricBlue.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number, style = MaterialTheme.typography.labelSmall, color = ElectricBlue, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}
