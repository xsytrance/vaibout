package com.xsytrance.vaib.vaib

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.core.design.VaibColors
import com.xsytrance.vaib.data.entities.VaibEntity

@Composable
fun VaibCard(vaib: VaibEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VaibColors.DeepBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = vaib.vaibName,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = vaib.trackName,
            color = VaibColors.TextSoft,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
        )
        if (vaib.mood.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row {
                Text(
                    text = vaib.mood,
                    color = VaibColors.CyanPulse.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "  ·  ${vaib.visualizerStyle}",
                    color = VaibColors.TextSoft.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                )
                if (vaib.sourceType == "INTERNET_ARCHIVE") {
                    Text(
                        text = "  ·  Internet Archive",
                        color = VaibColors.CyanPulse.copy(alpha = 0.40f),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}
