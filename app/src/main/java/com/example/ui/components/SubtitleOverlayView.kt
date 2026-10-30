package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubtitleSettings

@Composable
fun SubtitleOverlayView(
    text: String?,
    settings: SubtitleSettings,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = settings.isEnabled && !text.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val textColor = Color(settings.textColorHex)
            val boxModifier = if (settings.hasBackgroundBox) {
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            } else {
                Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            }

            Box(modifier = boxModifier) {
                Text(
                    text = text.orEmpty(),
                    style = TextStyle(
                        fontSize = settings.fontSizeSp.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        lineHeight = (settings.fontSizeSp * 1.3f).sp,
                        shadow = if (!settings.hasBackgroundBox) {
                            Shadow(color = Color.Black, blurRadius = 6f)
                        } else null
                    )
                )
            }
        }
    }
}
