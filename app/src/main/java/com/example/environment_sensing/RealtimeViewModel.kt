package com.example.environment_sensing

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RealtimeViewModel : ViewModel() {

    enum class ScanUiPhase { Idle, Starting, Active }

    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData: StateFlow<SensorData?> = _sensorData

    private val _rareMessage = MutableStateFlow("")
    val rareMessage: StateFlow<String> = _rareMessage

    private val _normalMessage = MutableStateFlow("")
    val normalMessage: StateFlow<String> = _normalMessage

    private val _showRareDialog = MutableStateFlow(false)
    val showRareDialog: StateFlow<Boolean> = _showRareDialog

    private val _showNormalDialog = MutableStateFlow(false)
    val showNormalDialog: StateFlow<Boolean> = _showNormalDialog

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _scanUiPhase = MutableStateFlow(ScanUiPhase.Idle)
    val scanUiPhase: StateFlow<ScanUiPhase> = _scanUiPhase

    init {
        viewModelScope.launch {
            SensorEventBus.sensorData.collect { _sensorData.value = it }
        }
        viewModelScope.launch {
            SensorEventBus.rareEvent.collect { _rareMessage.value = it; _showRareDialog.value = true }
        }
        viewModelScope.launch {
            SensorEventBus.normalEvent.collect { _normalMessage.value = it; _showNormalDialog.value = true }
        }
    }

    fun setScanning(enabled: Boolean) {
        _isScanning.value = enabled
        if (enabled) {
            _scanUiPhase.value = ScanUiPhase.Starting
            viewModelScope.launch {
                kotlinx.coroutines.delay(1200)
                _scanUiPhase.value = ScanUiPhase.Active
            }
        } else {
            _scanUiPhase.value = ScanUiPhase.Idle
        }
    }

    fun dismissRare() { _showRareDialog.value = false }
    fun dismissNormal() { _showNormalDialog.value = false }

    // === レベル判定（色分け） ===
    fun tempLevel(t: Double?): Level = when {
        t == null -> Level.Unknown
        t in 20.0..26.0 -> Level.Good
        t < 18.0 || t > 30.0 -> Level.Bad
        else -> Level.Warn
    }
    fun humidLevel(h: Double?): Level = when {
        h == null -> Level.Unknown
        h in 40.0..60.0 -> Level.Good
        h < 30.0 || h > 70.0 -> Level.Bad
        else -> Level.Warn
    }
    fun lightLevel(lx: Int?): Level = when {
        lx == null -> Level.Unknown
        lx in 300..700 -> Level.Good
        lx < 100 || lx > 2000 -> Level.Bad
        else -> Level.Warn
    }
    fun pressureLevel(hPa: Double?): Level = when {
        hPa == null -> Level.Unknown
        hPa in 1005.0..1020.0 -> Level.Good
        else -> Level.Warn
    }
    fun noiseLevel(db: Double?): Level = when {
        db == null -> Level.Unknown
        db < 50.0 -> Level.Good
        db > 75.0 -> Level.Bad
        else -> Level.Warn
    }
    fun tvocLevel(ppb: Int?): Level = when {
        ppb == null -> Level.Unknown
        ppb < 220 -> Level.Good
        ppb > 660 -> Level.Bad
        else -> Level.Warn
    }
    fun co2Level(ppm: Int?): Level = when {
        ppm == null -> Level.Unknown
        ppm < 1000 -> Level.Good
        ppm > 2000 -> Level.Bad
        else -> Level.Warn
    }

    enum class Level { Good, Warn, Bad, Unknown }

    fun levelColor(level: Level): Color = when (level) {
        Level.Good -> Color(0xFF2E7D32)
        Level.Warn -> Color(0xFFF9A825)
        Level.Bad  -> Color(0xFFC62828)
        Level.Unknown -> Color(0xFF757575)
    }
}