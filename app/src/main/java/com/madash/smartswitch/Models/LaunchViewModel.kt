package com.example.smartswitch

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore("user_prefs")
private val KEY_DONE = booleanPreferencesKey("onboarding_done")

class LaunchViewModel(app: Application) : AndroidViewModel(app) {

    /** true → user has completed onboarding at least once */
    val onboardingDone: StateFlow<Boolean> = app.dataStore.data
        .map { it[KEY_DONE] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** call after the user taps “Get Started” */
    fun markDone() = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { it[KEY_DONE] = true }
    }
}
