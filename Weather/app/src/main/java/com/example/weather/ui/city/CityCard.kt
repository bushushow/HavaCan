package com.example.weather.ui.city

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.weather.R
import com.example.weather.data.Region
import com.example.weather.ui.theme.WeatherTheme
import com.example.weather.ui.theme.regionColor

/**
 * A tappable city row: a coloured badge (plate code or country code), the city name and a subtitle.
 * Takes plain values rather than a model so it stays reusable as the data layer grows.
 */
@Composable
fun CityCard(
    name: String,
    subtitle: String?,
    badge: String,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    /** Replaces the trailing chevron, e.g. with an "İlçeler" button. */
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.selected = selected },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colors.primaryContainer else colors.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) colors.primary else colors.outline.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PlateBadge(text = badge, color = badgeColor)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            if (trailing != null && !selected) {
                trailing()
            } else {
                Icon(
                    painter = painterResource(if (selected) R.drawable.ic_check_circle else R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = if (selected) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun PlateBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.85f), color))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CityCardPreview() {
    WeatherTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CityCard(
                name = "Ankara",
                subtitle = "İç Anadolu Bölgesi",
                badge = "06",
                badgeColor = regionColor(Region.IC_ANADOLU),
                onClick = {},
            )
            CityCard(
                name = "İzmir",
                subtitle = "Ege Bölgesi",
                badge = "35",
                badgeColor = regionColor(Region.EGE),
                onClick = {},
                selected = true,
            )
        }
    }
}
