package com.example

import com.example.domain.WindPhysicsCalculator
import com.example.domain.model.CalculatorInputs
import com.example.domain.model.HeatingMode
import com.example.domain.model.InsulationLevel
import com.example.domain.model.LiveWeatherState
import com.example.domain.model.LocationCatalog
import com.example.domain.model.TurbineType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WindPhysicsCalculatorTest {

    @Test
    fun default14SqmRoom_producesValidMinimumHeatingAndSizing() {
        val inputs = CalculatorInputs(
            roomAreaSqm = 14.0,
            insulation = InsulationLevel.STANDARD,
            heatingMode = HeatingMode.MINIMUM,
            turbineType = TurbineType.HAWT_3_BLADE,
            selectedLocation = LocationCatalog.presets.first()
        )
        val result = WindPhysicsCalculator.calculate(inputs, LiveWeatherState())

        assertTrue("Minimum room heat should be positive", result.minimumRoomHeatWatts > 300)
        assertTrue("Comfort heat should exceed minimum heat", result.comfortRoomHeatWatts > result.minimumRoomHeatWatts)
        assertTrue("Recommended diameter should be in realistic bounds", result.recommendedDiameterM in 1.4..5.0)
        assertEquals(15, result.powerCurve.size)
        assertEquals(0.0, result.powerCurve.first().powerWatts, 0.01)
    }

    @Test
    fun higherWindLocation_recommendsSmallerRotorThanLowWindLocation() {
        val odesa = LocationCatalog.presets.first { it.id == "odesa" } // 5.6 m/s
        val uzhhorod = LocationCatalog.presets.first { it.id == "uzhhorod" } // 3.8 m/s

        val resOdesa = WindPhysicsCalculator.calculate(
            CalculatorInputs(selectedLocation = odesa),
            LiveWeatherState()
        )
        val resUzhhorod = WindPhysicsCalculator.calculate(
            CalculatorInputs(selectedLocation = uzhhorod),
            LiveWeatherState()
        )

        assertTrue(
            "Windier location (${odesa.nameUk}) should require smaller rotor than calmer location (${uzhhorod.nameUk})",
            resOdesa.recommendedDiameterM <= resUzhhorod.recommendedDiameterM
        )
    }

    @Test
    fun higherMastHeight_increasesEffectiveHubWind() {
        val lowMast = WindPhysicsCalculator.calculate(
            CalculatorInputs(mastHeightM = 5.0),
            LiveWeatherState()
        )
        val tallMast = WindPhysicsCalculator.calculate(
            CalculatorInputs(mastHeightM = 14.0),
            LiveWeatherState()
        )

        assertTrue(tallMast.effectiveWindAtHubMs > lowMast.effectiveWindAtHubMs)
        assertTrue(tallMast.expectedPowerAtHubWindW > lowMast.expectedPowerAtHubWindW)
    }
}
