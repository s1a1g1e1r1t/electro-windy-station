package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.model.HeatingMode
import com.example.domain.model.InsulationLevel
import com.example.domain.model.LocationCatalog
import com.example.domain.model.LocationPreset
import com.example.domain.model.TerrainRoughness
import com.example.domain.model.TurbineType
import com.example.ui.MainUiState
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.PowerCurveChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpecRow
import com.example.ui.theme.LocalVitroColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalculatorScreen(
    state: MainUiState,
    onRoomAreaChange: (Double) -> Unit,
    onCeilingHeightChange: (Double) -> Unit,
    onInsulationChange: (InsulationLevel) -> Unit,
    onHeatingModeChange: (HeatingMode) -> Unit,
    onTurbineTypeChange: (TurbineType) -> Unit,
    onRotorDiameterChange: (Double) -> Unit,
    onMastHeightChange: (Double) -> Unit,
    onTerrainChange: (TerrainRoughness) -> Unit,
    onWinterDesignToggle: (Boolean) -> Unit,
    onManualWindChange: (Double?) -> Unit,
    onAutoSizeForRoom: () -> Unit,
    onSelectLocation: (LocationPreset) -> Unit,
    onDetectGps: () -> Unit,
    onRefreshWeather: () -> Unit,
    onClearGpsMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vitroColors = LocalVitroColors.current
    var showCitySheet by remember { mutableStateOf(false) }
    var permissionDeniedHint by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedHint = false
            onDetectGps()
        } else {
            permissionDeniedHint = true
        }
    }

    val inputs = state.inputs
    val calc = state.calculation
    val weather = state.weather

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth >= 720.dp
        val contentMaxWidth = if (isWide) 880.dp else 600.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = contentMaxWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HERO BANNER CARD
            item {
                HeroProjectCard(
                    roomAreaSqm = inputs.roomAreaSqm,
                    minimumHeatWatts = calc.minimumRoomHeatWatts,
                    recommendedDiameterM = calc.recommendedDiameterM,
                    coveragePercent = calc.heatingCoveragePercent
                )
            }

            // 2. LOCATION & LIVE WEATHER CARD
            item {
                LocationWeatherCard(
                    inputs = inputs,
                    calc = calc,
                    weather = weather,
                    isDetectingGps = state.isDetectingGps,
                    gpsError = state.gpsMessage ?: if (permissionDeniedHint) {
                        "Доступ до геолокації відхилено. Оберіть місто вручну."
                    } else null,
                    onOpenCityPicker = { showCitySheet = true },
                    onGpsClick = {
                        onClearGpsMessage()
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            onDetectGps()
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    },
                    onRefreshWeather = onRefreshWeather,
                    onWinterDesignToggle = onWinterDesignToggle
                )
            }

            // 3. KPI SUMMARY GRID
            item {
                SectionHeader(
                    title = "Енергобаланс кімнати ${inputs.roomAreaSqm.roundToInt()} м²",
                    subtitle = "Мінімальні потреби опалення та параметри вітряка",
                    icon = Icons.Filled.Bolt
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            label = "Потреба тепла (${inputs.heatingMode.targetTempC.toInt()}°C)",
                            value = "${calc.activeTargetHeatWatts} Вт",
                            unitOrNote = "Мін. режим: ${calc.minimumRoomHeatWatts} Вт • ${String.format("%.1f", calc.dailyHeatNeedKwh)} кВт·год/добу",
                            icon = Icons.Filled.Thermostat,
                            accentColor = vitroColors.thermalAccent,
                            containerColor = vitroColors.thermalContainer,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            label = "Рекомендований ротор",
                            value = "Ø ${calc.recommendedDiameterM} м",
                            unitOrNote = "Генератор ${calc.recommendedGeneratorWatts} Вт / ${calc.systemVoltageV}В",
                            icon = Icons.Filled.Air,
                            accentColor = vitroColors.windAccent,
                            containerColor = vitroColors.windContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            label = "Виробіток при ${String.format("%.1f", calc.effectiveWindAtHubMs)} м/с",
                            value = "${calc.expectedPowerAtHubWindW} Вт",
                            unitOrNote = "${String.format("%.1f", calc.dailyEnergyKwh)} кВт·год/добу (${calc.heatingCoveragePercent}% потреби)",
                            icon = Icons.Filled.Bolt,
                            accentColor = if (calc.heatingCoveragePercent >= 85) vitroColors.successAccent else vitroColors.windAccent,
                            containerColor = vitroColors.windContainer,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            label = "Буфер АКБ та автономія",
                            value = "${calc.recommendedBatteryAh} А·год",
                            unitOrNote = "${calc.systemVoltageV}В (${String.format("%.1f", calc.batteryBankKwh)} кВт·год) • ~${String.format("%.1f", calc.autonomyHours)} год без вітру",
                            icon = Icons.Filled.BatteryChargingFull,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Auto-size CTA banner
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Підбір під мінімальне опалення",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Для ${inputs.roomAreaSqm.roundToInt()} м² у вашій локації оптимальний ротор Ø ${calc.recommendedDiameterM} м",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = onAutoSizeForRoom,
                            modifier = Modifier.testTag("auto_size_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Застосувати")
                        }
                    }
                }
            }

            // 4. INTERACTIVE POWER CURVE CHART
            item {
                PowerCurveChart(
                    points = calc.powerCurve,
                    targetHeatWatts = calc.activeTargetHeatWatts,
                    localWindMs = calc.effectiveWindAtHubMs,
                    cutInMs = inputs.turbineType.cutInMs
                )
            }

            // 5. ROOM & THERMAL CONTROLS
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionHeader(
                            title = "Параметри кімнати для обігріву",
                            subtitle = "Налаштуйте площу, температуру та утеплення",
                            icon = Icons.Outlined.Home
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Площа кімнати: ${String.format("%.1f", inputs.roomAreaSqm)} м²",
                                style = MaterialTheme.typography.titleSmall
                            )
                            FilterChip(
                                selected = inputs.roomAreaSqm == 14.0,
                                onClick = { onRoomAreaChange(14.0) },
                                label = { Text("Стандарт 14 м²") },
                                modifier = Modifier.testTag("preset_14_sqm_chip")
                            )
                        }
                        Slider(
                            value = inputs.roomAreaSqm.toFloat(),
                            onValueChange = { onRoomAreaChange(it.toDouble()) },
                            valueRange = 6f..40f,
                            steps = 67,
                            colors = SliderDefaults.colors(
                                thumbColor = vitroColors.thermalAccent,
                                activeTrackColor = vitroColors.thermalAccent
                            ),
                            modifier = Modifier.testTag("room_area_slider")
                        )

                        Text(
                            text = "Висота стелі: ${String.format("%.1f", inputs.ceilingHeightM)} м",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Slider(
                            value = inputs.ceilingHeightM.toFloat(),
                            onValueChange = { onCeilingHeightChange(it.toDouble()) },
                            valueRange = 2.2f..3.5f,
                            steps = 12
                        )

                        Text(
                            text = "Режим опалення",
                            style = MaterialTheme.typography.titleSmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HeatingMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = inputs.heatingMode == mode,
                                    onClick = { onHeatingModeChange(mode) },
                                    label = { Text(mode.titleUk) },
                                    leadingIcon = if (inputs.heatingMode == mode) {
                                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        Text(
                            text = "Утеплення приміщення",
                            style = MaterialTheme.typography.titleSmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InsulationLevel.entries.forEach { level ->
                                FilterChip(
                                    selected = inputs.insulation == level,
                                    onClick = { onInsulationChange(level) },
                                    label = { Text(level.titleUk) },
                                    leadingIcon = if (inputs.insulation == level) {
                                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                        Text(
                            text = inputs.insulation.subtitleUk,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 6. WIND TURBINE & MAST INTERACTIVE CALCULATOR
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionHeader(
                            title = "Конфігурація вітрогенератора",
                            subtitle = "Інтерактивний підбір ротора, щогли та тестування вітру",
                            icon = Icons.Filled.Tune
                        )

                        Text(
                            text = "Тип конструкції ротора",
                            style = MaterialTheme.typography.titleSmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TurbineType.entries.forEach { type ->
                                FilterChip(
                                    selected = inputs.turbineType == type,
                                    onClick = { onTurbineTypeChange(type) },
                                    label = { Text(type.shortTitleUk) },
                                    leadingIcon = if (inputs.turbineType == type) {
                                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                        Text(
                            text = inputs.turbineType.descriptionUk,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Діаметр ротора: ${String.format("%.1f", inputs.rotorDiameterM)} м",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Площа: ${String.format("%.2f", calc.sweptAreaSqm)} м²",
                                style = MaterialTheme.typography.labelMedium,
                                color = vitroColors.windAccent
                            )
                        }
                        Slider(
                            value = inputs.rotorDiameterM.toFloat(),
                            onValueChange = { onRotorDiameterChange(it.toDouble()) },
                            valueRange = 1.2f..4.6f,
                            steps = 33,
                            modifier = Modifier.testTag("rotor_diameter_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Висота щогли: ${inputs.mastHeightM.roundToInt()} м",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Вітер на осі: ${String.format("%.1f", calc.effectiveWindAtHubMs)} м/с",
                                style = MaterialTheme.typography.labelMedium,
                                color = vitroColors.windAccent
                            )
                        }
                        Slider(
                            value = inputs.mastHeightM.toFloat(),
                            onValueChange = { onMastHeightChange(it.toDouble()) },
                            valueRange = 4f..15f,
                            steps = 10,
                            modifier = Modifier.testTag("mast_height_slider")
                        )

                        Text(
                            text = "Рельєф забудови (впливає на підйом вітру)",
                            style = MaterialTheme.typography.titleSmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TerrainRoughness.entries.forEach { t ->
                                FilterChip(
                                    selected = inputs.terrain == t,
                                    onClick = { onTerrainChange(t) },
                                    label = { Text(t.titleUk) }
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (inputs.manualWindOverrideMs != null) {
                                    "Тестовий вітер (10м): ${String.format("%.1f", inputs.manualWindOverrideMs)} м/с"
                                } else {
                                    "Тест поривів вітру (зараз: з локації)"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (inputs.manualWindOverrideMs != null) {
                                TextButton(onClick = { onManualWindChange(null) }) {
                                    Text("Скинути")
                                }
                            }
                        }
                        Slider(
                            value = (inputs.manualWindOverrideMs ?: calc.baseWind10mMs).toFloat().coerceIn(2f, 14f),
                            onValueChange = { onManualWindChange(it.toDouble()) },
                            valueRange = 2f..14f,
                            steps = 23,
                            modifier = Modifier.testTag("wind_test_slider")
                        )
                    }
                }
            }

            // 7. TECHNICAL TELEMETRY SUMMARY CARD
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(
                            title = "Зведені інженерні параметри",
                            icon = Icons.Outlined.Info
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SpecRow("Швидкість старту (Cut-in)", "${inputs.turbineType.cutInMs} м/с")
                        SpecRow("Оберти при поточному вітрі", "${calc.rpmAtDesignWind} об/хв")
                        SpecRow("Номінал при 10 м/с", "${calc.ratedPowerAt10MsW} Вт")
                        SpecRow("Густина повітря (при ${String.format("%.1f", calc.outdoorTempUsedC)}°C)", "${String.format("%.3f", calc.airDensityKgM3)} кг/м³")
                        SpecRow("Макс. струм лінії (${calc.systemVoltageV}В)", "${String.format("%.1f", calc.maxCurrentAmps)} А")
                        SpecRow("Переріз мідного кабелю", "3 × ${calc.recommendedWireMm2.toInt()} мм²")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showCitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCitySheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Оберіть населений пункт / регіон",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                Text(
                    text = "Підтягнемо середні зимові параметри та живі дані вітру Open-Meteo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(380.dp)) {
                    items(LocationCatalog.presets) { preset ->
                        val isSelected = preset.id == inputs.selectedLocation.id
                        ListItem(
                            headlineContent = { Text(preset.nameUk, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            supportingContent = {
                                Text("${preset.regionUk} • Сер. вітер ${preset.avgWinterWind10mMs} м/с • Зима ${preset.avgWinterTempC}°C")
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = if (isSelected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                            modifier = Modifier.clickable {
                                onSelectLocation(preset)
                                showCitySheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroProjectCard(
    roomAreaSqm: Double,
    minimumHeatWatts: Int,
    recommendedDiameterM: Double,
    coveragePercent: Int
) {
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_wind_home),
                contentDescription = stringResource(R.string.hero_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC091828),
                                Color(0x990B192C),
                                Color(0xEB091828)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF00838F),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "КІМНАТА ${roomAreaSqm.roundToInt()} м² • ВЕС",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFFD95B00),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Покриття $coveragePercent%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.hero_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = "Мінімум для обігріву: $minimumHeatWatts Вт • Оптимальний ротор Ø $recommendedDiameterM м",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD5E6F5)
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationWeatherCard(
    inputs: com.example.domain.model.CalculatorInputs,
    calc: com.example.domain.model.StationCalculationResult,
    weather: com.example.domain.model.LiveWeatherState,
    isDetectingGps: Boolean,
    gpsError: String?,
    onOpenCityPicker: () -> Unit,
    onGpsClick: () -> Unit,
    onRefreshWeather: () -> Unit,
    onWinterDesignToggle: (Boolean) -> Unit
) {
    val vitroColors = LocalVitroColors.current
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = vitroColors.windContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = vitroColors.windAccent,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Column {
                        Text(
                            text = inputs.selectedLocation.nameUk,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${inputs.selectedLocation.regionUk} • ${weather.sourceLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onRefreshWeather, modifier = Modifier.testTag("refresh_weather_btn")) {
                    if (weather.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Оновити дані вітру")
                    }
                }
            }

            if (weather.isLoading || isDetectingGps) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Вітер (10м)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${String.format("%.1f", calc.baseWind10mMs)} м/с",
                            style = MaterialTheme.typography.titleMedium,
                            color = vitroColors.windAccent
                        )
                        weather.currentWind10mMs?.let { liveW ->
                            Text(
                                text = "Зараз: ${String.format("%.1f", liveW)} м/с",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Темп. ззовні", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${String.format("%.1f", calc.outdoorTempUsedC)}°C",
                            style = MaterialTheme.typography.titleMedium,
                            color = vitroColors.thermalAccent
                        )
                        weather.currentTempC?.let { liveT ->
                            Text(
                                text = "Зараз: ${String.format("%.1f", liveT)}°C",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = inputs.useWinterDesignConditions,
                    onClick = { onWinterDesignToggle(true) },
                    label = { Text("Зимова норма") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !inputs.useWinterDesignConditions,
                    onClick = { onWinterDesignToggle(false) },
                    label = { Text("Поточна погода") },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenCityPicker,
                    modifier = Modifier.weight(1f).testTag("pick_city_button")
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Обрати місто")
                }
                Button(
                    onClick = onGpsClick,
                    enabled = !isDetectingGps,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f).testTag("detect_gps_button")
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isDetectingGps) "Пошук..." else "Моє GPS")
                }
            }

            AnimatedVisibility(visible = gpsError != null || weather.errorMessage != null) {
                Text(
                    text = gpsError ?: weather.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = vitroColors.thermalAccent
                )
            }
        }
    }
}
