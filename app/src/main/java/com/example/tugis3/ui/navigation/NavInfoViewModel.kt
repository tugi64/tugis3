package com.example.tugis3.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import javax.inject.Inject

@HiltViewModel
class NavInfoViewModel @Inject constructor() : ViewModel() {

    private val _header = MutableStateFlow(HeaderInfo())
    val header: StateFlow<HeaderInfo> = _header.asStateFlow()

    init {
        // Simulated data updates
        viewModelScope.launch {
            while (true) {
                _header.value = _header.value.copy(
                    projectName = "TUGIS_PROJE_${Random.nextInt(1, 1000)}",
                    fixLabel = listOf("No Fix", "2D Fix", "3D Fix", "RTK Float", "RTK Fixed").random(),
                    satCounts = "${Random.nextInt(8, 25)}/${Random.nextInt(18, 33)}",
                    rmsLabel = "H:${String.format("%.3f", Random.nextDouble(0.005, 0.051))}m V:${String.format("%.3f", Random.nextDouble(0.008, 0.081))}m",
                    ntripStatus = listOf("Bağlantı Yok", "Bağlanıyor...", "Bağlandı", "Hata: Timeout").random()
                )
                kotlinx.coroutines.delay(2000)
            }
        }
    }
}

data class HeaderInfo(
    val projectName: String = "Yeni Proje",
    val fixLabel: String = "No Fix",
    val satCounts: String = "0/0",
    val rmsLabel: String = "H:- V:-",
    val ntripStatus: String = "Bağlantı Yok"
)
