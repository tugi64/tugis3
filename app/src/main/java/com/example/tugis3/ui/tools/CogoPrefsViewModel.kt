package com.example.tugis3.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.prefs.PrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CogoPrefsViewModel @Inject constructor(private val prefs: PrefsRepository): ViewModel() {
    private val _tab = MutableStateFlow(0)
    val tab: StateFlow<Int> = _tab
    init {
        viewModelScope.launch { prefs.cogoTab.collect { _tab.value = it } }
    }
    fun setTab(i:Int){ _tab.value = i; viewModelScope.launch { prefs.setCogoTab(i) } }
}
