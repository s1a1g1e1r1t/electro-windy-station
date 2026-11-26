package com.example.domain.model

enum class TurbineType(
    val titleUk: String,
    val shortTitleUk: String,
    val descriptionUk: String,
    val cp: Double,
    val cutInMs: Double,
    val ratedMs: Double,
    val tipSpeedRatio: Double
) {
    HAWT_3_BLADE(
        titleUk = "Горизонтальний (3 лопаті)",
        shortTitleUk = "3-лопатевий HAWT",
        descriptionUk = "Найвищий ККД (Cp 0.36), оптимальний для обігріву при помірному вітрі.",
        cp = 0.36,
        cutInMs = 2.7,
        ratedMs = 10.5,
        tipSpeedRatio = 6.0
    ),
    DARRIEUS_VAWT(
        titleUk = "Вертикальний (Дар'є H-ротор)",
        shortTitleUk = "Вертикальний Дар'є",
        descriptionUk = "Працює при будь-якому напрямку вітру без флюгера, тихіший у забудові.",
        cp = 0.27,
        cutInMs = 3.2,
        ratedMs = 11.5,
        tipSpeedRatio = 4.2
    ),
    SAVONIUS_VAWT(
        titleUk = "Вертикальний (Савоніуса)",
        shortTitleUk = "Ротор Савоніуса",
        descriptionUk = "Простий у виготовленні з бочок/ПВХ, стартує від 2.0 м/с, нижчий ККД.",
        cp = 0.17,
        cutInMs = 2.0,
        ratedMs = 12.0,
        tipSpeedRatio = 1.1
    )
}

enum class InsulationLevel(
    val titleUk: String,
    val subtitleUk: String,
    val wattsPerSqmAt35Delta: Double
) {
    ENERGY_EFFICIENT(
        titleUk = "Енергоефективна",
        subtitleUk = "Утеплення 150 мм, 2-камерні склопакети (~42 Вт/м²)",
        wattsPerSqmAt35Delta = 42.0
    ),
    STANDARD(
        titleUk = "Стандартна",
        subtitleUk = "Цегла/газоблок з базовим утепленням (~68 Вт/м²)",
        wattsPerSqmAt35Delta = 68.0
    ),
    POOR(
        titleUk = "Мінімальна / Дача",
        subtitleUk = "Тонкі стіни без утеплення (~105 Вт/м²)",
        wattsPerSqmAt35Delta = 105.0
    )
}

enum class HeatingMode(
    val titleUk: String,
    val targetTempC: Double,
    val descriptionUk: String
) {
    ANTI_FREEZE(
        titleUk = "Антизамерзання (+10°C)",
        targetTempC = 10.0,
        descriptionUk = "Мінімум для захисту стін та труб взимку"
    ),
    MINIMUM(
        titleUk = "Мінімальне (+16°C)",
        targetTempC = 16.0,
        descriptionUk = "Економне базове опалення кімнати"
    ),
    COMFORT(
        titleUk = "Комфортне (+20°C)",
        targetTempC = 20.0,
        descriptionUk = "Повноцінна житлова температура"
    )
}

enum class TerrainRoughness(
    val titleUk: String,
    val alphaExponent: Double,
    val hintUk: String
) {
    OPEN_FIELD(
        titleUk = "Відкрита місцевість",
        alphaExponent = 0.14,
        hintUk = "Поле, пагорб, берег — мінімум перешкод"
    ),
    SUBURBAN(
        titleUk = "Приватний сектор",
        alphaExponent = 0.20,
        hintUk = "Садиба, невисокі будинки та дерева"
    ),
    DENSE_URBAN(
        titleUk = "Щільна забудова",
        alphaExponent = 0.28,
        hintUk = "Високі перешкоди поруч — потрібна вища щогла"
    )
}

data class LocationPreset(
    val id: String,
    val nameUk: String,
    val regionUk: String,
    val latitude: Double,
    val longitude: Double,
    val avgWinterWind10mMs: Double,
    val avgWinterTempC: Double
)

object LocationCatalog {
    val presets = listOf(
        LocationPreset("kyiv", "Київ", "Київська обл.", 50.4501, 30.5234, 4.3, -3.5),
        LocationPreset("odesa", "Одеса", "Південь / Причорномор'я", 46.4825, 30.7233, 5.6, 0.2),
        LocationPreset("lviv", "Львів", "Захід / Передкарпаття", 49.8397, 24.0297, 4.6, -2.8),
        LocationPreset("kharkiv", "Харків", "Схід України", 49.9935, 36.2304, 4.5, -4.5),
        LocationPreset("dnipro", "Дніпро", "Придніпров'я", 48.4647, 35.0462, 4.7, -3.2),
        LocationPreset("carpathians", "Яремче / Карпати", "Гірський регіон", 48.4478, 24.5572, 5.9, -5.0),
        LocationPreset("poltava", "Полтава", "Центр України", 49.5883, 34.5514, 4.4, -4.0),
        LocationPreset("mykolaiv", "Миколаїв", "Степова зона", 46.9750, 31.9946, 5.3, -1.0),
        LocationPreset("vinnytsia", "Вінниця", "Поділля", 49.2331, 28.4682, 4.2, -3.6),
        LocationPreset("chernihiv", "Чернігів", "Полісся", 51.4982, 31.2893, 4.0, -4.8),
        LocationPreset("zaporizhzhia", "Запоріжжя", "Південний Схід", 47.8388, 35.1396, 5.1, -2.5),
        LocationPreset("uzhhorod", "Ужгород", "Закарпаття", 48.6208, 22.2879, 3.8, -1.2)
    )
}

data class LiveWeatherState(
    val isLoading: Boolean = false,
    val currentWind10mMs: Double? = null,
    val currentTempC: Double? = null,
    val weekMeanWind10mMs: Double? = null,
    val windDirectionDeg: Int? = null,
    val sourceLabel: String = "Кліматична норма регіону",
    val errorMessage: String? = null
)

data class CalculatorInputs(
    val roomAreaSqm: Double = 14.0,
    val ceilingHeightM: Double = 2.6,
    val insulation: InsulationLevel = InsulationLevel.STANDARD,
    val heatingMode: HeatingMode = HeatingMode.MINIMUM,
    val turbineType: TurbineType = TurbineType.HAWT_3_BLADE,
    val rotorDiameterM: Double = 2.6,
    val mastHeightM: Double = 9.0,
    val terrain: TerrainRoughness = TerrainRoughness.SUBURBAN,
    val useWinterDesignConditions: Boolean = true,
    val manualWindOverrideMs: Double? = null,
    val selectedLocation: LocationPreset = LocationCatalog.presets.first()
)

data class PowerCurvePoint(
    val windSpeedMs: Double,
    val powerWatts: Double
)

data class BlueprintSpecs(
    val rotorDiameterMm: Int,
    val rotorRadiusMm: Int,
    val bladeLengthMm: Int,
    val rootChordMm: Int,
    val tipChordMm: Int,
    val bladeTwistDeg: Int,
    val hubPcdMm: Int,
    val mastHeightMm: Int,
    val mastPipeOuterMm: Int,
    val mastWallMm: Double,
    val guyWireRadiusMm: Int,
    val guyWireTotalLengthM: Double,
    val foundationSideMm: Int,
    val foundationDepthMm: Int,
    val tailVaneLengthMm: Int,
    val tailFinAreaCm2: Int
)

data class StationCalculationResult(
    val minimumRoomHeatWatts: Int,
    val comfortRoomHeatWatts: Int,
    val activeTargetHeatWatts: Int,
    val outdoorTempUsedC: Double,
    val baseWind10mMs: Double,
    val effectiveWindAtHubMs: Double,
    val airDensityKgM3: Double,
    val sweptAreaSqm: Double,
    val expectedPowerAtHubWindW: Int,
    val ratedPowerAt10MsW: Int,
    val dailyEnergyKwh: Double,
    val dailyHeatNeedKwh: Double,
    val heatingCoveragePercent: Int,
    val recommendedDiameterM: Double,
    val recommendedGeneratorWatts: Int,
    val systemVoltageV: Int,
    val maxCurrentAmps: Double,
    val recommendedWireMm2: Double,
    val recommendedBatteryAh: Int,
    val batteryBankKwh: Double,
    val autonomyHours: Double,
    val rpmAtDesignWind: Int,
    val powerCurve: List<PowerCurvePoint>,
    val blueprintSpecs: BlueprintSpecs
)
