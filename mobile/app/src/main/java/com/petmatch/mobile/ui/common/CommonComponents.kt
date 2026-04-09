package com.petmatch.mobile.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petmatch.mobile.ui.theme.*

/** Gradient background brush (PetMatch brand) */
val petMatchGradient = Brush.linearGradient(
    colors = listOf(GradientStart, GradientEnd),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

/** App top bar with gradient title */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetMatchTopBar(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        },
        navigationIcon = { navigationIcon?.invoke() },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PrimaryPink
        )
    )
}

/** Gradient button */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.LightGray
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (enabled) petMatchGradient else
                    Brush.linearGradient(listOf(Color.LightGray, Color.LightGray))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

/** Chip selector */
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) PrimaryPink else Color.Transparent
    val textColor = if (selected) Color.White else PrimaryPink
    val border = if (selected) null else ButtonDefaults.outlinedButtonBorder()

    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(36.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
            Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(36.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPink),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
            Text(label, color = PrimaryPink, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Loading indicator with brand color */
@Composable
fun PetMatchLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PrimaryPink)
    }
}

/** Error message with retry */
@Composable
fun ErrorMessage(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        if (onRetry != null) {
            OutlinedButton(onClick = onRetry) {
                Text("Thử lại")
            }
        }
    }
}

/** Shimmer loading effect */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, cornerRadius: Dp = 8.dp) {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1200, easing = LinearEasing)),
        label = "shimmer_translate"
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 500f, 0f),
        end = Offset(translateAnim, 0f)
    )
    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius)).background(brush))
}

/** Info row for pet detail */
@Composable
fun InfoRow(label: String, value: String?, modifier: Modifier = Modifier) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(0.6f)
        )
    }
}
