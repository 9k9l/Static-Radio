package eg.radio.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import eg.radio.BuildConfig
import eg.radio.data.local.MixDao
import eg.radio.data.local.StationDao
import eg.radio.data.settings.AccentColor
import eg.radio.data.settings.DEFAULT_BUFFER_SECONDS
import eg.radio.data.settings.ImageShape
import eg.radio.data.settings.SettingsRepository
import eg.radio.data.settings.ThemeMode
import eg.radio.ui.common.LocalPlayerBarBottomInset
import eg.radio.playback.PlaybackRepository
import eg.radio.playback.RadioController
import eg.radio.ui.theme.toComposeColor
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val MAX_SLEEP_TIMER_MINUTES = 24 * 60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    stationDao: StationDao,
    mixDao: MixDao,
    radioController: RadioController,
    playbackRepository: PlaybackRepository,
    onManageGenres: () -> Unit,
    onManageMoods: () -> Unit,
    onManageStyles: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(settingsRepository, stationDao, mixDao, radioController, playbackRepository)
    )
    val context = LocalContext.current

    val themeMode by viewModel.themeMode.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val imageShape by viewModel.imageShape.collectAsState()
    val genreVocabulary by viewModel.genreVocabulary.collectAsState()
    val message by viewModel.importExportMessage.collectAsState()
    val sleepTimerEndAtMillis by viewModel.sleepTimerEndAtMillis.collectAsState()
    val normalizeVolume by viewModel.normalizeVolume.collectAsState()
    val showBackgroundGrid by viewModel.showBackgroundGrid.collectAsState()
    val gridSpacingDp by viewModel.gridSpacingDp.collectAsState()
    val gridLineWidthDp by viewModel.gridLineWidthDp.collectAsState()
    val gridOpacity by viewModel.gridOpacity.collectAsState()
    val bufferSeconds by viewModel.bufferSeconds.collectAsState()
    val castEnabled by viewModel.castEnabled.collectAsState()
    val recMaxMinutes by viewModel.recordingMaxMinutes.collectAsState()
    val recStopOnChange by viewModel.recordingStopOnStationChange.collectAsState()
    val recSubfolder by viewModel.recordingSubfolder.collectAsState()
    val moodVocabulary by stationDao.observeTagsByType(eg.radio.data.local.TagType.MOOD).collectAsState(initial = emptyList())
    val styleVocabulary by stationDao.observeTagsByType(eg.radio.data.local.TagType.STYLE).collectAsState(initial = emptyList())

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.export(context.contentResolver, it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.import(context, it) } }

    val exportMixesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.exportMixes(context.contentResolver, it) } }

    val importMixesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importMixes(context, it) } }

    LaunchedEffect(message) {
        if (message != null) {
            delay(3000)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + LocalPlayerBarBottomInset.current)
    ) {
        if (message != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(message.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = viewModel::clearMessage) {
                    Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                }
            }
        }

        // ---- Appearance ----
        SettingsCategory(title = "المظهر") {
            // Switch enforces a 48dp minimum touch target by default, which pads
            // it well beyond its visual size and was the real source of the extra
            // gap between these two adjacent rows — suppressed just here.
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement provides false
            ) {
                SettingsRow(label = "الوضع الداكن") {
                    Switch(
                        checked = themeMode == ThemeMode.DARK,
                        onCheckedChange = { viewModel.setThemeMode(if (it) ThemeMode.DARK else ThemeMode.LIGHT) }
                    )
                }
                SettingsRow(label = "اتباع النظام") {
                    Switch(
                        checked = themeMode == ThemeMode.SYSTEM,
                        onCheckedChange = { if (it) viewModel.setThemeMode(ThemeMode.SYSTEM) }
                    )
                }
            }
            SettingsRow(label = "لون التمييز") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentColor.entries.forEach { accent ->
                        ColorSwatch(
                            color = accent.toComposeColor(),
                            selected = accent == accentColor,
                            onClick = { viewModel.setAccentColor(accent) }
                        )
                    }
                }
            }
            SettingsRow(label = "شكل صورة المحطة") {
                ImageShapeDropdown(selected = imageShape, onSelect = viewModel::setImageShape)
            }
            SettingsRow(label = "إظهار شبكة الخلفية") {
                Switch(
                    checked = showBackgroundGrid,
                    onCheckedChange = viewModel::setShowBackgroundGrid
                )
            }
            if (showBackgroundGrid) {
                var gridControlsExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                SettingsRow(label = "ضبط الشبكة") {
                    OutlinedButton(onClick = { gridControlsExpanded = !gridControlsExpanded }) {
                        Text(if (gridControlsExpanded) "إخفاء" else "إظهار")
                    }
                }
                if (gridControlsExpanded) {
                    GridCustomizationControls(
                        spacing = gridSpacingDp,
                        lineWidth = gridLineWidthDp,
                        opacity = gridOpacity,
                        onSpacingChange = viewModel::setGridSpacingDp,
                        onLineWidthChange = viewModel::setGridLineWidthDp,
                        onOpacityChange = viewModel::setGridOpacity,
                        onReset = viewModel::resetGridDefaults
                    )
                }
            }
        }

        // ---- Playback ----
        SettingsCategory(title = "التشغيل") {
            SettingsRow(label = "توحيد مستوى الصوت") {
                Switch(
                    checked = normalizeVolume,
                    onCheckedChange = viewModel::setNormalizeVolume
                )
            }
            Text(
                "يوازن بين المحطات العالية والهادئة فورياً عبر قياس الإشارة وتعديل مستوى التضخيم تدريجياً نحو مستوى ثابت — لا توجد بيانات عن شدة الصوت في بث الراديو عبر الإنترنت، لذلك يستمع التطبيق ويتكيف بدلاً من ذلك.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SettingsRow(label = "ذاكرة البث المؤقتة: ${bufferSeconds} ث") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionButton(label = "-", active = false, onClick = { viewModel.setBufferSeconds((bufferSeconds - 5).coerceAtLeast(5)) })
                    OptionButton(label = "+", active = false, onClick = { viewModel.setBufferSeconds((bufferSeconds + 5).coerceAtMost(120)) })
                }
            }
            Text(
                "الذاكرة المؤقتة الأكبر تحتفظ بمزيد من الصوت قبل تشغيله، مقابل بدء أبطأ قليلاً وثبات أفضل عند انقطاع الشبكة — نفس فكرة إعداد الذاكرة المؤقتة في Transistor. يسري التغيير عند تشغيل المحطة في المرة القادمة.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---- Cast ----
        SettingsCategory(title = "الإرسال") {
            SettingsRow(label = "كروم كاست") {
                Switch(
                    checked = castEnabled,
                    onCheckedChange = viewModel::setCastEnabled
                )
            }
            Text(
                "يضيف زر إرسال في شريط المشغّل للبث إلى جهاز Chromecast على شبكتك. معطّل افتراضياً — تفعيله هو الشيء الوحيد في التطبيق الذي يستخدم خدمات Google Play.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---- Recording ----
        SettingsCategory(title = "التسجيل") {
            Text(
                "زر التسجيل في شريط المشغّل يحفظ البث الحالي كما هو (بدون إعادة ترميز) في مجلد Download على الجهاز.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SettingsRow(
                label = if (recMaxMinutes == 0) "أقصى مدة: بدون حد" else "أقصى مدة: $recMaxMinutes دقيقة"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionButton(label = "-", active = false, onClick = {
                        viewModel.setRecordingMaxMinutes((recMaxMinutes - 15).coerceAtLeast(0))
                    })
                    OptionButton(label = "+", active = false, onClick = {
                        viewModel.setRecordingMaxMinutes((recMaxMinutes + 15).coerceAtMost(600))
                    })
                }
            }
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement provides false
            ) {
                SettingsRow(label = "إيقاف التسجيل عند تغيير المحطة") {
                    Switch(checked = recStopOnChange, onCheckedChange = viewModel::setRecordingStopOnStationChange)
                }
                SettingsRow(label = "الحفظ في مجلد فرعي EGRadio") {
                    Switch(checked = recSubfolder, onCheckedChange = viewModel::setRecordingSubfolder)
                }
            }
            Text(
                "المدة صفر تعني تسجيلاً مفتوحاً حتى تضغط إيقاف. بث HLS وقوائم التشغيل غير مدعومة للتسجيل.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---- Sleep timer ----
        SettingsCategory(title = "مؤقت النوم") {
            SleepTimerControls(
                endAtMillis = sleepTimerEndAtMillis,
                onStart = viewModel::startSleepTimer,
                onCancel = viewModel::cancelSleepTimer
            )
        }

        // ---- Vocabularies ----
        SettingsCategory(title = "القوائم") {
            SettingsRow(label = "${genreVocabulary.size} نوع (أنواع) معرّف") {
                OutlinedButton(onClick = onManageGenres) { Text("إدارة") }
            }
            SettingsRow(label = "${moodVocabulary.size} مزاج (أمزجة) معرّف") {
                OutlinedButton(onClick = onManageMoods) { Text("إدارة") }
            }
            SettingsRow(label = "${styleVocabulary.size} نمط (أنماط) معرّف") {
                OutlinedButton(onClick = onManageStyles) { Text("إدارة") }
            }
        }

        // ---- Backup ----
        SettingsCategory(title = "النسخ الاحتياطي") {
            Text(
                "محطات الراديو",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "ينسخ احتياطياً كل بيانات المحطة — النوع/المزاج/النمط، الإحداثيات، الوصف، اللغة، الشعبية — بالإضافة إلى قوائم الأنواع والأمزجة والأنماط كاملة. يمكن استيراد ملف تصدير Transistor مباشرة، لكن صيغته لا تحمل هذه الحقول الإضافية.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                    exportLauncher.launch("static-backup_$timestamp.zip")
                }) { Text("تصدير") }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                    Text("استيراد")
                }
            }
            Text(
                "الميكسات المحفوظة",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            Text(
                "صيغة zip الخاصة بتطبيق راديو EG — يضم التصدير قائمة المقاطع وأي صور تم رفعها محلياً.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                    exportMixesLauncher.launch("static-mixes_$timestamp.zip")
                }) { Text("تصدير") }
                OutlinedButton(onClick = { importMixesLauncher.launch(arrayOf("*/*")) }) {
                    Text("استيراد")
                }
            }
        }

        Text(
            text = "الإصدار ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
    }
}

@Composable
private fun SettingsCategory(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 28.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.0.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun GridCustomizationControls(
    spacing: Float,
    lineWidth: Float,
    opacity: Float,
    onSpacingChange: (Float) -> Unit,
    onLineWidthChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text("تباعد الشبكة: ${spacing.toInt()}dp", style = MaterialTheme.typography.labelMedium)
        Slider(value = spacing, onValueChange = onSpacingChange, valueRange = 12f..64f)

        Text("سمك خط الشبكة: ${"%.1f".format(lineWidth)}dp", style = MaterialTheme.typography.labelMedium)
        Slider(value = lineWidth, onValueChange = onLineWidthChange, valueRange = 0.5f..4f)

        Text("شفافية الشبكة: ${(opacity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
        Slider(value = opacity, onValueChange = onOpacityChange, valueRange = 0f..1f)

        OutlinedButton(onClick = onReset, modifier = Modifier.padding(top = 4.dp)) { Text("إعادة للافتراضي") }
    }
}

@Composable
private fun SettingsRow(label: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        trailing()
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val keyline = MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .border(1.0.dp, keyline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(if (selected) 14.dp else 16.dp)
    )
}

@Composable
private fun SleepTimerControls(
    endAtMillis: Long?,
    onStart: (Long) -> Unit,
    onCancel: () -> Unit
) {
    if (endAtMillis != null) {
        var remainingMillis by remember(endAtMillis) { mutableLongStateOf(endAtMillis - System.currentTimeMillis()) }
        LaunchedEffect(endAtMillis) {
            while (remainingMillis > 0) {
                delay(1000)
                remainingMillis = endAtMillis - System.currentTimeMillis()
            }
        }
        val clamped = remainingMillis.coerceAtLeast(0)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(clamped)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(clamped) % 60
        SettingsRow(label = "يتوقف بعد %02d:%02d".format(minutes, seconds)) {
            OutlinedButton(onClick = onCancel) { Text("إلغاء") }
        }
    } else {
        var hours by remember { mutableIntStateOf(0) }
        var minutes by remember { mutableIntStateOf(30) }

        SettingsRow(label = "الساعات") {
            Stepper(
                value = hours,
                onDecrement = { hours = (hours - 1).coerceAtLeast(0) },
                onIncrement = {
                    val next = hours + 1
                    if (next * 60 + minutes <= MAX_SLEEP_TIMER_MINUTES) hours = next
                }
            )
        }
        SettingsRow(label = "الدقائق") {
            Stepper(
                value = minutes,
                onDecrement = { minutes = (minutes - 15).coerceAtLeast(0) },
                onIncrement = {
                    val next = minutes + 15
                    if (hours * 60 + next <= MAX_SLEEP_TIMER_MINUTES) minutes = next
                }
            )
        }
        SettingsRow(label = "يتوقف التشغيل تلقائياً") {
            val totalMinutes = hours * 60 + minutes
            OutlinedButton(
                enabled = totalMinutes > 0,
                onClick = { onStart(TimeUnit.MINUTES.toMillis(totalMinutes.toLong())) }
            ) { Text("بدء") }
        }
    }
}

@Composable
private fun Stepper(value: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OptionButton(label = "-", active = false, onClick = onDecrement)
        Text(
            value.toString(),
            style = MaterialTheme.typography.labelMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.widthIn(min = 24.dp)
        )
        OptionButton(label = "+", active = false, onClick = onIncrement)
    }
}

@Composable
private fun ImageShapeDropdown(selected: ImageShape, onSelect: (ImageShape) -> Unit) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val keyline = MaterialTheme.colorScheme.outline
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.0.dp, keyline, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(shapeLabel(selected), style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ImageShape.entries.forEach { shape ->
                DropdownMenuItem(
                    text = { Text(shapeLabel(shape)) },
                    onClick = { onSelect(shape); expanded = false }
                )
            }
        }
    }
}

private fun shapeLabel(shape: ImageShape): String = when (shape) {
    ImageShape.CIRCLE -> "دائري"
    ImageShape.SQUARE -> "مربع"
    ImageShape.ROUNDED -> "حواف مدورة"
}

@Composable
private fun OptionButton(label: String, active: Boolean, onClick: () -> Unit) {
    val keyline = MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .border(1.0.dp, keyline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
