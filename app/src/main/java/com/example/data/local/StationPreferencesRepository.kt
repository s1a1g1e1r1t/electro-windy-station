package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.CalculatorInputs
import com.example.domain.model.HeatingMode
import com.example.domain.model.InsulationLevel
import com.example.domain.model.LocationCatalog
import com.example.domain.model.LocationPreset
import com.example.domain.model.TerrainRoughness
import com.example.domain.model.TurbineType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.stationDataStore: DataStore<Preferences> by preferencesDataStore(name = "vitrodim_prefs")

data class PersistedStationState(
    val inputs: CalculatorInputs = CalculatorInputs(),
    val checkedMaterialIds: Set<String> = emptySet(),
    val completedStepIds: Set<String> = emptySet()
)

class StationPreferencesRepository(private val context: Context) {

    private object Keys {
        val ROOM_AREA = doublePreferencesKey("room_area_sqm")
        val CEILING_HEIGHT = doublePreferencesKey("ceiling_height_m")
        val INSULATION = stringPreferencesKey("insulation_level")
        val HEATING_MODE = stringPreferencesKey("heating_mode")
        val TURBINE_TYPE = stringPreferencesKey("turbine_type")
        val ROTOR_DIAMETER = doublePreferencesKey("rotor_diameter_m")
        val MAST_HEIGHT = doublePreferencesKey("mast_height_m")
        val TERRAIN = stringPreferencesKey("terrain_roughness")
        val USE_WINTER_DESIGN = booleanPreferencesKey("use_winter_design")
        val LOCATION_ID = stringPreferencesKey("location_id")
        val CUSTOM_LOC_NAME = stringPreferencesKey("custom_loc_name")
        val CUSTOM_LOC_REGION = stringPreferencesKey("custom_loc_region")
        val CUSTOM_LOC_LAT = doublePreferencesKey("custom_loc_lat")
        val CUSTOM_LOC_LON = doublePreferencesKey("custom_loc_lon")
        val CHECKED_MATERIALS = stringSetPreferencesKey("checked_materials")
        val COMPLETED_STEPS = stringSetPreferencesKey("completed_steps")
    }

    val stateFlow: Flow<PersistedStationState> = context.stationDataStore.data.map { prefs ->
        val locId = prefs[Keys.LOCATION_ID] ?: "kyiv"
        val location = if (locId == "gps_custom") {
            LocationPreset(
                id = "gps_custom",
                nameUk = prefs[Keys.CUSTOM_LOC_NAME] ?: "Ваша локація",
                regionUk = prefs[Keys.CUSTOM_LOC_REGION] ?: "GPS",
                latitude = prefs[Keys.CUSTOM_LOC_LAT] ?: 50.45,
                longitude = prefs[Keys.CUSTOM_LOC_LON] ?: 30.52,
                avgWinterWind10mMs = 4.5,
                avgWinterTempC = -3.5
            )
        } else {
            LocationCatalog.presets.find { it.id == locId } ?: LocationCatalog.presets.first()
        }

        val inputs = CalculatorInputs(
            roomAreaSqm = prefs[Keys.ROOM_AREA] ?: 14.0,
            ceilingHeightM = prefs[Keys.CEILING_HEIGHT] ?: 2.6,
            insulation = prefs[Keys.INSULATION]?.let { name ->
                InsulationLevel.entries.find { it.name == name }
            } ?: InsulationLevel.STANDARD,
            heatingMode = prefs[Keys.HEATING_MODE]?.let { name ->
                HeatingMode.entries.find { it.name == name }
            } ?: HeatingMode.MINIMUM,
            turbineType = prefs[Keys.TURBINE_TYPE]?.let { name ->
                TurbineType.entries.find { it.name == name }
            } ?: TurbineType.HAWT_3_BLADE,
            rotorDiameterM = prefs[Keys.ROTOR_DIAMETER] ?: 2.6,
            mastHeightM = prefs[Keys.MAST_HEIGHT] ?: 9.0,
            terrain = prefs[Keys.TERRAIN]?.let { name ->
                TerrainRoughness.entries.find { it.name == name }
            } ?: TerrainRoughness.SUBURBAN,
            useWinterDesignConditions = prefs[Keys.USE_WINTER_DESIGN] ?: true,
            selectedLocation = location
        )

        PersistedStationState(
            inputs = inputs,
            checkedMaterialIds = prefs[Keys.CHECKED_MATERIALS] ?: emptySet(),
            completedStepIds = prefs[Keys.COMPLETED_STEPS] ?: emptySet()
        )
    }

    suspend fun saveInputs(inputs: CalculatorInputs) {
        context.stationDataStore.edit { prefs ->
            prefs[Keys.ROOM_AREA] = inputs.roomAreaSqm
            prefs[Keys.CEILING_HEIGHT] = inputs.ceilingHeightM
            prefs[Keys.INSULATION] = inputs.insulation.name
            prefs[Keys.HEATING_MODE] = inputs.heatingMode.name
            prefs[Keys.TURBINE_TYPE] = inputs.turbineType.name
            prefs[Keys.ROTOR_DIAMETER] = inputs.rotorDiameterM
            prefs[Keys.MAST_HEIGHT] = inputs.mastHeightM
            prefs[Keys.TERRAIN] = inputs.terrain.name
            prefs[Keys.USE_WINTER_DESIGN] = inputs.useWinterDesignConditions
            prefs[Keys.LOCATION_ID] = inputs.selectedLocation.id
            if (inputs.selectedLocation.id == "gps_custom") {
                prefs[Keys.CUSTOM_LOC_NAME] = inputs.selectedLocation.nameUk
                prefs[Keys.CUSTOM_LOC_REGION] = inputs.selectedLocation.regionUk
                prefs[Keys.CUSTOM_LOC_LAT] = inputs.selectedLocation.latitude
                prefs[Keys.CUSTOM_LOC_LON] = inputs.selectedLocation.longitude
            }
        }
    }

    suspend fun toggleMaterialChecked(materialId: String) {
        context.stationDataStore.edit { prefs ->
            val current = prefs[Keys.CHECKED_MATERIALS] ?: emptySet()
            prefs[Keys.CHECKED_MATERIALS] = if (materialId in current) current - materialId else current + materialId
        }
    }

    suspend fun clearCheckedMaterials() {
        context.stationDataStore.edit { prefs ->
            prefs[Keys.CHECKED_MATERIALS] = emptySet()
        }
    }

    suspend fun toggleStepCompleted(stepId: String) {
        context.stationDataStore.edit { prefs ->
            val current = prefs[Keys.COMPLETED_STEPS] ?: emptySet()
            prefs[Keys.COMPLETED_STEPS] = if (stepId in current) current - stepId else current + stepId
        }
    }
}
