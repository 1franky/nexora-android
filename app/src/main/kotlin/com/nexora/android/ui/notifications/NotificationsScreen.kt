package com.nexora.android.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexora.android.R
import com.nexora.android.data.notification.Notification
import com.nexora.android.data.notification.NotificationRepository
import com.nexora.android.data.notification.NotificationStatus
import com.nexora.android.data.notification.NotificationType
import com.nexora.android.ui.common.formatDateShort

@Composable
fun NotificationsScreen(notificationRepository: NotificationRepository) {
    val viewModel: NotificationsViewModel = viewModel(
        factory = viewModelFactory { initializer { NotificationsViewModel(notificationRepository) } },
    )
    val fallbackError = stringResource(R.string.notifications_load_error)
    LaunchedEffect(Unit) { viewModel.load(fallbackError) }

    val state = viewModel.uiState

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.notifications_title), style = MaterialTheme.typography.headlineSmall)
                val unreadCount = (state as? NotificationsUiState.Success)?.notifications?.count { it.status == NotificationStatus.UNREAD } ?: 0
                if (unreadCount > 0) {
                    Text(
                        unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            if (state is NotificationsUiState.Success && state.notifications.any { it.status == NotificationStatus.UNREAD }) {
                TextButton(onClick = { viewModel.markAllAsRead(fallbackError) }) {
                    Text(stringResource(R.string.notifications_mark_all_read))
                }
            }
        }

        when (state) {
            NotificationsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is NotificationsUiState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(state.message, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { viewModel.load(fallbackError) }, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.retry))
                }
            }
            is NotificationsUiState.Success -> {
                if (state.notifications.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.notifications_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.notifications, key = { it.id }) { notification ->
                            NotificationRow(
                                notification = notification,
                                onClick = {
                                    if (notification.status == NotificationStatus.UNREAD) {
                                        viewModel.markAsRead(notification.id, fallbackError)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun iconFor(type: NotificationType): ImageVector = when (type) {
    NotificationType.INSTALLMENT_DUE -> Icons.Outlined.CalendarMonth
    NotificationType.PAYMENT_DUE, NotificationType.PAYMENT_DUE_SOON, NotificationType.PAYMENT_OVERDUE -> Icons.Outlined.CreditCard
    NotificationType.BUDGET_EXCEEDED, NotificationType.UNUSUAL_EXPENSE -> Icons.Outlined.Notifications
    NotificationType.SAT_SYNC_COMPLETED, NotificationType.SAT_SYNC_FAILED -> Icons.Outlined.Description
}

@Composable
private fun NotificationRow(notification: Notification, onClick: () -> Unit) {
    val unread = notification.status == NotificationStatus.UNREAD
    val contentColor = if (unread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = unread, onClick = onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .let {
                if (unread) it.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp)) else it
            }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (unread) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                iconFor(notification.type),
                contentDescription = null,
                tint = if (unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                )
                if (unread) {
                    Text(
                        stringResource(R.string.notifications_unread),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                notification.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                formatDateShort(notification.createdAt.take(10)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
