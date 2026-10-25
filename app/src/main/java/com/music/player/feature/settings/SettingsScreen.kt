/*
 * Settings screen: theme mode, artwork-derived colors, the short-file filter, library rescan,
 * and an About section. Appearance follows blueprints/visual-theme.md; the filter and rescan
 * follow blueprints/library-sync.md.
 */
package com.music.player.feature.settings

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.player.core.model.ThemeMode
import com.music.player.core.ui.components.Formatting
import com.music.player.core.ui.components.clickableButton
import com.music.player.core.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

/** Minimum-length choices offered for the short-file filter, in seconds. */
private val MinDurationChoices = listOf(0, 15, 30, 60)

/** Settings screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(contentPadding: PaddingValues, viewModel: SettingsViewModel = koinViewModel()) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val minDuration by viewModel.minDurationSec.collectAsStateWithLifecycle()
    val trackCount by viewModel.trackCount.collectAsStateWithLifecycle()
    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version = remember(context) { versionName(context) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings", style = MaterialTheme.typography.headlineMedium) })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding()),
        ) {
            Section("Appearance")
            Text(
                "Theme",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            )
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
            ) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = appearance.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    ) { Text(mode.label) }
                }
            }
            ListItem(
                modifier = Modifier.toggleable(
                    value = appearance.artworkColors,
                    role = Role.Switch,
                    onValueChange = viewModel::setArtworkColors,
                ),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                headlineContent = { Text("Colors from artwork") },
                supportingContent = { Text("Tint the app with the colors of the current album cover") },
                trailingContent = { Switch(checked = appearance.artworkColors, onCheckedChange = null) },
            )

            HorizontalDivider(Modifier.padding(vertical = Spacing.xs))
            Section("Library")
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                headlineContent = { Text("Hide short audio") },
                supportingContent = { Text("Skip voice notes and sound clips shorter than this") },
            )
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
            ) {
                MinDurationChoices.forEachIndexed { index, seconds ->
                    SegmentedButton(
                        selected = minDuration == seconds,
                        onClick = { if (minDuration != seconds) viewModel.setMinDurationSec(seconds) },
                        shape = SegmentedButtonDefaults.itemShape(index, MinDurationChoices.size),
                    ) { Text(if (seconds == 0) "Off" else "${seconds}s") }
                }
            }
            ListItem(
                modifier = Modifier.clickableButton { if (!scanning) viewModel.rescan() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                headlineContent = { Text("Rescan library") },
                supportingContent = { Text(if (scanning) "Scanning\u2026" else "${Formatting.songs(trackCount)} on this device") },
                trailingContent = {
                    if (scanning) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                },
            )

            HorizontalDivider(Modifier.padding(vertical = Spacing.xs))
            Section("About")
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) },
                headlineContent = { Text("Music") },
                supportingContent = { Text("Version $version") },
            )
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { Icon(Icons.Rounded.WifiOff, contentDescription = null) },
                headlineContent = { Text("Works offline") },
                supportingContent = { Text("Music plays files on this device and has no internet access. Nothing leaves your phone.") },
            )
        }
    }
}

/** Section title. */
@Composable
private fun Section(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.xs),
    )
}

/** User-facing label of a theme mode. */
private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.System -> "System"
        ThemeMode.Light -> "Light"
        ThemeMode.Dark -> "Dark"
    }

/** The installed version name, or an empty string if the package cannot be read. */
private fun versionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
