package ru.dyatel.tsuschedule

import android.app.Application
import android.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import ru.dyatel.tsuschedule.database.DatabaseManager

class ScheduleApplication : Application() {

    val database = DatabaseManager(this)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.ENABLE_CRASHLYTICS)
            Fabric.with(this, Crashlytics())

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
    }

}
