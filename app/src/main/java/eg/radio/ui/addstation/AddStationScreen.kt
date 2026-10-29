package eg.radio.ui.addstation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import eg.radio.data.local.StationDao
import eg.radio.data.local.TagType
import eg.radio.data.remote.RadioBrowserStation
import eg.radio.ui.common.CountryCodeField
import eg.radio.ui.common.GenreVocabularyField
import eg.radio.ui.common.LocalPlayerBarBottomInset
import eg.radio.ui.common.TagPickerField
import eg.radio.ui.home.StationArt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStationScreen(
    stationDao: StationDao,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AddStationViewModel = viewModel(factory = AddStationViewModel.Factory(stationDao))
    val mode by viewModel.mode.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.stationSaved.collect { onBack() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("إضافة محطة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = mode.ordinal,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = mode == AddStationMode.SEARCH,
                    onClick = { viewModel.setMode(AddStationMode.SEARCH) },
                    text = { Text("بحث") }
                )
                Tab(
                    selected = mode == AddStationMode.MANUAL,
                    onClick = { viewModel.setMode(AddStationMode.MANUAL) },
                    text = { Text("رابط يدوي") }
                )
            }

            when (mode) {
                AddStationMode.MANUAL -> ManualEntryForm(viewModel, stationDao)
                AddStationMode.SEARCH -> SearchForm(viewModel)
            }
        }
    }
}

@Composable
private fun ManualEntryForm(viewModel: AddStationViewModel, stationDao: StationDao) {
    val streamUrl by viewModel.streamUrl.collectAsState()
    val name by viewModel.name.collectAsState()
    val genre by viewModel.genre.collectAsState()
    val country by viewModel.country.collectAsState()
    val mood by viewModel.mood.collectAsState()
    val style by viewModel.style.collectAsState()
    val error by viewModel.manualError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = LocalPlayerBarBottomInset.current),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = streamUrl,
            onValueChange = viewModel::setStreamUrl,
            label = { Text("رابط البث *") },
            placeholder = { Text("https://...") },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = name,
            onValueChange = viewModel::setName,
            label = { Text("الاسم (اختياري)") },
            modifier = Modifier.fillMaxWidth()
        )
        GenreVocabularyField(
            stationDao = stationDao,
            value = genre,
            onValueChange = viewModel::setGenre,
            label = "النوع (اختياري)"
        )
        CountryCodeField(
            value = country,
            onValueChange = viewModel::setCountry,
            label = "الدولة (اختياري)"
        )
        TagPickerField(
            stationDao = stationDao,
            tagType = TagType.MOOD,
            value = mood,
            onValueChange = viewModel::setMood,
            label = "المزاج (اختياري)"
        )
        TagPickerField(
            stationDao = stationDao,
            tagType = TagType.STYLE,
            value = style,
            onValueChange = viewModel::setStyle,
            label = "النمط (اختياري)"
        )
        Button(onClick = viewModel::saveManualStation, modifier = Modifier.fillMaxWidth()) {
            Text("إضافة محطة")
        }
    }
}

@Composable
private fun SearchForm(viewModel: AddStationViewModel) {
    val query by viewModel.query.collectAsState()
    val tagQuery by viewModel.tagQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val error by viewModel.searchError.collectAsState()

    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            kotlinx.coroutines.delay(400)
            viewModel.search()
        }
    }
    LaunchedEffect(tagQuery) {
        if (tagQuery.isNotBlank()) {
            kotlinx.coroutines.delay(400)
            viewModel.searchByTag()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                label = { Text("بحث بالاسم") },
                modifier = Modifier.weight(1f)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            Button(onClick = viewModel::search) { Text("بحث") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = tagQuery,
                onValueChange = viewModel::setTagQuery,
                label = { Text("بحث بوسم النوع") },
                modifier = Modifier.weight(1f)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            Button(onClick = viewModel::searchByTag) { Text("بحث") }
        }

        val isIdle = query.isBlank() && tagQuery.isBlank()

        if (isIdle) {
            // لا يوجد بحث: اعرض اختصارات المحطات المصرية (تختفي بمجرد الكتابة وتعود عند المسح)
            EgyptShortcuts(viewModel)
        } else {
            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }

            if (isSearching) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (results.isEmpty() && error == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "لم يتم العثور على محطات. جرّب اسماً أو وسماً مختلفاً.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = LocalPlayerBarBottomInset.current)
                ) {
                    items(results, key = { it.stationUuid }) { result ->
                        SearchResultRow(result = result, onAdd = { viewModel.importStation(result) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EgyptShortcuts(viewModel: AddStationViewModel) {
    val stations by viewModel.egyptStations.collectAsState()
    val loading by viewModel.isLoadingEgypt.collectAsState()
    val error by viewModel.egyptError.collectAsState()
    val added by viewModel.addedUuids.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = if (stations.isEmpty()) "محطات مصرية" else "محطات مصرية (${stations.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        when {
            loading && stations.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            stations.isEmpty() -> Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = error ?: "لا توجد محطات.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::loadEgyptStations) { Text("إعادة المحاولة") }
            }
            else -> {
                if (error != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::loadEgyptStations) { Text("إعادة المحاولة") }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = LocalPlayerBarBottomInset.current)
                ) {
                    items(stations, key = { it.stationUuid }) { station ->
                        SearchResultRow(
                            result = station,
                            added = station.stationUuid in added,
                            onAdd = { viewModel.importStation(station, closeScreen = false) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: RadioBrowserStation, onAdd: () -> Unit, added: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StationArt(name = result.name, imageUrl = result.favicon.ifBlank { null }, size = 44.dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(result.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            val subtitle = listOfNotNull(
                result.countryCode.ifBlank { null }?.let { eg.radio.ui.common.countryAr(it) },
                result.primaryGenre?.let { eg.radio.ui.common.tagAr(it) },
                result.bitrate.takeIf { it > 0 }?.let { eg.radio.ui.common.bitrateAr(it) }
            ).joinToString(" - ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        TextButton(onClick = onAdd, enabled = !added) { Text(if (added) "تمت الإضافة" else "إضافة") }
    }
}
