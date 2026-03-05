package com.multibankgroup.pricetracker.app

import android.app.Application
import com.multibankgroup.pricetracker.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class required by Hilt for dependency injection.
 *
 * Also initializes Timber for debug logging
 */
@HiltAndroidApp
class PriceTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}