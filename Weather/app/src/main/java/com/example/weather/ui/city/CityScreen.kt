package com.example.weather.ui.city

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import com.example.weather.data.Countries
import com.example.weather.data.TURKIYE
import com.example.weather.data.TurkishProvinces
import com.example.weather.data.normalizeForSearch
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weather.R
import com.example.weather.model.City
import com.example.weather.ui.theme.LocalHeaderGradient

/** Screens at least this wide put the search field in the top bar and centre the list. */
private val WideLayoutMinWidth = 600.dp
private val ContentMaxWidth = 720.dp

/** Connects [CityScreen] to [CityViewModel]. */
@Composable
fun CityRoute(
    onCityClick: (City) -> Unit,
    viewModel: CityViewModel = viewModel(factory = CityViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val countryCode by viewModel.countryCode.collectAsStateWithLifecycle()
    val browsingPlate by viewModel.browsingPlate.collectAsStateWithLifecycle()
    var showCountries by rememberSaveable { mutableStateOf(false) }

    // Back from a district list returns to the provinces before it closes the picker
    BackHandler(enabled = browsingPlate != null) { viewModel.onExitDistricts() }

    CityScreen(
        uiState = uiState,
        query = viewModel.query,
        onQueryChange = viewModel::onQueryChange,
        selectedCityId = viewModel.selectedCityId,
        onCityClick = { city ->
            viewModel.onCitySelected(city)
            onCityClick(city)
        },
        onRetry = viewModel::retry,
        countryCode = countryCode,
        onCountryClick = { showCountries = true },
        onBrowseDistricts = { city -> TurkishProvinces.find(city.admin1)?.let { viewModel.onBrowseDistricts(it.plate) } },
        onExitDistricts = viewModel::onExitDistricts,
    )

    if (showCountries) {
        CountryPickerSheet(
            selected = countryCode,
            onSelect = { code ->
                viewModel.onCountrySelected(code)
                showCountries = false
            },
            onDismiss = { showCountries = false },
        )
    }
}

@Composable
fun CityScreen(
    uiState: CityUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedCityId: Long?,
    onCityClick: (City) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    countryCode: String? = TURKIYE,
    onCountryClick: () -> Unit = {},
    /** Shows an "İlçeler" button on Turkish provinces when set. */
    onBrowseDistricts: ((City) -> Unit)? = null,
    onExitDistricts: () -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val wide = maxWidth >= WideLayoutMinWidth
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(query = query, onQueryChange = onQueryChange, wide = wide)

            val bottomInset = WindowInsets.safeDrawing
                .only(WindowInsetsSides.Bottom)
                .asPaddingValues()
                .calculateBottomPadding()
            val listPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomInset + 16.dp)
            Column(
                modifier = Modifier
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding(),
            ) {
                CountryBar(countryCode, onCountryClick)
                when (uiState) {
                    CityUiState.Loading -> LoadingState(contentPadding = listPadding)
                    is CityUiState.Success -> {
                        val districtsOf = uiState.districtsOf
                        SectionHeader(
                            title = when {
                                districtsOf != null -> stringResource(R.string.districts_of, districtsOf)
                                uiState.isSuggestion -> stringResource(
                                    if (countryCode == TURKIYE) R.string.section_provinces else R.string.section_popular,
                                )
                                else -> stringResource(R.string.section_results)
                            },
                            count = uiState.cities.size,
                            onBack = if (districtsOf != null) onExitDistricts else null,
                        )
                        CityList(
                            cities = uiState.cities,
                            selectedCityId = selectedCityId,
                            onCityClick = onCityClick,
                            onBrowseDistricts = if (districtsOf == null) onBrowseDistricts else null,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = listPadding,
                        )
                    }
                    is CityUiState.Prompt -> MessageState(
                        icon = R.drawable.ic_search,
                        title = uiState.countryName?.let { stringResource(R.string.prompt_country, it) }
                            ?: stringResource(R.string.prompt_world),
                        detail = stringResource(R.string.prompt_detail),
                    )
                    is CityUiState.Empty -> MessageState(
                        icon = R.drawable.ic_search,
                        title = stringResource(R.string.city_not_found),
                        detail = stringResource(R.string.city_not_found_detail, uiState.query),
                    )
                    is CityUiState.Error -> MessageState(
                        icon = R.drawable.ic_cloud_off,
                        title = stringResource(R.string.error_title),
                        detail = stringResource(
                            when (uiState.reason) {
                                ErrorReason.NoConnection -> R.string.error_no_connection
                                ErrorReason.Server -> R.string.error_server
                                ErrorReason.Unknown -> R.string.error_unknown
                            },
                        ),
                        action = {
                            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Header(query: String, onQueryChange: (String) -> Unit, wide: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.verticalGradient(LocalHeaderGradient.current)),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(R.string.greeting),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            },
            actions = {
                if (wide) {
                    SearchField(
                        query = query,
                        onQueryChange = onQueryChange,
                        modifier = Modifier
                            .width(420.dp)
                            .padding(end = 16.dp),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
            ),
        )
        if (!wide) {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
            )
        } else {
            Spacer(Modifier.size(12.dp))
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_label)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_clear),
                        contentDescription = stringResource(R.string.search_clear),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        // Searching already runs while typing; the keyboard's search key just closes the keyboard
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedLeadingIconColor = colors.primary,
            unfocusedLeadingIconColor = colors.onSurfaceVariant,
        ),
        modifier = modifier,
    )
}

@Composable
private fun SectionHeader(title: String, count: Int, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (onBack != null) 8.dp else 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back),
                    stringResource(R.string.back_to_provinces),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.result_count, count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Pulsing placeholder cards shaped like the real list, so the layout doesn't jump when results arrive. */
@Composable
private fun LoadingState(contentPadding: PaddingValues) {
    val pulse by rememberInfiniteTransition(label = "loading").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse",
    )
    val description = stringResource(R.string.loading)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(top = 20.dp)
            .alpha(pulse)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .width(120.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Box(
                        Modifier
                            .width(180.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageState(
    icon: Int,
    title: String,
    detail: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.size(8.dp))
            action()
        }
    }
}

/** "🇹🇷 Türkiye ⌄" chip that opens the country picker. */
@Composable
private fun CountryBar(countryCode: String?, onClick: () -> Unit) {
    val country = Countries.byCode(countryCode)
    val world = stringResource(R.string.country_world)
    val label = country?.let { "${it.flag}  ${it.name}" } ?: "\uD83C\uDF0D  $world"
    val description = stringResource(R.string.cd_choose_country, country?.name ?: world)
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.heightIn(min = 40.dp).semantics { contentDescription = description },
        ) {
            Row(
                Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Icon(
                    painterResource(R.drawable.ic_expand_more),
                    null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Searchable list of every country: worldwide first, then Türkiye, then A–Z in Turkish order. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryPickerSheet(selected: String?, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("") }
    val countries = remember(filter) {
        val needle = normalizeForSearch(filter)
        Countries.all.filter { needle.isEmpty() || normalizeForSearch(it.name).contains(needle) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            placeholder = { Text(stringResource(R.string.country_search)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 8.dp)) {
            if (filter.isBlank()) {
                item(key = "world") {
                    CountryRow("\uD83C\uDF0D", stringResource(R.string.country_world), selected == null) { onSelect(null) }
                }
            }
            items(countries, key = { it.code }) { country ->
                CountryRow(country.flag, country.name, country.code == selected) { onSelect(country.code) }
            }
        }
    }
}

@Composable
private fun CountryRow(flag: String, name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(flag, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.size(16.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) Icon(painterResource(R.drawable.ic_check), null, tint = MaterialTheme.colorScheme.primary)
    }
}
