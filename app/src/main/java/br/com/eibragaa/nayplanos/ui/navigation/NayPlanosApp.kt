package br.com.eibragaa.nayplanos.ui.navigation

import android.net.Uri
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import br.com.eibragaa.nayplanos.R
import br.com.eibragaa.nayplanos.domain.editor.WeeklyPlanEditorReducer
import br.com.eibragaa.nayplanos.domain.editor.WeeklyPlanEditorState
import br.com.eibragaa.nayplanos.domain.document.DocumentImage
import br.com.eibragaa.nayplanos.domain.document.PlanDocumentGenerator
import br.com.eibragaa.nayplanos.domain.image.ImageStore
import br.com.eibragaa.nayplanos.domain.model.PlanDay
import br.com.eibragaa.nayplanos.domain.model.WeeklyPlan
import br.com.eibragaa.nayplanos.domain.repository.WeeklyPlanRepository
import br.com.eibragaa.nayplanos.domain.validation.WeeklyPlanValidator
import br.com.eibragaa.nayplanos.ui.screens.HomeScreen
import br.com.eibragaa.nayplanos.ui.screens.HistoryScreen
import br.com.eibragaa.nayplanos.ui.screens.NewPlanScreen
import br.com.eibragaa.nayplanos.ui.screens.PlaceholderScreen
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NayPlanosApp(
    repository: WeeklyPlanRepository,
    imageStore: ImageStore,
    documentGenerator: PlanDocumentGenerator,
    onShareDocument: suspend (fileName: String, bytes: ByteArray) -> Unit,
    onPrepareCameraCapture: () -> Uri?,
    onCleanupCameraCapture: (Uri) -> Unit,
) {
    var selectedDestinationName by rememberSaveable { mutableStateOf(AppDestination.HOME.name) }
    val today = remember { LocalDate.now() }
    val monday = remember(today) { today.with(DayOfWeek.MONDAY) }
    var editorState by remember(monday) {
        mutableStateOf(
            WeeklyPlanEditorState(
                days = (0L until WeeklyPlanEditorState.WEEK_LENGTH.toLong()).map { offset ->
                    PlanDay(date = monday.plusDays(offset))
                },
            ),
        )
    }
    var isSaving by remember { mutableStateOf(false) }
    var isImageOperationInProgress by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var pendingCameraTargetDate by rememberSaveable { mutableStateOf<String?>(null) }
    var loadedThumbnail by remember { mutableStateOf<Pair<String, ImageBitmap>?>(null) }
    var hasSavedCurrentWeek by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val plansFlow = remember(repository) { repository.observeAll() }
    val savedPlans by plansFlow.collectAsState(initial = emptyList())
    val saveSuccessMessage = stringResource(R.string.new_plan_save_success)
    val saveErrorMessage = stringResource(R.string.new_plan_save_error)
    val importImageErrorMessage = stringResource(R.string.new_plan_image_import_error)
    val cameraUnavailableMessage = stringResource(R.string.new_plan_camera_unavailable)
    val cameraCaptureErrorMessage = stringResource(R.string.new_plan_camera_capture_error)
    val removeImageErrorMessage = stringResource(R.string.new_plan_image_remove_error)
    val exportSuccessMessage = stringResource(R.string.new_plan_export_success)
    val exportErrorMessage = stringResource(R.string.new_plan_export_error)
    val selectedDestination = AppDestination.entries.firstOrNull {
        it.name == selectedDestinationName
    } ?: AppDestination.HOME
    val selectedImageReference = editorState.selectedDay.activity.imageReference
    val selectedDayThumbnail = loadedThumbnail
        ?.takeIf { it.first == selectedImageReference }
        ?.second

    LaunchedEffect(selectedImageReference, imageStore) {
        loadedThumbnail = null
        if (!selectedImageReference.isNullOrBlank()) {
            val bitmap = try {
                withContext(Dispatchers.IO) {
                    val thumbnail = imageStore.readThumbnail(selectedImageReference)
                        ?: return@withContext null
                    BitmapFactory.decodeByteArray(
                        thumbnail.jpegBytes,
                        0,
                        thumbnail.jpegBytes.size,
                    )?.asImageBitmap()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
            if (bitmap != null) {
                loadedThumbnail = selectedImageReference to bitmap
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                AppDestination.entries.forEach { destination ->
                    val label = stringResource(destination.labelRes)
                    NavigationBarItem(
                        selected = selectedDestination == destination,
                        onClick = { selectedDestinationName = destination.name },
                        icon = { DestinationGlyph(destination = destination) },
                        label = { Text(text = label) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when (selectedDestination) {
                AppDestination.HOME -> HomeScreen()
                AppDestination.NEW_PLAN -> NewPlanScreen(
                    state = editorState,
                    thumbnail = selectedDayThumbnail,
                    canSave = !isSaving && !isImageOperationInProgress && !hasSavedCurrentWeek &&
                        !isExporting &&
                        WeeklyPlanValidator.validate(
                            WeeklyPlan(
                                weekStart = editorState.days.first().date,
                                days = editorState.days,
                            ),
                        ).isEmpty(),
                    onSelectDay = { index ->
                        editorState = WeeklyPlanEditorReducer.selectDay(editorState, index)
                    },
                    onTextChange = { text ->
                        editorState = WeeklyPlanEditorReducer.updateText(editorState, text)
                    },
                    onPickImage = { sourceReference ->
                        val stateAtImportStart = editorState
                        val targetDayIndex = stateAtImportStart.selectedDayIndex
                        val targetDayAtImportStart = stateAtImportStart.days[targetDayIndex]
                        if (!isImageOperationInProgress && !targetDayAtImportStart.isNoClass) {
                            isImageOperationInProgress = true
                            coroutineScope.launch {
                                try {
                                    val storedImage = imageStore.importImage(sourceReference)
                                    val currentState = editorState
                                    val currentTargetIndex = currentState.days.indexOfFirst {
                                        it.date == targetDayAtImportStart.date
                                    }
                                    val targetStillCompatible = currentTargetIndex >= 0 &&
                                        currentState.days[currentTargetIndex] == targetDayAtImportStart &&
                                        !currentState.days[currentTargetIndex].isNoClass
                                    if (targetStillCompatible) {
                                        val updatedDays = currentState.days.toMutableList()
                                        val currentTarget = updatedDays[currentTargetIndex]
                                        val previousImageReference =
                                            currentTarget.activity.imageReference
                                        updatedDays[currentTargetIndex] = currentTarget.copy(
                                            activity = currentTarget.activity.copy(
                                                imageReference = storedImage.reference,
                                            ),
                                        )
                                        editorState = currentState.copy(days = updatedDays)

                                        if (previousImageReference != null &&
                                            previousImageReference != storedImage.reference
                                        ) {
                                            try {
                                                imageStore.deleteImage(previousImageReference)
                                            } catch (cancellation: CancellationException) {
                                                throw cancellation
                                            } catch (_: Exception) {
                                                snackbarHostState.showSnackbar(
                                                    "$removeImageErrorMessage A nova imagem foi aplicada; a anterior pode continuar ocupando espaço.",
                                                )
                                            }
                                        }
                                    } else {
                                        withContext(NonCancellable) {
                                            imageStore.deleteImage(storedImage.reference)
                                        }
                                    }
                                } catch (cancellation: CancellationException) {
                                    throw cancellation
                                } catch (_: Exception) {
                                    snackbarHostState.showSnackbar(importImageErrorMessage)
                                } finally {
                                    isImageOperationInProgress = false
                                }
                            }
                        }
                    },
                    onPrepareCameraCapture = {
                        pendingCameraTargetDate = editorState.selectedDay.date.toString()
                        onPrepareCameraCapture()
                    },
                    onCleanupCameraCapture = onCleanupCameraCapture,
                    onCameraCaptureResult = { sourceReference, captured ->
                        if (!captured) {
                            pendingCameraTargetDate = null
                            return@NewPlanScreen
                        }
                        val targetDate = pendingCameraTargetDate
                            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        pendingCameraTargetDate = null
                        val currentStateAtResult = editorState
                        val targetDay = targetDate?.let { date ->
                            currentStateAtResult.days.firstOrNull { it.date == date }
                        }
                        if (targetDay == null) {
                            runCatching { onCleanupCameraCapture(Uri.parse(sourceReference)) }
                            return@NewPlanScreen
                        }
                        if (isImageOperationInProgress || targetDay.isNoClass) {
                            runCatching { onCleanupCameraCapture(Uri.parse(sourceReference)) }
                            return@NewPlanScreen
                        }
                        isImageOperationInProgress = true
                        coroutineScope.launch {
                            val captureUri = Uri.parse(sourceReference)
                            var importedReference: String? = null
                            var appliedToEditor = false
                            try {
                                val storedImage = imageStore.importImage(sourceReference)
                                importedReference = storedImage.reference
                                val currentState = editorState
                                val targetIndex = currentState.days.indexOfFirst { it.date == targetDay.date }
                                if (targetIndex >= 0 && currentState.days[targetIndex] == targetDay &&
                                    !currentState.days[targetIndex].isNoClass
                                ) {
                                    val updatedDays = currentState.days.toMutableList()
                                    val oldReference = updatedDays[targetIndex].activity.imageReference
                                    updatedDays[targetIndex] = updatedDays[targetIndex].copy(
                                        activity = updatedDays[targetIndex].activity.copy(
                                            imageReference = storedImage.reference,
                                        ),
                                    )
                                    editorState = currentState.copy(days = updatedDays)
                                    appliedToEditor = true
                                    if (oldReference != null && oldReference != storedImage.reference) {
                                        runCatching { imageStore.deleteImage(oldReference) }
                                    }
                                } else {
                                    withContext(NonCancellable) { imageStore.deleteImage(storedImage.reference) }
                                }
                            } catch (cancellation: CancellationException) {
                                if (!appliedToEditor) {
                                    withContext(NonCancellable) {
                                        importedReference?.let {
                                            try {
                                                imageStore.deleteImage(it)
                                            } catch (_: Exception) {
                                                // Best effort: the next image-store cleanup can retry it.
                                            }
                                        }
                                    }
                                }
                                throw cancellation
                            } catch (_: Exception) {
                                snackbarHostState.showSnackbar(importImageErrorMessage)
                            } finally {
                                withContext(NonCancellable) {
                                    runCatching { onCleanupCameraCapture(captureUri) }
                                }
                                isImageOperationInProgress = false
                            }
                        }
                    },
                    onCameraError = { unavailable ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                if (unavailable) cameraUnavailableMessage else cameraCaptureErrorMessage,
                            )
                        }
                    },
                    onRemoveImageReference = {
                        val stateAtRemovalStart = editorState
                        val targetDay = stateAtRemovalStart.selectedDay
                        val imageReference = targetDay.activity.imageReference
                        if (!isImageOperationInProgress && imageReference != null) {
                            isImageOperationInProgress = true
                            coroutineScope.launch {
                                try {
                                    imageStore.deleteImage(imageReference)
                                    val currentState = editorState
                                    val currentTargetIndex = currentState.days.indexOfFirst {
                                        it.date == targetDay.date
                                    }
                                    if (currentTargetIndex >= 0 && currentState.days[currentTargetIndex]
                                            .activity.imageReference == imageReference
                                    ) {
                                        val updatedDays = currentState.days.toMutableList()
                                        val currentTarget = updatedDays[currentTargetIndex]
                                        updatedDays[currentTargetIndex] = currentTarget.copy(
                                            activity = currentTarget.activity.copy(imageReference = null),
                                        )
                                        editorState = currentState.copy(days = updatedDays)
                                    }
                                } catch (cancellation: CancellationException) {
                                    throw cancellation
                                } catch (_: Exception) {
                                    snackbarHostState.showSnackbar(removeImageErrorMessage)
                                } finally {
                                    isImageOperationInProgress = false
                                }
                            }
                        }
                    },
                    onSave = {
                        if (!isSaving && !isImageOperationInProgress && !isExporting && !hasSavedCurrentWeek &&
                            WeeklyPlanValidator.validate(
                                WeeklyPlan(
                                    weekStart = editorState.days.first().date,
                                    days = editorState.days,
                                ),
                            ).isEmpty()
                        ) {
                            val plan = WeeklyPlan(
                                weekStart = editorState.days.first().date,
                                days = editorState.days.toList(),
                            )
                            isSaving = true
                            coroutineScope.launch {
                                try {
                                    repository.save(plan)
                                    hasSavedCurrentWeek = true
                                    selectedDestinationName = AppDestination.HOME.name
                                    snackbarHostState.showSnackbar(saveSuccessMessage)
                                } catch (cancellation: CancellationException) {
                                    throw cancellation
                                } catch (_: Exception) {
                                    snackbarHostState.showSnackbar(saveErrorMessage)
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    },
                    canExport = !isSaving && !isImageOperationInProgress && !isExporting &&
                        WeeklyPlanValidator.validate(
                            WeeklyPlan(
                                weekStart = editorState.days.first().date,
                                days = editorState.days,
                            ),
                        ).isEmpty(),
                    onExport = {
                        if (!isSaving && !isImageOperationInProgress && !isExporting) {
                            val plan = WeeklyPlan(
                                weekStart = editorState.days.first().date,
                                days = editorState.days.toList(),
                            )
                            if (WeeklyPlanValidator.validate(plan).isEmpty()) {
                                isExporting = true
                                coroutineScope.launch {
                                    try {
                                        val bytes = withContext(Dispatchers.IO) {
                                            val imagesByReference = plan.days.mapNotNull { day ->
                                                val reference = day.activity.imageReference
                                                    ?: return@mapNotNull null
                                                val thumbnail = imageStore.readThumbnail(reference)
                                                    ?: return@mapNotNull null
                                                reference to DocumentImage(
                                                    jpegBytes = thumbnail.jpegBytes,
                                                    width = thumbnail.width,
                                                    height = thumbnail.height,
                                                )
                                            }.toMap()
                                            documentGenerator.generate(plan, imagesByReference)
                                        }
                                        val fileName = "plano-${plan.weekStart}"
                                        onShareDocument(fileName, bytes)
                                        snackbarHostState.showSnackbar(exportSuccessMessage)
                                    } catch (cancellation: CancellationException) {
                                        throw cancellation
                                    } catch (_: Exception) {
                                        snackbarHostState.showSnackbar(exportErrorMessage)
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            }
                        }
                    },
                )
                AppDestination.HISTORY -> HistoryScreen(plans = savedPlans)
                AppDestination.SETTINGS -> PlaceholderScreen(
                    title = stringResource(selectedDestination.titleRes),
                    description = stringResource(selectedDestination.descriptionRes),
                )
            }
        }
    }
}

@Composable
private fun DestinationGlyph(destination: AppDestination) {
    val glyph = when (destination) {
        AppDestination.HOME -> "⌂"
        AppDestination.NEW_PLAN -> "+"
        AppDestination.HISTORY -> "▤"
        AppDestination.SETTINGS -> "⚙"
    }
    Text(
        text = glyph,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
