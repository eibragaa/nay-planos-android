package br.com.eibragaa.nayplanos.ui.screens

import android.net.Uri
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.eibragaa.nayplanos.R
import br.com.eibragaa.nayplanos.domain.editor.WeeklyPlanEditorState
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NewPlanScreen(
    state: WeeklyPlanEditorState,
    thumbnail: ImageBitmap?,
    onSelectDay: (index: Int) -> Unit,
    onTextChange: (text: String) -> Unit,
    onPickImage: (sourceReference: String) -> Unit,
    onPrepareCameraCapture: () -> Uri?,
    onCleanupCameraCapture: (Uri) -> Unit,
    onCameraCaptureResult: (sourceReference: String, captured: Boolean) -> Unit,
    onCameraError: (unavailable: Boolean) -> Unit,
    onRemoveImageReference: () -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    onExport: () -> Unit,
    canExport: Boolean,
    modifier: Modifier = Modifier,
) {
    val weekFormatter = remember {
        DateTimeFormatter.ofPattern("dd/MM", Locale("pt", "BR"))
    }
    val selectedDay = state.selectedDay
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let { onPickImage(it.toString()) } },
    )
    val chooseImageDescription = stringResource(R.string.new_plan_choose_image_description)
    val chooseImageLabel = stringResource(R.string.new_plan_choose_image)
    val removeImageDescription = stringResource(R.string.new_plan_remove_image_description)
    val cameraLabel = stringResource(R.string.new_plan_take_photo)
    val cameraDescription = stringResource(R.string.new_plan_take_photo_description)
    var pendingCaptureUri by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { captured ->
            val source = pendingCaptureUri
            pendingCaptureUri = null
            if (source != null) {
                onCameraCaptureResult(source, captured)
                if (!captured) runCatching { onCleanupCameraCapture(Uri.parse(source)) }
            }
        },
    )

    fun launchImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    fun launchCamera() {
        val outputUri = try {
            onPrepareCameraCapture()
        } catch (_: Exception) {
            null
        }
        if (outputUri == null) {
            onCameraError(true)
            return
        }
        pendingCaptureUri = outputUri.toString()
        try {
            cameraLauncher.launch(outputUri)
        } catch (_: ActivityNotFoundException) {
            pendingCaptureUri = null
            runCatching { onCleanupCameraCapture(outputUri) }
            onCameraError(true)
        } catch (_: Exception) {
            pendingCaptureUri = null
            runCatching { onCleanupCameraCapture(outputUri) }
            onCameraError(false)
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.new_plan_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(
                        R.string.new_plan_week_range,
                        state.days.first().date.format(weekFormatter),
                        state.days.last().date.format(weekFormatter),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.new_plan_days_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.days.forEachIndexed { index, day ->
                        val dayName = stringResource(day.dayOfWeek.toLabelResource())
                        val dateLabel = day.date.format(weekFormatter)
                        FilterChip(
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    contentDescription = "$dayName, $dateLabel"
                                },
                            selected = index == state.selectedDayIndex,
                            onClick = { onSelectDay(index) },
                            label = {
                                Column {
                                    Text(
                                        text = dayName.take(3),
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                    )
                                    Text(text = dateLabel, style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            enabled = !day.isNoClass,
                        )
                    }
                }
                if (selectedDay.isNoClass) {
                    Text(
                        text = stringResource(R.string.new_plan_no_class_status),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = selectedDay.activity.text,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.new_plan_activity_label)) },
                supportingText = { Text(stringResource(R.string.new_plan_activity_supporting)) },
                minLines = 4,
                maxLines = 8,
                enabled = !selectedDay.isNoClass,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text,
                ),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.new_plan_image_section),
                    style = MaterialTheme.typography.titleMedium,
                )
                val imageReference = selectedDay.activity.imageReference
                val imageStatusDescription = if (imageReference.isNullOrBlank()) {
                    stringResource(R.string.new_plan_image_status_label) + ": " +
                        stringResource(R.string.new_plan_image_not_attached)
                } else {
                    stringResource(R.string.new_plan_image_status_label) + ": " +
                        stringResource(R.string.new_plan_image_attached)
                }
                Text(
                    modifier = Modifier.semantics {
                        contentDescription = imageStatusDescription
                    },
                    text = if (imageReference.isNullOrBlank()) {
                        stringResource(R.string.new_plan_image_not_attached)
                    } else {
                        stringResource(R.string.new_plan_image_attached)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = stringResource(R.string.new_plan_image_preview_description),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                    )
                }
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = chooseImageDescription
                        },
                    onClick = { launchImagePicker() },
                    enabled = !selectedDay.isNoClass,
                ) {
                    Text(chooseImageLabel)
                }
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = cameraDescription },
                    onClick = { launchCamera() },
                    enabled = !selectedDay.isNoClass,
                ) {
                    Text(cameraLabel)
                }
                if (imageReference != null) {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = removeImageDescription
                            },
                        onClick = onRemoveImageReference,
                    ) {
                        Text(stringResource(R.string.new_plan_remove_image))
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSave,
                enabled = canSave,
            ) {
                Text(stringResource(R.string.new_plan_save))
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onExport,
                enabled = canExport,
            ) {
                Text(stringResource(R.string.new_plan_export_docx))
            }
        }
    }
}

private fun java.time.DayOfWeek.toLabelResource(): Int = when (this) {
    java.time.DayOfWeek.MONDAY -> R.string.day_monday
    java.time.DayOfWeek.TUESDAY -> R.string.day_tuesday
    java.time.DayOfWeek.WEDNESDAY -> R.string.day_wednesday
    java.time.DayOfWeek.THURSDAY -> R.string.day_thursday
    java.time.DayOfWeek.FRIDAY -> R.string.day_friday
    java.time.DayOfWeek.SATURDAY -> R.string.day_saturday
    java.time.DayOfWeek.SUNDAY -> R.string.day_sunday
}
