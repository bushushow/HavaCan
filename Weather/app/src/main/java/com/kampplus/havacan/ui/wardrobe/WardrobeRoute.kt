package com.kampplus.havacan.ui.wardrobe

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.havacan.ui.home.ShakeEffect

enum class WardrobeDestination { Closet, Editor, Outfit }

/**
 * The Dolabım tab. [outfitRequest] increments when the home screen's Salla Kombin card is tapped
 * or the phone is shaken there; it opens the outfit and builds a new one, exactly like a shake here.
 */
@Composable
fun WardrobeRoute(
    clock: () -> Float,
    outfitRequest: Int,
    onShowBottomBar: (Boolean) -> Unit,
    viewModel: WardrobeViewModel = viewModel(factory = WardrobeViewModel.Factory)
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val total by viewModel.allItemCount.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val outfit by viewModel.outfit.collectAsStateWithLifecycle()
    val weather by viewModel.weatherContext.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(WardrobeDestination.Closet) }
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val showOutfit = {
        viewModel.generateOutfit()
        destination = WardrobeDestination.Outfit
    }
    LaunchedEffect(outfitRequest) {
        if (outfitRequest > 0) {
            viewModel.refreshWeather()
            showOutfit()
        }
    }
    LaunchedEffect(destination) { onShowBottomBar(destination == WardrobeDestination.Closet) }
    // Shaking anywhere in the wardrobe (but not while filling the form) makes an outfit
    ShakeEffect(enabled = destination != WardrobeDestination.Editor, onShake = showOutfit)

    BackHandler(enabled = destination != WardrobeDestination.Closet) {
        if (destination == WardrobeDestination.Editor) viewModel.cancelEditor()
        destination = WardrobeDestination.Closet
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val uri = cameraUri
        if (taken && uri != null) viewModel.onPhotoPicked(uri)
    }
    val pickPhoto = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val takePhoto = {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", viewModel.cameraFile())
        cameraUri = uri
        try {
            camera.launch(uri)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Kamera uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            val forward = targetState != WardrobeDestination.Closet
            (slideInHorizontally(tween(320)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(250))) togetherWith
                (slideOutHorizontally(tween(320)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(200)))
        },
        label = "wardrobe"
    ) { dest ->
        when (dest) {
            WardrobeDestination.Closet -> WardrobeScreen(
                state = state,
                totalCount = total,
                clock = clock,
                onAdd = {
                    viewModel.startNew()
                    destination = WardrobeDestination.Editor
                },
                onOpenItem = {
                    viewModel.startEdit(it)
                    destination = WardrobeDestination.Editor
                },
                onFilter = viewModel::setFilter,
                onMakeOutfit = showOutfit,
                onDeleteOutfit = viewModel::deleteSavedOutfit
            )
            WardrobeDestination.Editor -> ClothingEditorScreen(
                state = editor,
                aiConfigured = viewModel.aiConfigured,
                clock = clock,
                onTakePhoto = takePhoto,
                onPickPhoto = pickPhoto,
                onName = viewModel::setName,
                onCategory = viewModel::setCategory,
                onColor = viewModel::setColor,
                onToggleSeason = viewModel::toggleSeason,
                onSave = { viewModel.saveEditor { destination = WardrobeDestination.Closet } },
                onDelete = { viewModel.deleteEditing { destination = WardrobeDestination.Closet } },
                onBack = {
                    viewModel.cancelEditor()
                    destination = WardrobeDestination.Closet
                }
            )
            WardrobeDestination.Outfit -> OutfitScreen(
                outfit = outfit,
                weather = weather,
                clock = clock,
                onShakeAgain = viewModel::generateOutfit,
                onSave = viewModel::saveOutfit,
                onAddCategory = { category ->
                    viewModel.startNew(category)
                    destination = WardrobeDestination.Editor
                },
                onBack = { destination = WardrobeDestination.Closet }
            )
        }
    }
}
