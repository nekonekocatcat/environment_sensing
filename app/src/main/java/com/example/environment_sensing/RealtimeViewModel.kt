package com.example.environment_sensing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RealtimeViewModel : ViewModel() {

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

    init {
        // 👇 LogService や EventBus からのデータを購読
        viewModelScope.launch {
            SensorEventBus.sensorData.collect {
                _sensorData.value = it
            }
        }
        viewModelScope.launch {
            SensorEventBus.rareEvent.collect {
                _rareMessage.value = it
                _showRareDialog.value = true
            }
        }
        viewModelScope.launch {
            SensorEventBus.normalEvent.collect {
                _normalMessage.value = it
                _showNormalDialog.value = true
            }
        }
    }

    fun dismissRare() {
        _showRareDialog.value = false
    }

    fun dismissNormal() {
        _showNormalDialog.value = false
    }
}