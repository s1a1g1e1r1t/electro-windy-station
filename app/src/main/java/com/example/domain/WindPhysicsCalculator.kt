package com.example.domain

import com.example.domain.model.BlueprintSpecs
import com.example.domain.model.CalculatorInputs
import com.example.domain.model.HeatingMode
import com.example.domain.model.LiveWeatherState
import com.example.domain.model.PowerCurvePoint
import com.example.domain.model.StationCalculationResult
import com.example.domain.model.TurbineType
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

object WindPhysicsCalculator {

    private const val GENERATOR_EFFICIENCY = 0.84
    private const val CONTROLLER_EFFICIENCY = 0.93
    private const val WEIBULL_ENERGY_FACTOR = 1.55 // Accounts for cubic wind distribution vs mean speed
    private const val REFERENCE_DELTA_T = 35.0

    fun calculate(
        inputs: CalculatorInputs,
        weather: LiveWeatherState
    ): StationCalculationResult {
        val outdoorTempC = if (inputs.useWinterDesignConditions) {
            inputs.selectedLocation.avgWinterTempC
        } else {
            weather.currentTempC ?: inputs.selectedLocation.avgWinterTempC
        }

        val baseWind10m = inputs.manualWindOverrideMs
            ?: if (inputs.useWinterDesignConditions) {
                weather.weekMeanWind10mMs?.let { (it + inputs.selectedLocation.avgWinterWind10mMs) / 2.0 }
                    ?: inputs.selectedLocation.avgWinterWind10mMs
            } else {
                weather.currentWind10mMs ?: inputs.selectedLocation.avgWinterWind10mMs
            }

        // Hellmann power law for wind shear at mast height
        val heightFactor = (inputs.mastHeightM / 10.0).pow(inputs.terrain.alphaExponent)
        val effectiveWindHub = (baseWind10m * heightFactor).coerceIn(1.0, 22.0)

        // Ideal gas law air density adjusted for cold weather (denser air yields more energy)
        val airDensity = 101325.0 / (287.05 * (outdoorTempC + 273.15))

        // Thermal losses for the room (default 14 m²)
        val heightRatio = inputs.ceilingHeightM / 2.6
        val minDeltaT = max(4.0, HeatingMode.MINIMUM.targetTempC - outdoorTempC)
        val comfortDeltaT = max(6.0, HeatingMode.COMFORT.targetTempC - outdoorTempC)
        val activeDeltaT = max(4.0, inputs.heatingMode.targetTempC - outdoorTempC)

        val baseSpecificW = inputs.insulation.wattsPerSqmAt35Delta
        val minimumHeatW = (inputs.roomAreaSqm * baseSpecificW * heightRatio * (minDeltaT / REFERENCE_DELTA_T)).roundToInt()
        val comfortHeatW = (inputs.roomAreaSqm * baseSpecificW * heightRatio * (comfortDeltaT / REFERENCE_DELTA_T)).roundToInt()
        val activeHeatW = (inputs.roomAreaSqm * baseSpecificW * heightRatio * (activeDeltaT / REFERENCE_DELTA_T)).roundToInt()

        val totalEfficiency = inputs.turbineType.cp * GENERATOR_EFFICIENCY * CONTROLLER_EFFICIENCY
        val sweptArea = PI * (inputs.rotorDiameterM / 2.0).pow(2)

        val powerAtHubWind = computeTurbinePowerWatts(
            windMs = effectiveWindHub,
            sweptAreaSqm = sweptArea,
            airDensity = airDensity,
            turbineType = inputs.turbineType
        )
        val ratedPower10Ms = computeTurbinePowerWatts(
            windMs = 10.0,
            sweptAreaSqm = sweptArea,
            airDensity = airDensity,
            turbineType = inputs.turbineType
        )

        // Recommended rotor diameter to meet active minimum room heat demand at local wind
        // Note: We target realistic design wind = max(effectiveWindHub, 5.2 m/s) or actual effectiveWindHub
        val designWindForSizing = max(effectiveWindHub, 4.2)
        val requiredSweptArea = activeHeatW.toDouble() /
            (0.5 * airDensity * designWindForSizing.pow(3) * totalEfficiency * WEIBULL_ENERGY_FACTOR)
        val rawRecommendedDiameter = 2.0 * sqrt(requiredSweptArea / PI)
        val recommendedDiameter = ((rawRecommendedDiameter * 10.0).roundToInt() / 10.0).coerceIn(1.4, 5.0)

        // Recommended generator step based on rated power at 10-11 m/s
        val recommendedGenRaw = max(ratedPower10Ms, (activeHeatW * 1.35).roundToInt())
        val standardGeneratorRatings = listOf(400, 600, 800, 1000, 1200, 1500, 2000, 2500, 3000)
        val recommendedGeneratorW = standardGeneratorRatings.firstOrNull { it >= recommendedGenRaw } ?: 3000

        val systemVoltage = if (recommendedGeneratorW <= 1200) 24 else 48
        val maxCurrent = recommendedGeneratorW.toDouble() / systemVoltage.toDouble()
        val recommendedWireMm2 = when {
            maxCurrent <= 20.0 -> 4.0
            maxCurrent <= 35.0 -> 6.0
            maxCurrent <= 55.0 -> 10.0
            else -> 16.0
        }

        // Daily yield & coverage
        val avgContinuousPowerW = if (effectiveWindHub < inputs.turbineType.cutInMs) {
            powerAtHubWind * 0.45
        } else {
            min(recommendedGeneratorW.toDouble(), powerAtHubWind * WEIBULL_ENERGY_FACTOR)
        }
        val dailyEnergyKwh = ((avgContinuousPowerW * 24.0) / 1000.0 * 0.85)
        val dailyHeatNeedKwh = (activeHeatW * 24.0) / 1000.0
        val coveragePct = ((dailyEnergyKwh / max(0.1, dailyHeatNeedKwh)) * 100.0)
            .roundToInt()
            .coerceIn(0, 150)

        // Battery buffer for ~8 hours of room heating during calm periods
        val neededBufferWh = activeHeatW * 8.0
        val rawAh = neededBufferWh / (systemVoltage * 0.8) // 80% DoD for LiFePO4 / deep-cycle
        val standardAhSteps = listOf(100, 150, 200, 250, 300, 400)
        val recommendedBatteryAh = standardAhSteps.firstOrNull { it >= rawAh } ?: 400
        val batteryBankKwh = (recommendedBatteryAh * systemVoltage) / 1000.0
        val autonomyHours = (batteryBankKwh * 0.85 * 1000.0) / max(100, activeHeatW)

        // RPM at hub wind: RPM = (60 * TSR * V) / (PI * D)
        val rpm = ((60.0 * inputs.turbineType.tipSpeedRatio * effectiveWindHub) / (PI * inputs.rotorDiameterM))
            .roundToInt()

        val curve = (0..14).map { v ->
            val p = computeTurbinePowerWatts(
                windMs = v.toDouble(),
                sweptAreaSqm = sweptArea,
                airDensity = airDensity,
                turbineType = inputs.turbineType
            )
            PowerCurvePoint(windSpeedMs = v.toDouble(), powerWatts = p.toDouble())
        }

        val blueprintSpecs = buildBlueprintSpecs(inputs)

        return StationCalculationResult(
            minimumRoomHeatWatts = minimumHeatW,
            comfortRoomHeatWatts = comfortHeatW,
            activeTargetHeatWatts = activeHeatW,
            outdoorTempUsedC = outdoorTempC,
            baseWind10mMs = baseWind10m,
            effectiveWindAtHubMs = effectiveWindHub,
            airDensityKgM3 = airDensity,
            sweptAreaSqm = sweptArea,
            expectedPowerAtHubWindW = powerAtHubWind,
            ratedPowerAt10MsW = ratedPower10Ms,
            dailyEnergyKwh = dailyEnergyKwh,
            dailyHeatNeedKwh = dailyHeatNeedKwh,
            heatingCoveragePercent = coveragePct,
            recommendedDiameterM = recommendedDiameter,
            recommendedGeneratorWatts = recommendedGeneratorW,
            systemVoltageV = systemVoltage,
            maxCurrentAmps = maxCurrent,
            recommendedWireMm2 = recommendedWireMm2,
            recommendedBatteryAh = recommendedBatteryAh,
            batteryBankKwh = batteryBankKwh,
            autonomyHours = autonomyHours,
            rpmAtDesignWind = rpm,
            powerCurve = curve,
            blueprintSpecs = blueprintSpecs
        )
    }

    private fun computeTurbinePowerWatts(
        windMs: Double,
        sweptAreaSqm: Double,
        airDensity: Double,
        turbineType: TurbineType
    ): Int {
        if (windMs < turbineType.cutInMs) return 0
        val cappedWind = min(windMs, turbineType.ratedMs + 2.0)
        val efficiency = turbineType.cp * GENERATOR_EFFICIENCY * CONTROLLER_EFFICIENCY
        val raw = 0.5 * airDensity * sweptAreaSqm * cappedWind.pow(3) * efficiency
        return raw.roundToInt()
    }

    private fun buildBlueprintSpecs(inputs: CalculatorInputs): BlueprintSpecs {
        val dMm = (inputs.rotorDiameterM * 1000).roundToInt()
        val rMm = dMm / 2
        val hubRadiusMm = (dMm * 0.08).roundToInt().coerceIn(90, 180)
        val bladeLenMm = rMm - hubRadiusMm
        val rootChord = (dMm * 0.095).roundToInt().coerceIn(130, 320)
        val tipChord = (rootChord * 0.42).roundToInt().coerceIn(55, 140)
        val mastMm = (inputs.mastHeightM * 1000).roundToInt()
        val mastPipeMm = when {
            inputs.mastHeightM <= 7.0 && inputs.rotorDiameterM <= 2.4 -> 57
            inputs.mastHeightM <= 10.0 && inputs.rotorDiameterM <= 3.2 -> 76
            else -> 89
        }
        val guyRadiusMm = (mastMm * 0.55).roundToInt()
        val guyWireSingleM = sqrt((inputs.mastHeightM * 0.78).pow(2) + (guyRadiusMm / 1000.0).pow(2))
        val totalGuyWireM = ceil(guyWireSingleM * 3.0 * 1.15) // 3 anchors + 15% loops/turnbuckles
        val foundationSide = when {
            inputs.rotorDiameterM <= 2.2 -> 700
            inputs.rotorDiameterM <= 3.0 -> 850
            else -> 1000
        }
        val foundationDepth = when {
            inputs.mastHeightM <= 8.0 -> 1100
            else -> 1400
        }
        val tailLength = (rMm * 0.95).roundToInt().coerceIn(850, 1800)
        val tailFinArea = ((dMm / 1000.0).pow(2) * 320).roundToInt().coerceIn(900, 3200)

        return BlueprintSpecs(
            rotorDiameterMm = dMm,
            rotorRadiusMm = rMm,
            bladeLengthMm = bladeLenMm,
            rootChordMm = rootChord,
            tipChordMm = tipChord,
            bladeTwistDeg = if (inputs.turbineType == TurbineType.HAWT_3_BLADE) 14 else 0,
            hubPcdMm = (hubRadiusMm * 1.35).roundToInt(),
            mastHeightMm = mastMm,
            mastPipeOuterMm = mastPipeMm,
            mastWallMm = if (mastPipeMm >= 76) 4.0 else 3.5,
            guyWireRadiusMm = guyRadiusMm,
            guyWireTotalLengthM = totalGuyWireM,
            foundationSideMm = foundationSide,
            foundationDepthMm = foundationDepth,
            tailVaneLengthMm = tailLength,
            tailFinAreaCm2 = tailFinArea
        )
    }
}
