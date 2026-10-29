package eg.radio.ui.editstation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import eg.radio.data.local.StationDao
import eg.radio.data.local.TagType
import eg.radio.ui.common.CountryCodeField
import eg.radio.ui.common.GenreVocabularyField
import eg.radio.ui.common.LanguageField
import eg.radio.ui.common.LocalPlayerBarBottomInset
import eg.radio.ui.common.TagPickerField
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStationScreen(
    stationId: String,
    stationDao: StationDao,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: EditStationViewModel = viewModel(
        key = stationId,
        factory = EditStationViewModel.Factory(stationId, stationDao)
    )
    val isLoaded by viewModel.isLoaded.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val path = copyImageToInternalStorage(context.filesDir, context.contentResolver, uri, stationId)
                if (path != null) viewModel.setImageUrl(path)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.done.collect { onBack() }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("حذف المحطة؟") },
            text = { Text("سيتم حذفها من مكتبتك. لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                }) { Text("حذف") }
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
                title = { Text("تعديل المحطة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف المحطة")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (!isLoaded) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val streamUrl by viewModel.streamUrl.collectAsState()
        val name by viewModel.name.collectAsState()
        val imageUrl by viewModel.imageUrl.collectAsState()
        val country by viewModel.country.collectAsState()
        val genre by viewModel.genre.collectAsState()
        val bitrate by viewModel.bitrate.collectAsState()
        val description by viewModel.description.collectAsState()
        val language by viewModel.language.collectAsState()
        val websiteUrl by viewModel.websiteUrl.collectAsState()
        val isFavorite by viewModel.isFavorite.collectAsState()
        val mood by viewModel.mood.collectAsState()
        val style by viewModel.style.collectAsState()
        val liveTimesFrom by viewModel.liveTimesFrom.collectAsState()
        val liveTimesTo by viewModel.liveTimesTo.collectAsState()
        val is24x7 by viewModel.is24x7.collectAsState()
        val error by viewModel.error.collectAsState()
        val liveTimeError by viewModel.liveTimeError.collectAsState()
        val clickCountSnapshot by viewModel.clickCountSnapshot.collectAsState()
        val popularityTier by viewModel.popularityTier.collectAsState()

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + LocalPlayerBarBottomInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = streamUrl,
                onValueChange = viewModel::setStreamUrl,
                label = { Text("رابط البث *") },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            SourcedField(name, viewModel::setName, "الاسم")

            SourcedField(imageUrl, viewModel::setImageUrl, "رابط الصورة")
            Button(
                onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("رفع صورة") }

            CountryCodeField(
                value = country,
                onValueChange = viewModel::setCountry,
                label = "الدولة"
            )

            if (clickCountSnapshot != null) {
                ReadOnlyField(
                    label = "عدد النقرات",
                    value = "$clickCountSnapshot${popularityTier?.let { " · $it" }.orEmpty()}"
                )
            } else {
                PopularityPicker(selected = popularityTier, onSelect = viewModel::setManualPopularityTier)
            }

            GenreVocabularyField(
                stationDao = stationDao,
                value = genre,
                onValueChange = viewModel::setGenre,
                label = "النوع",
                supportingText = "الأنواع الموسيقية المقدَّمة — يمكن اختيار أكثر من نوع"
            )
            TagPickerField(
                stationDao = stationDao,
                tagType = TagType.MOOD,
                value = mood,
                onValueChange = viewModel::setMood,
                label = "المزاج",
                supportingText = "الأجواء التي قد ترغب في تشغيل الراديو فيها"
            )
            TagPickerField(
                stationDao = stationDao,
                tagType = TagType.STYLE,
                value = style,
                onValueChange = viewModel::setStyle,
                label = "النمط",
                supportingText = "نوع البث، مثل: عرض دي جي، برنامج راديو، برنامج حواري"
            )
            SourcedField(bitrate, viewModel::setBitrate, "معدل البت (ك.ب/ث)")
            SourcedField(description, viewModel::setDescription, "الوصف")
            SourcedField(websiteUrl, viewModel::setWebsiteUrl, "رابط الموقع")
            LanguageField(
                value = language,
                onValueChange = viewModel::setLanguage,
                label = "اللغة"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("مفضّلة")
                Switch(checked = isFavorite, onCheckedChange = viewModel::setIsFavorite)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("بث مباشر على مدار الساعة")
                Switch(checked = is24x7, onCheckedChange = viewModel::setIs24x7)
            }
            if (!is24x7) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = liveTimesFrom,
                        onValueChange = viewModel::setLiveTimesFrom,
                        label = { Text("يبدأ البث من") },
                        placeholder = { Text("23:59 (24h)") },
                        isError = liveTimeError != null,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = liveTimesTo,
                        onValueChange = viewModel::setLiveTimesTo,
                        label = { Text("ينتهي البث عند") },
                        placeholder = { Text("23:59 (24h)") },
                        isError = liveTimeError != null,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (liveTimeError != null) {
                    Text(
                        liveTimeError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                val localFrom = eg.radio.data.CountryTimeZones.toLocalEquivalent(country, liveTimesFrom)
                val localTo = eg.radio.data.CountryTimeZones.toLocalEquivalent(country, liveTimesTo)
                if (localFrom != null && localTo != null) {
                    Text(
                        "≈ $localFrom–$localTo بتوقيتك (دولة المحطة: $country)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("حفظ")
            }
        }
    }
}

private fun copyImageToInternalStorage(
    filesDir: File,
    contentResolver: android.content.ContentResolver,
    uri: Uri,
    stationId: String
): String? = try {
    val imagesDir = File(filesDir, "images").apply { mkdirs() }
    val destination = File(imagesDir, "$stationId.jpg")
    contentResolver.openInputStream(uri)?.use { input ->
        destination.outputStream().use { output -> input.copyTo(output) }
    }
    "file://${destination.absolutePath}"
} catch (e: Exception) {
    null
}

@Composable
private fun SourcedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth()
    )
}

private val POPULARITY_EMOJIS = listOf("❄️", "🧊", "😐", "🔥", "🌋")

@Composable
private fun PopularityPicker(selected: String?, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("الشعبية", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "لا توجد بيانات نقرات من Radio Browser لهذه المحطة — اختر المستوى بنفسك",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            POPULARITY_EMOJIS.forEach { emoji ->
                val isSelected = emoji == selected
                Box(
                    modifier = Modifier
                        .clickable { onSelect(emoji) }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        style = if (isSelected) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
