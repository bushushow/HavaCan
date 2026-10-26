package com.kampplus.havacan.ui.city

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.havacan.R
import com.kampplus.havacan.data.PlaceItem
import com.kampplus.havacan.domain.voice.TurkishText
import com.kampplus.havacan.model.City
import com.kampplus.havacan.ui.home.character.HavaCanArt
import com.kampplus.havacan.ui.home.character.HavaCanPoseStyle
import com.kampplus.havacan.ui.theme.HavaBlue
import com.kampplus.havacan.ui.theme.HavaBlueLight
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.NavySoft
import com.kampplus.havacan.ui.theme.RiskRed
import com.kampplus.havacan.ui.theme.SkyTint
import kotlinx.coroutines.delay

private val CardShape = RoundedCornerShape(28.dp)

/**
 * Konum seç and an il's ilçe page. Used for the home city and for a plan's or run's location:
 * [onCityClick] decides what the pick is for. Search runs on the phone over all 81 provinces and
 * 973 districts; only places abroad go to Open-Meteo.
 */
@Composable
fun CityRoute(
    onCityClick: (City) -> Unit,
    onBack: () -> Unit,
    clock: () -> Float,
    reduceMotion: Boolean,
    viewModel: LocationPickerViewModel = viewModel(factory = LocationPickerViewModel.Factory)
) {
    val context = LocalContext.current
    val view = LocalView.current
    val search by viewModel.search.collectAsStateWithLifecycle()
    val recents by viewModel.recentPlaces.collectAsStateWithLifecycle()
    val plate by viewModel.plate.collectAsStateWithLifecycle()
    val page by viewModel.districtPage.collectAsStateWithLifecycle()
    val locate by viewModel.locate.collectAsStateWithLifecycle()
    var success by remember { mutableStateOf<PlaceItem?>(null) }
    val pick by rememberUpdatedState(onCityClick)

    BackHandler(enabled = plate != null) { viewModel.closeProvince() }

    // A pick: confirm it (seen and heard), then hand it over and reset for next time
    LaunchedEffect(viewModel) {
        viewModel.picked.collect { item ->
            success = item
            @Suppress("DEPRECATION")
            view.announceForAccessibility("${item.shortLabel} seçildi")
            if (!reduceMotion) delay(700)
            pick(item.city)
            success = null
            viewModel.closeProvince()
            viewModel.onQueryChange("")
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) viewModel.useCurrentLocation() else viewModel.onLocationPermissionDenied()
    }
    val useLocation = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            viewModel.useCurrentLocation()
        } else {
            permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
    val openSettings = {
        val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val intent = if (granted) {
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        }
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        viewModel.dismissLocateError()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = page,
            contentKey = { it?.province?.city?.id },
            transitionSpec = {
                when {
                    reduceMotion -> EnterTransition.None togetherWith ExitTransition.None
                    targetState != null -> (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut())
                    else -> (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
                }
            },
            label = "pickerPage"
        ) { current ->
            if (current == null) {
                MainPage(
                    query = viewModel.query,
                    onQuery = viewModel::onQueryChange,
                    search = search,
                    recents = recents,
                    provinces = viewModel.provinces,
                    locate = locate,
                    clock = clock,
                    onBack = onBack,
                    onUseLocation = useLocation,
                    onOpenSettings = openSettings,
                    onDismissLocate = viewModel::dismissLocateError,
                    onProvince = viewModel::openProvince,
                    onPick = viewModel::pick,
                    onRecent = viewModel::pickRecent,
                    onRetry = viewModel::retry
                )
            } else {
                DistrictsPage(
                    page = current,
                    query = viewModel.districtQuery,
                    onQuery = viewModel::onDistrictQueryChange,
                    clock = clock,
                    onBack = viewModel::closeProvince,
                    onPick = viewModel::pick
                )
            }
        }
        AnimatedVisibility(
            visible = success != null,
            enter = if (reduceMotion) fadeIn() else scaleIn(spring(dampingRatio = 0.55f)) + fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            success?.let { SuccessBadge(it) }
        }
    }
}

// region Pages

@Composable
private fun MainPage(
    query: String,
    onQuery: (String) -> Unit,
    search: SearchState,
    recents: List<City>,
    provinces: List<PlaceItem>,
    locate: LocateState,
    clock: () -> Float,
    onBack: () -> Unit,
    onUseLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissLocate: () -> Unit,
    onProvince: (PlaceItem) -> Unit,
    onPick: (PlaceItem) -> Unit,
    onRecent: (City) -> Unit,
    onRetry: () -> Unit
) {
    PickerList {
        item {
            PickerHeader("Konum seç", clock, onBack, "Geri dön") {
                Text("HavaCan neresi için baksın?", style = MaterialTheme.typography.titleMedium, color = Navy)
            }
        }
        item { SearchField(query, onQuery, "Şehir veya ilçe ara…") }
        if (query.isBlank()) {
            item {
                ActionCard(
                    title = "Mevcut konumumu kullan",
                    subtitle = if (locate == LocateState.Locating) "Konumun bulunuyor…" else "Konum izniyle otomatik bul",
                    busy = locate == LocateState.Locating,
                    onClick = onUseLocation
                )
            }
            if (locate is LocateState.Failed) item { LocateError(locate, onOpenSettings, onDismissLocate) }
            if (recents.isNotEmpty()) {
                item { SectionTitle("Son konumlar") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(recents, key = { it.id }) { city -> RecentChip(city) { onRecent(city) } }
                    }
                }
            }
            item { SectionTitle("Şehirler", trailing = "${provinces.size} il") }
            placeRows(provinces, chevron = true, onClick = onProvince)
            item { Footer("Şehir seçerek ilçelerini görebilirsin.") }
        } else {
            when (search) {
                SearchState.Idle -> Unit
                SearchState.Loading -> item {
                    StatusCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = HavaBlue, strokeWidth = 3.dp)
                            Text("Aranıyor…", style = MaterialTheme.typography.titleMedium, color = Navy)
                        }
                    }
                }
                is SearchState.Empty -> item {
                    EmptyCard("“${search.query}” için sonuç yok", "İl ya da ilçe adını kontrol et; Türkçe harf yazmasan da bulurum.")
                }
                is SearchState.Error -> item {
                    StatusCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                when (search.reason) {
                                    ErrorReason.NoConnection -> "İnternet bağlantısı yok"
                                    ErrorReason.Server -> "Arama hizmetine ulaşılamadı"
                                    ErrorReason.Unknown -> "Beklenmeyen bir sorun oldu"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = Navy,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Türkiye'deki il ve ilçeler internetsiz bulunur; bu arama yurt dışı için internete ihtiyaç duyuyor.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NavySoft
                            )
                            TextButton(onClick = onRetry) { Text("Tekrar dene") }
                        }
                    }
                }
                is SearchState.Results -> {
                    item { SectionTitle(if (search.online) "Dünyadan sonuçlar" else "Sonuçlar", trailing = "${search.items.size}") }
                    // A province opens its districts; a district or a place abroad is picked right away
                    placeRows(search.items, chevron = true) { item ->
                        if (item.kind ==
                            PlaceItem.Kind.Province
                        ) {
                            onProvince(item)
                        } else {
                            onPick(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DistrictsPage(
    page: DistrictPage,
    query: String,
    onQuery: (String) -> Unit,
    clock: () -> Float,
    onBack: () -> Unit,
    onPick: (PlaceItem) -> Unit
) {
    val name = page.province.city.name
    PickerList {
        item {
            PickerHeader("$name ilçeleri", clock, onBack, "Şehirlere dön") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Konum: Türkiye, $name" }
                ) {
                    Text("Türkiye", style = MaterialTheme.typography.titleMedium, color = HavaBlue)
                    Icon(
                        painterResource(R.drawable.ic_chevron_right),
                        null,
                        tint = HavaBlue,
                        modifier = Modifier.padding(horizontal = 4.dp).size(20.dp)
                    )
                    Text(name, style = MaterialTheme.typography.titleMedium, color = HavaBlue)
                }
            }
        }
        item { SearchField(query, onQuery, "${TurkishText.locative(name)} ilçe ara…") }
        item { ActionCard("$name genelini seç", "İl merkezi için hava durumu", busy = false) { onPick(page.province) } }
        item {
            Column(Modifier.padding(top = 8.dp)) {
                SectionTitle("İlçeler", trailing = "${page.districts.size}")
                Text("İlçe seçerek devam et", style = MaterialTheme.typography.bodyLarge, color = NavySoft)
            }
        }
        if (page.districts.isEmpty()) {
            item { EmptyCard("“${query.trim()}” adında ilçe yok", "$name ilçelerinde bulunamadı. Başka bir yazımla dene.") }
        } else {
            placeRows(page.districts, chevron = true, showSubtitle = false, onClick = onPick)
        }
        item { Footer("Bir ilçe seçtiğinde hava durumu güncellenir.") }
    }
}

// endregion

// region Building blocks

@Composable
private fun PickerList(content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        content()
        item { Spacer(Modifier.navigationBarsPadding().height(8.dp)) }
    }
}

@Composable
private fun PickerHeader(title: String, clock: () -> Float, onBack: () -> Unit, backLabel: String, subtitle: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.size(56.dp).semantics { contentDescription = backLabel }
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_arrow_back), null, tint = Navy) }
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = Navy,
                fontWeight = FontWeight.Black,
                modifier = Modifier.semantics {
                    heading()
                }
            )
            subtitle()
        }
        Box(Modifier.size(104.dp).clearAndSetSemantics { }) {
            HavaCanArt(HavaCanPoseStyle.Cheer, clock, Modifier.fillMaxSize())
            // A map pin HavaCan holds up, drawn so it scales with the character
            Canvas(Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 10.dp).size(34.dp, 44.dp)) {
                val w = size.width
                val h = size.height
                val pin = Path().apply {
                    moveTo(w / 2, h)
                    cubicTo(w * 0.1f, h * 0.58f, 0f, h * 0.42f, 0f, w / 2)
                    cubicTo(0f, w * 0.22f, w * 0.22f, 0f, w / 2, 0f)
                    cubicTo(w * 0.78f, 0f, w, w * 0.22f, w, w / 2)
                    cubicTo(w, h * 0.42f, w * 0.9f, h * 0.58f, w / 2, h)
                    close()
                }
                drawPath(pin, Brush.verticalGradient(listOf(Color(0xFF4B8BFF), HavaBlue)))
                drawCircle(Color.White, w * 0.17f, Offset(w / 2, w / 2))
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border by animateColorAsState(if (focused) HavaBlue else Color.Transparent, label = "searchBorder")
    val elevation by animateDpAsState(if (focused) 10.dp else 5.dp, label = "searchShadow")
    val iconScale by animateFloatAsState(if (focused) 1.12f else 1f, label = "searchIcon")
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(32.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interaction,
        textStyle = MaterialTheme.typography.titleMedium.merge(TextStyle(color = Navy)),
        cursorBrush = SolidColor(HavaBlue),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(elevation, shape, ambientColor = Navy.copy(alpha = 0.1f), spotColor = Navy.copy(alpha = 0.1f))
            .background(Color.White, shape)
            .border(BorderStroke(1.5.dp, border), shape)
            .semantics { contentDescription = placeholder },
        decorationBox = { inner ->
            Row(Modifier.heightIn(min = 68.dp).padding(start = 22.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_search), null, tint = HavaBlue, modifier = Modifier.size(30.dp).scale(iconScale))
                Box(Modifier.weight(1f).padding(start = 16.dp)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.titleMedium,
                            color = NavySoft.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    inner()
                }
                if (value.isNotEmpty()) {
                    IconButton(onClick = {
                        onValueChange("")
                    }) { Icon(painterResource(R.drawable.ic_clear), "Aramayı temizle", tint = NavySoft) }
                }
            }
        }
    )
}

@Composable
private fun ActionCard(title: String, subtitle: String, busy: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "actionPress")
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .scale(scale)
            .shadow(6.dp, CardShape, ambientColor = HavaBlue.copy(alpha = 0.15f), spotColor = HavaBlue.copy(alpha = 0.15f))
            .background(Brush.linearGradient(listOf(HavaBlueLight, SkyTint, Color(0xFFF5F9FF))), CardShape)
            .border(1.dp, Color.White, CardShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, enabled = !busy, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$title. $subtitle" }
    ) {
        Row(
            Modifier.padding(16.dp).heightIn(min = 72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(Modifier.size(72.dp).background(Color.White.copy(alpha = 0.7f), CircleShape), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(40.dp)) {
                    val r = size.minDimension / 2
                    val c = Offset(r, r)
                    drawCircle(HavaBlue, r * 0.62f, c, style = Stroke(width = r * 0.16f))
                    drawCircle(HavaBlue, r * 0.32f, c)
                    listOf(Offset(0f, -1f), Offset(0f, 1f), Offset(-1f, 0f), Offset(1f, 0f)).forEach { d ->
                        drawLine(HavaBlue, c + d * (r * 0.62f), c + d * r, strokeWidth = r * 0.16f)
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Navy, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = NavySoft)
            }
            Box(Modifier.size(52.dp).background(HavaBlue, CircleShape), contentAlignment = Alignment.Center) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(26.dp), color = Color.White, strokeWidth = 3.dp)
                } else {
                    Icon(painterResource(R.drawable.ic_chevron_right), null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        }
    }
}

@Composable
private fun LocateError(state: LocateState.Failed, onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    StatusCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                state.message,
                style = MaterialTheme.typography.bodyLarge,
                color = RiskRed,
                modifier = Modifier.semantics {
                    liveRegion =
                        LiveRegionMode.Polite
                }
            )
            Row {
                if (state.openSettings) TextButton(onClick = onOpenSettings) { Text("Ayarları aç") }
                TextButton(onClick = onDismiss) { Text("Tamam") }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
        Text(
            text,
            style = MaterialTheme.typography.headlineSmall,
            color = Navy,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f).semantics {
                heading()
            }
        )
        trailing?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = NavySoft) }
    }
}

@Composable
private fun RecentChip(city: City, onClick: () -> Unit) {
    val label = if (city.admin1 != null && city.admin1 != city.name) "${city.admin1}, ${city.name}" else city.name
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        modifier = Modifier.heightIn(min = 60.dp).widthIn(max = 260.dp).semantics {
            contentDescription =
                "Son konum: $label. Seçmek için dokun."
        }
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(painterResource(R.drawable.ic_place), null, tint = HavaBlue, modifier = Modifier.size(24.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = Navy,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = HavaBlue, modifier = Modifier.size(22.dp))
        }
    }
}

/** Rows that look like one white card: rounded top on the first, bottom on the last, thin dividers between. */
private fun LazyListScope.placeRows(items: List<PlaceItem>, chevron: Boolean, showSubtitle: Boolean = true, onClick: (PlaceItem) -> Unit) {
    itemsIndexed(items, key = { _, it -> "${it.kind}-${it.city.id}" }) { index, item ->
        val first = index == 0
        val last = index == items.lastIndex
        val shape = RoundedCornerShape(
            topStart = if (first) 28.dp else 0.dp,
            topEnd = if (first) 28.dp else 0.dp,
            bottomStart = if (last) 28.dp else 0.dp,
            bottomEnd = if (last) 28.dp else 0.dp
        )
        Column(Modifier.background(Color.White, shape)) {
            PlaceRow(item, chevron, showSubtitle, if (first) 8.dp else 0.dp, if (last) 8.dp else 0.dp) { onClick(item) }
            if (!last) HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Navy.copy(alpha = 0.07f))
        }
    }
}

@Composable
private fun PlaceRow(item: PlaceItem, chevron: Boolean, showSubtitle: Boolean, top: Dp, bottom: Dp, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "rowPress")
    val kindWord = when (item.kind) {
        PlaceItem.Kind.Province -> "il, ilçelerini görmek için dokun"
        PlaceItem.Kind.District -> "seçmek için dokun"
        PlaceItem.Kind.Abroad -> "seçmek için dokun"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = top, bottom = bottom)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                role = Role.Button,
                onClick = onClick
            )
            .scale(scale)
            .heightIn(min = 72.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { contentDescription = "${item.city.name}, ${item.subtitle}, $kindWord" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.size(52.dp).background(HavaBlueLight.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_place), null, tint = HavaBlue, modifier = Modifier.size(28.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(item.city.name, style = MaterialTheme.typography.titleLarge, color = Navy, fontWeight = FontWeight.Bold)
            // On an ilçe page the province is already in the title; elsewhere say what and where it is
            if (showSubtitle &&
                item.subtitle.isNotBlank()
            ) {
                Text(item.subtitle, style = MaterialTheme.typography.bodyLarge, color = NavySoft)
            }
        }
        if (chevron) Icon(painterResource(R.drawable.ic_chevron_right), null, tint = Navy, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun StatusCard(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).background(Color.White, CardShape).padding(20.dp)) { content() }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    StatusCard {
        Column(
            Modifier.semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
            },
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Navy, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = NavySoft)
        }
    }
}

@Composable
private fun Footer(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_info), null, tint = NavySoft, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = NavySoft, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SuccessBadge(item: PlaceItem) {
    Surface(shape = RoundedCornerShape(32.dp), color = Color.White, shadowElevation = 16.dp) {
        Column(
            Modifier.padding(horizontal = 32.dp, vertical = 24.dp).semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(64.dp).background(HavaBlue, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_check), null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Text(item.shortLabel, style = MaterialTheme.typography.titleLarge, color = Navy, fontWeight = FontWeight.ExtraBold)
            Text("seçildi", style = MaterialTheme.typography.bodyLarge, color = NavySoft)
        }
    }
}

// endregion
