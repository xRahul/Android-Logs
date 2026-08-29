package `in`.rahulja.getlogs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.rahulja.getlogs.model.LogEntity
import `in`.rahulja.getlogs.model.LogType
import `in`.rahulja.getlogs.ui.theme.LogTypeGeneralColor
import `in`.rahulja.getlogs.ui.theme.LogTypeLocationColor
import `in`.rahulja.getlogs.ui.theme.LogTypeSecurityColor
import `in`.rahulja.getlogs.ui.theme.LogTypeSystemColor
import `in`.rahulja.getlogs.ui.theme.LogTypeWifiColor
import `in`.rahulja.getlogs.util.LogFormatter
import java.text.DateFormat
import java.util.Date

private const val MAX_PAYLOAD_LINES = 4

@Composable
fun LogItemCard(
    log: LogEntity,
    modifier: Modifier = Modifier,
    onLogCopied: ((LogEntity) -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    val formattedDate = remember(log.timestamp) {
        DateFormat.getDateTimeInstance().format(Date(log.timestamp))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val textToCopy = LogFormatter.formatForClipboard(log)
                clipboardManager.setText(AnnotatedString(textToCopy))
                onLogCopied?.invoke(log)
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LogTypeBadge(logType = log.logType)
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = log.action,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (log.dataPayload.isNotBlank() && log.dataPayload != "{}") {
                Spacer(modifier = Modifier.height(6.dp))
                LogPayloadPreview(dataPayload = log.dataPayload)
            }
        }
    }
}

@Composable
fun LogPayloadPreview(
    dataPayload: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = dataPayload,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = MAX_PAYLOAD_LINES,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LogTypeBadge(
    logType: LogType,
    modifier: Modifier = Modifier
) {
    val badgeColor = when (logType) {
        LogType.LOCATION -> LogTypeLocationColor
        LogType.WIFI -> LogTypeWifiColor
        LogType.SECURITY -> LogTypeSecurityColor
        LogType.SYSTEM -> LogTypeSystemColor
        LogType.GENERAL -> LogTypeGeneralColor
    }

    Box(
        modifier = modifier
            .background(
                color = badgeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = logType.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = badgeColor
        )
    }
}
