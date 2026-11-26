package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.InstructionStep
import com.example.data.catalog.MaterialItem
import com.example.data.catalog.MaterialsAndInstructionsCatalog
import com.example.data.local.PersistedStationState
import com.example.data.local.StationPreferencesRepository
import com.example.data.location.LocationHelper
import com.example.data.remote.OpenMeteoApi
import com.example.domain.WindPhysicsCalculator
import com.example.domain.model.CalculatorInputs
import com.example.domain.model.HeatingMode
import com.example.domain.model.InsulationLevel
import com.example.domain.model.LiveWeatherState
import com.example.domain.model.LocationPreset
import com.example.domain.model.StationCalculationResult
import com.example.domain.model.TerrainRoughness
import com.example.domain.model.TurbineType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class MainUiState(
    val inputs: CalculatorInputs = CalculatorInputs(),
    val weather: LiveWeatherState = LiveWeatherState(),
    val calculation: StationCalculationResult = WindPhysicsCalculator.calculate(
        CalculatorInputs(),
        LiveWeatherState()
    ),
    val materials: List<MaterialItem> = emptyList(),
    val checkedMaterialIds: Set<String> = emptySet(),
    val instructions: List<InstructionStep> = emptyList(),
    val completedStepIds: Set<String> = emptySet(),
    val isDetectingGps: Boolean = false,
    val gpsMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = StationPreferencesRepository(application.applicationContext)
    private val locationHelper = LocationHelper(application.applicationContext)
    private val weatherApi = OpenMeteoApi.create()

    private val weatherState = MutableStateFlow(LiveWeatherState())
    private val manualWindState = MutableStateFlow<Double?>(null)
    private val gpsDetectingState = MutableStateFlow(false)
    private val gpsMessageState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MainUiState> = combine(
        prefsRepo.stateFlow,
        weatherState,
        manualWindState,
        gpsDetectingState,
        gpsMessageState
    ) { persisted: PersistedStationState, weather, manualWind, isGps, gpsMsg ->
        val effectiveInputs = persisted.inputs.copy(manualWindOverrideMs = manualWind)
        val calc = WindPhysicsCalculator.calculate(effectiveInputs, weather)
        val materials = MaterialsAndInstructionsCatalog.buildMaterialsList(effectiveInputs, calc)
        val instructions = MaterialsAndInstructionsCatalog.buildInstructions(effectiveInputs, calc)

        MainUiState(
            inputs = effectiveInputs,
            weather = weather,
            calculation = calc,
            materials = materials,
            checkedMaterialIds = persisted.checkedMaterialIds,
            instructions = instructions,
            completedStepIds = persisted.completedStepIds,
            isDetectingGps = isGps,
            gpsMessage = gpsMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(
            materials = MaterialsAndInstructionsCatalog.buildMaterialsList(
                CalculatorInputs(),
                WindPhysicsCalculator.calculate(CalculatorInputs(), LiveWeatherState())
            ),
            instructions = MaterialsAndInstructionsCatalog.buildInstructions(
                CalculatorInputs(),
                WindPhysicsCalculator.calculate(CalculatorInputs(), LiveWeatherState())
            )
        )
    )

    init {
        viewModelScope.launch {
            prefsRepo.stateFlow
                .map { it.inputs.selectedLocation }
                .distinctUntilChanged { a, b ->
                    a.id == b.id && a.latitude == b.latitude && a.longitude == b.longitude
                }
                .collect { loc ->
                    fetchWeatherForLocation(loc)
                }
        }
    }

    private suspend fun updateInputs(transform: (CalculatorInputs) -> CalculatorInputs) {
        val current = prefsRepo.stateFlow.first().inputs
        prefsRepo.saveInputs(transform(current))
    }

    fun updateRoomArea(areaSqm: Double) {
        viewModelScope.launch {
            updateInputs { it.copy(roomAreaSqm = (areaSqm * 2.0).roundToInt() / 2.0) }
        }
    }

    fun updateCeilingHeight(heightM: Double) {
        viewModelScope.launch {
            updateInputs { it.copy(ceilingHeightM = (heightM * 10.0).roundToInt() / 10.0) }
        }
    }

    fun updateInsulation(level: InsulationLevel) {
        viewModelScope.launch { updateInputs { it.copy(insulation = level) } }
    }

    fun updateHeatingMode(mode: HeatingMode) {
        viewModelScope.launch { updateInputs { it.copy(heatingMode = mode) } }
    }

    fun updateTurbineType(type: TurbineType) {
        viewModelScope.launch { updateInputs { it.copy(turbineType = type) } }
    }

    fun updateRotorDiameter(diameterM: Double) {
        viewModelScope.launch {
            updateInputs { it.copy(rotorDiameterM = (diameterM * 10.0).roundToInt() / 10.0) }
        }
    }

    fun updateMastHeight(heightM: Double) {
        viewModelScope.launch {
            updateInputs { it.copy(mastHeightM = heightM.roundToInt().toDouble()) }
        }
    }

    fun updateTerrain(terrain: TerrainRoughness) {
        viewModelScope.launch { updateInputs { it.copy(terrain = terrain) } }
    }

    fun setWinterDesignConditions(useWinter: Boolean) {
        viewModelScope.launch { updateInputs { it.copy(useWinterDesignConditions = useWinter) } }
    }

    fun setManualWindOverride(windMs: Double?) {
        manualWindState.value = windMs?.let { (it * 10.0).roundToInt() / 10.0 }
    }

    fun autoSizeForRoom() {
        val recommended = uiState.value.calculation.recommendedDiameterM
        updateRotorDiameter(recommended)
    }

    fun selectLocationPreset(preset: LocationPreset) {
        manualWindState.value = null
        gpsMessageState.value = null
        viewModelScope.launch {
            updateInputs { it.copy(selectedLocation = preset) }
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            fetchWeatherForLocation(uiState.value.inputs.selectedLocation)
        }
    }

    fun detectGpsLocation() {
        if (gpsDetectingState.value) return
        viewModelScope.launch {
            gpsDetectingState.value = true
            gpsMessageState.value = null
            val detected = locationHelper.getCurrentCoarseLocation()
            gpsDetectingState.value = false

            if (detected == null) {
                gpsMessageState.value = "Не вдалося отримати координати. Оберіть місто зі списку."
                return@launch
            }

            val estimatedWinterWind = estimateRegionalWinterWind(detected.latitude)
            val estimatedWinterTemp = estimateRegionalWinterTemp(detected.latitude)
            val customLoc = LocationPreset(
                id = "gps_custom",
                nameUk = detected.displayName,
                regionUk = detected.regionLabel,
                latitude = detected.latitude,
                longitude = detected.longitude,
                avgWinterWind10mMs = estimatedWinterWind,
                avgWinterTempC = estimatedWinterTemp
            )
            manualWindState.value = null
            updateInputs { it.copy(selectedLocation = customLoc) }
        }
    }

    fun clearGpsMessage() {
        gpsMessageState.value = null
    }

    private suspend fun fetchWeatherForLocation(location: LocationPreset) {
        weatherState.value = weatherState.value.copy(isLoading = true, errorMessage = null)
        try {
            val response = weatherApi.getWindForecast(
                latitude = location.latitude,
                longitude = location.longitude
            )
            val currentWind = response.current?.windSpeed10m
            val currentTemp = response.current?.temperature2m
            val windDir = response.current?.windDirection10m
            val weeklyWinds = response.daily?.windSpeed10mMean?.filterNotNull().orEmpty()
            val weekMean = if (weeklyWinds.isNotEmpty()) weeklyWinds.average() else null

            weatherState.value = LiveWeatherState(
                isLoading = false,
                currentWind10mMs = currentWind,
                currentTempC = currentTemp,
                weekMeanWind10mMs = weekMean,
                windDirectionDeg = windDir,
                sourceLabel = "Open-Meteo (живі дані)",
                errorMessage = null
            )
        } catch (_: Exception) {
            weatherState.value = LiveWeatherState(
                isLoading = false,
                currentWind10mMs = null,
                currentTempC = null,
                weekMeanWind10mMs = null,
                windDirectionDeg = null,
                sourceLabel = "Кліматична норма регіону",
                errorMessage = "Офлайн-режим: використано кліматичні дані регіону"
            )
        }
    }

    fun toggleMaterialChecked(materialId: String) {
        viewModelScope.launch { prefsRepo.toggleMaterialChecked(materialId) }
    }

    fun clearCheckedMaterials() {
        viewModelScope.launch { prefsRepo.clearCheckedMaterials() }
    }

    fun toggleStepCompleted(stepId: String) {
        viewModelScope.launch { prefsRepo.toggleStepCompleted(stepId) }
    }

    fun buildShareableMaterialsText(): String {
        val state = uiState.value
        val totalUah = state.materials.sumOf { it.estimatedCostUah }
        return buildString {
            appendLine("ВітроДім — Специфікація матеріалів")
            appendLine("Кімната: ${state.inputs.roomAreaSqm.roundToInt()} м² | Ротор: ${state.inputs.rotorDiameterM} м | Щогла: ${state.inputs.mastHeightM.roundToInt()} м")
            appendLine("Генератор: ${state.calculation.recommendedGeneratorWatts} Вт / ${state.calculation.systemVoltageV}В")
            appendLine()
            state.materials.forEachIndexed { idx, item ->
                val mark = if (item.id in state.checkedMaterialIds) "[✓]" else "[ ]"
                appendLine("${idx + 1}. $mark ${item.nameUk} — ${item.quantityUk} (~${item.estimatedCostUah} грн)")
                appendLine("    ${item.specUk}")
            }
            appendLine()
            appendLine("Орієнтовний бюджет: ~$totalUah грн")
        }
    }

    private fun estimateRegionalWinterWind(lat: Double): Double {
        return when {
            lat < 47.2 -> 5.3 // Southern coastal/steppe
            lat < 49.0 -> 4.8
            else -> 4.3
        }
    }

    private fun estimateRegionalWinterTemp(lat: Double): Double {
        return when {
            lat < 47.0 -> 0.0
            lat < 48.5 -> -2.5
            else -> -4.0
        }
    }
}
