package com.rnsgate.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rnsgate.app.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsStore: SettingsStore) : ViewModel() {

    private val _name = MutableStateFlow("Operator")
    val displayName: StateFlow<String> = _name

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    init {
        viewModelScope.launch {
            settingsStore.displayName.collect { stored ->
                if (!_editing) _name.value = stored
            }
        }
    }

    private var _editing = false

    fun onNameChange(v: String) {
        _editing = true
        _name.value = v
    }

    fun save() {
        viewModelScope.launch {
            settingsStore.setDisplayName(_name.value)
            _editing = false
            _saved.value = true
        }
    }

    fun clearSaved() {
        _saved.value = false
    }

    companion object {
        fun factory(settingsStore: SettingsStore) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settingsStore) as T
            }
        }
    }
}
