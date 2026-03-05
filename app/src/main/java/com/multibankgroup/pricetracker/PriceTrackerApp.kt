package com.multibankgroup.pricetracker

import android.app.Application
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