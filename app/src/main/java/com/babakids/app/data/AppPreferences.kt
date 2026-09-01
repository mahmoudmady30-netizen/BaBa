package com.babakids.app.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * IMPORTANT: DataStore crashes at runtime if two separate
 * `preferencesDataStore(name = "...")` delegates are created for the same
 * file. Every repository in this app must go through this single shared
 * instance instead of declaring its own.
 */
val Context.appDataStore by preferencesDataStore(name = "baba_kids_prefs")
