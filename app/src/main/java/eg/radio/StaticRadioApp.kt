package eg.radio

import android.app.Application
import eg.radio.data.local.AppDatabase
import eg.radio.data.settings.SettingsRepository
import eg.radio.playback.PlaybackRepository
import eg.radio.playback.RadioController

/**
 * Manual DI container for the skeleton — see PlaybackRepository's own note.
 * Swap for Hilt once the app grows past this stage.
 */
class StaticRadioApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val playbackRepository: PlaybackRepository by lazy { PlaybackRepository.getInstance() }
    val radioController: RadioController by lazy { RadioController(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
