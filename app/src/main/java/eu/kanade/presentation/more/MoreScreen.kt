package eu.kanade.presentation.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import tachiyomi.core.common.Constants
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    isMangaDexLoggedIn: Boolean,
    onMangaDexLogin: (String, String) -> Unit,
    onMangaDexLogout: () -> Unit,
    onClickMangaDexSync: () -> Unit,
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickDataAndStorage: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var showLoginDialog by remember { mutableStateOf(false) }

    if (showLoginDialog) {
        MangaDexLoginDialog(
            onDismiss = { showLoginDialog = false },
            onLogin = { username, password ->
                showLoginDialog = false
                onMangaDexLogin(username, password)
            },
        )
    }

    Scaffold { contentPadding ->
        ScrollbarLazyColumn(
            modifier = Modifier.padding(contentPadding),
        ) {
            item {
                LogoHeader()
            }
            item {
                SwitchPreferenceWidget(
                    title = stringResource(MR.strings.label_downloaded_only),
                    subtitle = stringResource(MR.strings.downloaded_only_summary),
                    icon = Icons.Outlined.CloudOff,
                    checked = downloadedOnly,
                    onCheckedChanged = onDownloadedOnlyChange,
                )
            }
            item {
                SwitchPreferenceWidget(
                    title = stringResource(MR.strings.pref_incognito_mode),
                    subtitle = stringResource(MR.strings.pref_incognito_mode_summary),
                    icon = ImageVector.vectorResource(R.drawable.ic_glasses_24dp),
                    checked = incognitoMode,
                    onCheckedChanged = onIncognitoModeChange,
                )
            }

            item { HorizontalDivider() }

            item {
                TextPreferenceWidget(
                    title = "MangaDex Account",
                    subtitle = if (isMangaDexLoggedIn) "Logged in" else "Not logged in",
                    icon = Icons.Outlined.AccountCircle,
                    onPreferenceClick = {
                        if (isMangaDexLoggedIn) {
                            onMangaDexLogout()
                        } else {
                            showLoginDialog = true
                        }
                    },
                )
            }
            if (isMangaDexLoggedIn) {
                item {
                    TextPreferenceWidget(
                        title = "Sync Library",
                        subtitle = "Sync MangaDex follows to library",
                        icon = Icons.Outlined.Sync,
                        onPreferenceClick = onClickMangaDexSync,
                    )
                }
            }

            item { HorizontalDivider() }

            item {
                val downloadQueueState = downloadQueueStateProvider()
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_download_queue),
                    subtitle = when (downloadQueueState) {
                        DownloadQueueState.Stopped -> null
                        is DownloadQueueState.Paused -> {
                            val pending = downloadQueueState.pending
                            if (pending == 0) {
                                stringResource(MR.strings.paused)
                            } else {
                                "${stringResource(MR.strings.paused)} • ${
                                    pluralStringResource(
                                        MR.plurals.download_queue_summary,
                                        count = pending,
                                        pending,
                                    )
                                }"
                            }
                        }
                        is DownloadQueueState.Downloading -> {
                            val pending = downloadQueueState.pending
                            pluralStringResource(MR.plurals.download_queue_summary, count = pending, pending)
                        }
                    },
                    icon = Icons.Outlined.GetApp,
                    onPreferenceClick = onClickDownloadQueue,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.categories),
                    icon = Icons.AutoMirrored.Outlined.Label,
                    onPreferenceClick = onClickCategories,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_stats),
                    icon = Icons.Outlined.QueryStats,
                    onPreferenceClick = onClickStats,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_data_storage),
                    icon = Icons.Outlined.Storage,
                    onPreferenceClick = onClickDataAndStorage,
                )
            }

            item { HorizontalDivider() }

            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_settings),
                    icon = Icons.Outlined.Settings,
                    onPreferenceClick = onClickSettings,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.pref_category_about),
                    icon = Icons.Outlined.Info,
                    onPreferenceClick = onClickAbout,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_help),
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    onPreferenceClick = { uriHandler.openUri(Constants.URL_HELP) },
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_donate),
                    icon = Icons.Outlined.AttachMoney,
                    onPreferenceClick = { uriHandler.openUri(Constants.URL_DONATE) },
                )
            }
        }
    }
}

@Composable
private fun MangaDexLoginDialog(
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MangaDex Login") },
        text = {
            Column {
                Text("Enter your MangaDex credentials")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username or Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onLogin(username, password) },
                enabled = username.isNotBlank() && password.isNotBlank(),
            ) {
                Text("Login")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
