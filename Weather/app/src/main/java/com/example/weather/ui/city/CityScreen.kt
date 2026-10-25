package com.example.weather.ui.city

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.weather.R
import com.example.weather.data.filterByQuery
import com.example.weather.model.City
import com.example.weather.ui.theme.LocalHeaderGradient

/** Screens at least this wide put the search field in the top bar and centre the list. */
private val WideLayoutMinWidth = 600.dp
private val ContentMaxWidth = 720.dp

@Composable
fun CityScreen(
    cities: List<City>,
    onCityClick: (City) -> Unit,
    modifier: Modifier = Modifier,
) {
    // rememberSaveable keeps both values across rotation and process death; plain remember lost them
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCityId by rememberSaveable { mutableStateOf<Long?>(null) }
    val filtered = remember(cities, query) { cities.filterByQuery(query) }

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
            Header(query = query, onQueryChange = { query = it }, wide = wide)

            val sideAndBottom = WindowInsets.safeDrawing
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues()
            Column(
                modifier = Modifier
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding(),
            ) {
                SectionHeader(count = filtered.size)
                if (filtered.isEmpty()) {
                    EmptyState(query = query, modifier = Modifier.fillMaxSize())
                } else {
                    CityList(
                        cities = filtered,
                        selectedCityId = selectedCityId,
                        onCityClick = { city ->
                            selectedCityId = city.id
                            onCityClick(city)
                        },
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = sideAndBottom.calculateBottomPadding() + 16.dp,
                        ),
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
private fun SectionHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.section_cities),
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

@Composable
private fun EmptyState(query: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
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
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.city_not_found),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.city_not_found_detail, query.trim()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
