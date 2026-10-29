package com.staticradio.app.ui.mixes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.staticradio.app.data.local.MixDao
import com.staticradio.app.data.local.StationDao
import com.staticradio.app.data.local.TagType
import com.staticradio.app.ui.common.GenreVocabularyField
import com.staticradio.app.ui.common.LocalPlayerBarBottomInset
import com.staticradio.app.ui.common.TagPickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixFormScreen(
    mixId: String?,
    mixDao: MixDao,
    stationDao: StationDao,
    prefillUrl: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MixFormViewModel = viewModel(
        key = mixId ?: "new-$prefillUrl",
        factory = MixFormViewModel.Factory(mixId, mixDao, prefillUrl)
    )
    val isLoaded by viewModel.isLoaded.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.done.collect { onBack() }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("حذف الميكس؟") },
            text = { Text("سيتم حذفه من الميكسات المحفوظة. لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.delete() }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("إلغاء") }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditMode) "تعديل الميكس" else "إضافة ميكس") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (viewModel.isEditMode) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "حذف الميكس")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (!isLoaded) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val url by viewModel.url.collectAsState()
        val fullTitle by viewModel.fullTitle.collectAsState()
        val artist by viewModel.artist.collectAsState()
        val mixTitle by viewModel.mixTitle.collectAsState()
        val sourceRadio by viewModel.sourceRadio.collectAsState()
        val genre by viewModel.genre.collectAsState()
        val mood by viewModel.mood.collectAsState()
        val style by viewModel.style.collectAsState()
        val image by viewModel.image.collectAsState()
        val releasedDate by viewModel.releasedDate.collectAsState()
        val description by viewModel.description.collectAsState()
        val isFavorite by viewModel.isFavorite.collectAsState()
        val tracks by viewModel.tracks.collectAsState()
        val error by viewModel.error.collectAsState()
        val releasedDateError by viewModel.releasedDateError.collectAsState()

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + LocalPlayerBarBottomInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = viewModel::setUrl,
                label = { Text("الرابط *") },
                placeholder = { Text("https://soundcloud.com/... أو mixcloud.com/...") },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = fullTitle,
                onValueChange = viewModel::setFullTitle,
                label = { Text("العنوان الكامل") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = artist,
                onValueChange = viewModel::setArtist,
                label = { Text("الفنان") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = mixTitle,
                onValueChange = viewModel::setMixTitle,
                label = { Text("عنوان الميكس") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = sourceRadio,
                onValueChange = viewModel::setSourceRadio,
                label = { Text("الراديو / البرنامج المصدر") },
                modifier = Modifier.fillMaxWidth()
            )
            GenreVocabularyField(
                stationDao = stationDao,
                value = genre.orEmpty(),
                onValueChange = { viewModel.setGenre(it.ifBlank { null }) },
                label = "النوع",
                supportingText = "الأنواع الموسيقية المقدَّمة — يمكن اختيار أكثر من نوع"
            )
            TagPickerField(
                stationDao = stationDao,
                tagType = TagType.MOOD,
                value = mood,
                onValueChange = viewModel::setMood,
                label = "المزاج",
                supportingText = "الأجواء التي قد ترغب في تشغيل هذا الميكس فيها"
            )
            TagPickerField(
                stationDao = stationDao,
                tagType = TagType.STYLE,
                value = style,
                onValueChange = viewModel::setStyle,
                label = "النمط",
                supportingText = "نوع البث، مثل: عرض دي جي، برنامج راديو، برنامج حواري"
            )
            OutlinedTextField(
                value = image,
                onValueChange = viewModel::setImage,
                label = { Text("رابط الصورة") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = releasedDate,
                onValueChange = viewModel::setReleasedDate,
                label = { Text("تاريخ الإصدار") },
                placeholder = { Text("يوم/شهر/سنة، مثال: 01/01/2020") },
                isError = releasedDateError != null,
                supportingText = releasedDateError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::setDescription,
                label = { Text("الوصف") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("مفضّل")
                Switch(checked = isFavorite, onCheckedChange = viewModel::setIsFavorite)
            }

            Text("قائمة المقاطع", style = MaterialTheme.typography.titleMedium)
            tracks.forEachIndexed { index, track ->
                TrackRow(
                    track = track,
                    onChange = { viewModel.updateTrackRow(index, it) },
                    onRemove = { viewModel.removeTrackRow(index) }
                )
            }
            OutlinedButton(onClick = viewModel::addTrackRow, modifier = Modifier.fillMaxWidth()) {
                Text("إضافة مقطع")
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text(if (viewModel.isEditMode) "حفظ" else "إضافة ميكس")
            }
        }
    }
}

@Composable
private fun TrackRow(track: TrackDraft, onChange: (TrackDraft) -> Unit, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = track.timestamp,
            onValueChange = { onChange(track.copy(timestamp = it)) },
            label = { Text("د:ث") },
            modifier = Modifier.weight(0.7f)
        )
        OutlinedTextField(
            value = track.artist,
            onValueChange = { onChange(track.copy(artist = it)) },
            label = { Text("الفنان") },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = track.trackTitle,
            onValueChange = { onChange(track.copy(trackTitle = it)) },
            label = { Text("المقطع") },
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "حذف المقطع")
        }
    }
}
