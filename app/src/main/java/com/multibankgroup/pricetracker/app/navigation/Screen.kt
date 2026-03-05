package com.multibankgroup.pricetracker.app.navigation

import kotlinx.serialization.Serializable

/** Feed screen — start destination. */
@Serializable
data object Feed

/** Detail screen — requires [symbol] argument. */
@Serializable
data class Detail(val symbol: String)