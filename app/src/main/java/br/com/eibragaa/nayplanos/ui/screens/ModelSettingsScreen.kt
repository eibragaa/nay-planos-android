package br.com.eibragaa.nayplanos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.eibragaa.nayplanos.R
import br.com.eibragaa.nayplanos.data.model.ModelSetupState
import br.com.eibragaa.nayplanos.domain.device.LocalModel
import br.com.eibragaa.nayplanos.domain.device.ModelRecommendation
import br.com.eibragaa.nayplanos.domain.device.NoFitReason

@Composable
fun ModelSettingsScreen(
    state: ModelSetupState,
    onSelect: (modelId: String) -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDelete: (modelId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.model_section_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.model_section_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val profile = state.profile
        if (profile == null) {
            Text(
                text = stringResource(R.string.model_reading_device),
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }
        Text(
            text = stringResource(
                R.string.model_device_summary,
                profile.cpuAbi,
                (profile.totalRamMb / 1024).toInt(),
                profile.cpuCores,
                (profile.freeStorageMb / 1024).toInt(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (val recommendation = state.recommendation) {
            is ModelRecommendation.Compatible -> recommendation.options.forEachIndexed { index, model ->
                ModelOption(
                    model = model,
                    recommended = index == 0,
                    selected = model.id == state.selectedModelId,
                    installed = model.id in state.installedModelIds,
                    onSelect = { onSelect(model.id) },
                )
            }
            is ModelRecommendation.NoneFits -> Text(
                text = stringResource(noFitMessage(recommendation.reason)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            null -> Unit
        }

        val selected = state.recommendation
            ?.let { it as? ModelRecommendation.Compatible }
            ?.options
            ?.firstOrNull { it.id == state.selectedModelId }
        val progress = state.download
        if (progress != null) {
            val percent = if (progress.totalBytes > 0) {
                (progress.downloadedBytes * 100 / progress.totalBytes).toInt()
            } else {
                0
            }
            Text(
                text = stringResource(R.string.model_downloading, progress.file, percent),
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = onCancelDownload, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.model_cancel_download))
            }
        } else if (selected != null) {
            val installed = selected.id in state.installedModelIds
            if (installed) {
                Text(
                    text = stringResource(R.string.model_installed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(
                    onClick = { onDelete(selected.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Remover o modelo instalado e liberar espaço" },
                ) {
                    Text(stringResource(R.string.model_delete))
                }
            } else {
                Button(
                    onClick = onDownload,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Baixar o modelo selecionado para este aparelho" },
                ) {
                    Text(stringResource(R.string.model_download))
                }
            }
        }

        state.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ModelOption(
    model: LocalModel,
    recommended: Boolean,
    selected: Boolean,
    installed: Boolean,
    onSelect: () -> Unit,
) {
    val description = stringResource(R.string.model_selected_description, model.displayName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column {
            Text(
                text = buildString {
                    append(model.displayName)
                    if (recommended) append(" · ").append(stringResource(R.string.model_option_recommended))
                    if (installed) append(" · ").append(stringResource(R.string.model_installed))
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    R.string.model_option_size,
                    model.totalDownloadBytes / 1_048_576L,
                    model.estimatedRamMb,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun noFitMessage(reason: NoFitReason): Int = when (reason) {
    NoFitReason.UNSUPPORTED_ABI -> R.string.model_none_fits_abi
    NoFitReason.INSUFFICIENT_RAM -> R.string.model_none_fits_ram
    NoFitReason.INSUFFICIENT_STORAGE -> R.string.model_none_fits_storage
}
