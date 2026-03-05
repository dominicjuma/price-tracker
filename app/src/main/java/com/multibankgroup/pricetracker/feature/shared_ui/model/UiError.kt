package com.multibankgroup.pricetracker.feature.shared_ui.model

import androidx.annotation.StringRes
import com.multibankgroup.pricetracker.R

/** User-facing error messages. Mapped from [DataError] in ViewModels. */
enum class UiError(@StringRes val messageResId: Int) {
    CONNECTION_FAILED(R.string.error_connection_failed),
    CONNECTION_LOST(R.string.error_connection_lost),
    NO_INTERNET(R.string.error_no_internet),
    PARSE_FAILED(R.string.error_parse_failed)
}