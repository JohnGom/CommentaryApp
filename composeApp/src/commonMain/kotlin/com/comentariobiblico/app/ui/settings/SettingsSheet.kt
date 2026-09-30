package com.comentariobiblico.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comentariobiblico.app.domain.model.LineHeight
import com.comentariobiblico.app.domain.model.ReaderSettings
import com.comentariobiblico.app.domain.model.ThemeMode
import com.comentariobiblico.app.ui.theme.LocalReaderTypography
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Lectura", style = MaterialTheme.typography.titleLarge)

            Setting("Tema") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = listOf(
                        ThemeMode.SYSTEM to "Sistema",
                        ThemeMode.LIGHT to "Claro",
                        ThemeMode.DARK to "Oscuro",
                        ThemeMode.SEPIA to "Sepia",
                    )
                    options.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = settings.theme == mode,
                            onClick = { viewModel.setTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) { Text(label) }
                    }
                }
            }

            Setting("Tamaño del texto") {
                val steps = ReaderSettings.FONT_SCALES
                val current = steps.indexOfFirst { it == settings.fontScale }
                    .takeIf { it >= 0 } ?: 1
                Slider(
                    value = current.toFloat(),
                    onValueChange = { viewModel.setFontScale(steps[it.toInt()]) },
                    valueRange = 0f..(steps.size - 1).toFloat(),
                    steps = steps.size - 2,
                )
                // Vista previa en vivo: elegir un tamaño a ciegas y tener que
                // salir a comprobarlo es la forma más rápida de no cambiarlo.
                Text(
                    "En el principio creó Dios los cielos y la tierra.",
                    style = LocalReaderTypography.current.body,
                )
            }

            Setting("Interlineado") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = listOf(
                        LineHeight.COMPACT to "Compacto",
                        LineHeight.NORMAL to "Normal",
                        LineHeight.RELAXED to "Amplio",
                    )
                    options.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = settings.lineHeight == value,
                            onClick = { viewModel.setLineHeight(value) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) { Text(label) }
                    }
                }
            }

            SwitchRow(
                label = "Tipografía con serifa",
                checked = settings.serif,
                onChange = viewModel::setSerif,
            )

            SwitchRow(
                label = "Mantener la pantalla encendida",
                checked = settings.keepScreenOn,
                onChange = viewModel::setKeepScreenOn,
            )
        }
    }
}

@Composable
private fun Setting(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
