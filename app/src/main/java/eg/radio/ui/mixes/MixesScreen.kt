package eg.radio.ui.mixes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import eg.radio.data.GenreTags
import eg.radio.data.local.MixDao
import eg.radio.data.local.MixEntity
import eg.radio.data.local.MixSource
import eg.radio.data.local.MixWithTracks
import eg.radio.ui.common.AppTopBar
import eg.radio.ui.common.LocalPlayerBarBottomInset
import eg.radio.ui.common.TopBarMode
import eg.radio.ui.common.verticalLinesBackground
import eg.radio.ui.home.StationArt
import eg.radio.ui.home.StationPlate

@Composable
fun MixesScreen(
    mixDao: MixDao,
    onEditMix: (String) -> Unit,
    onAddMix: () -> Unit,
    onSettingsClick: () -> Unit,
    onBack: () -> Unit,
    showBackgroundGrid: Boolean = true,
    gridSpacing: androidx.compose.ui.unit.Dp = 28.dp,
    gridLineWidth: androidx.compose.ui.unit.Dp = 1.dp,
    gridOpacity: Float = 1f,
    modifier: Modifier = Modifier
) {
    val viewModel: MixesViewModel = viewModel(factory = MixesViewModel.Factory(mixDao))
    val filter by viewModel.filter.collectAsState()
    val genres by viewModel.genres.collectAsState()
    val moods by viewModel.moods.collectAsState()
    val styles by viewModel.styles.collectAsState()
    val mixes by viewModel.mixes.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }
    val filterActive = filter.favoritesOnly || filter.genre != null || filter.mood != null || filter.style != null
    val context = LocalContext.current

    if (showFilterDialog) {
        MixFilterDialog(
            filter = filter,
            genres = genres,
            moods = moods,
            styles = styles,
            onGenreClick = viewModel::toggleGenreFilter,
            onMoodClick = viewModel::toggleMoodFilter,
            onStyleClick = viewModel::toggleStyleFilter,
            onFavoritesClick = viewModel::toggleFavoritesOnly,
            onDismiss = { showFilterDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppTopBar(
            mode = TopBarMode.MIXES,
            activeViewMode = null,
            onListClick = {},
            onGridClick = {},
            onMixesClick = {},
            onFilterClick = { showFilterDialog = true },
            filterActive = filterActive,
            onAddClick = onAddMix,
            onSettingsClick = onSettingsClick,
            subtitle = "// الميكسات المحفوظة",
            onBackClick = onBack
        )

        val lineColor = MaterialTheme.colorScheme.primary
        val backgroundModifier = if (showBackgroundGrid) {
            Modifier.verticalLinesBackground(lineColor, cellSize = gridSpacing, lineWidth = gridLineWidth, opacity = gridOpacity)
        } else {
            Modifier
        }

        if (mixes.isEmpty() && !filterActive) {
            eg.radio.ui.home.EmptyStateMessage(
                title = "لا توجد ميكسات بعد",
                body = "اضغط على زر + بالأعلى للإضافة — أو شارك رابطاً مباشرة من تطبيق SoundCloud أو Mixcloud وسيعبّئ راديو EG التفاصيل تلقائياً.",
                modifier = Modifier.fillMaxSize().then(backgroundModifier)
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().then(backgroundModifier),
            contentPadding = PaddingValues(
                start = 14.dp, top = 14.dp, end = 14.dp,
                bottom = 14.dp + LocalPlayerBarBottomInset.current
            )
        ) {
            items(mixes, key = { it.mix.id }) { mixWithTracks ->
                MixListRow(
                    mix = mixWithTracks.mix,
                    onClick = { openMixExternally(context, mixWithTracks.mix.url) },
                    onLongClick = { onEditMix(mixWithTracks.mix.id) },
                    modifier = Modifier.padding(bottom = 9.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MixListRow(
    mix: MixEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        StationPlate(
            popularityEmoji = null,
            mood = mix.mood,
            isFavorite = mix.isFavorite,
            genres = mix.genre?.let { GenreTags.parse(it) } ?: emptyList(),
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StationArt(name = mix.fullTitle ?: mix.url, imageUrl = mix.image, size = 52.dp, shape = RoundedCornerShape(8.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f, fill = true)) {
                    Text(
                        text = mix.fullTitle ?: "ميكس بدون عنوان",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    val artistLine = listOfNotNull(mix.artist, mix.mixTitle).joinToString(" - ")
                    if (artistLine.isNotEmpty()) {
                        Text(
                            text = artistLine,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    if (!mix.sourceRadio.isNullOrBlank()) {
                        Text(
                            text = mix.sourceRadio,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                SourceBadge(source = mix.sourceStreamingSite)
            }
        }
    }
}

@Composable
private fun SourceBadge(source: MixSource) {
    when (source) {
        MixSource.SOUNDCLOUD -> SoundCloudLogo()
        MixSource.MIXCLOUD -> MixcloudLogo()
        MixSource.YOUTUBE -> YouTubeLogo()
        MixSource.OTHER -> Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.background)
        }
    }
}

@Composable
private fun MixFilterDialog(
    filter: MixFilter,
    genres: List<String>,
    moods: List<String>,
    styles: List<String>,
    onGenreClick: (String) -> Unit,
    onMoodClick: (String) -> Unit,
    onStyleClick: (String) -> Unit,
    onFavoritesClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("تصفية") },
        text = {
            Column {
                MixFilterSection(title = "المفضلة") {
                    MixFilterChip(label = "★ المفضلة", active = filter.favoritesOnly, onClick = onFavoritesClick)
                }
                MixFilterSection(title = "النوع") {
                    if (genres.isEmpty()) {
                        Text("لا توجد أنواع بعد", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    genres.forEach { genre -> MixFilterChip(label = eg.radio.ui.common.tagAr(genre), active = genre == filter.genre, onClick = { onGenreClick(genre) }) }
                }
                MixFilterSection(title = "المزاج") {
                    if (moods.isEmpty()) {
                        Text("لا توجد أمزجة بعد", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    moods.forEach { mood -> MixFilterChip(label = eg.radio.ui.common.tagAr(mood), active = mood == filter.mood, onClick = { onMoodClick(mood) }) }
                }
                MixFilterSection(title = "النمط") {
                    if (styles.isEmpty()) {
                        Text("لا توجد أنماط بعد", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    styles.forEach { style -> MixFilterChip(label = eg.radio.ui.common.tagAr(style), active = style == filter.style, onClick = { onStyleClick(style) }) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("تم") }
        }
    )
}

@Composable
private fun MixFilterSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun MixFilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface)
            .border(1.0.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = if (active) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
        )
    }
}
