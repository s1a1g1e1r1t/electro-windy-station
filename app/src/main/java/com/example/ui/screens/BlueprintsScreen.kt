package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.BlueprintSpecs
import com.example.domain.model.StationCalculationResult
import com.example.ui.MainUiState
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpecRow
import com.example.ui.theme.BlueprintGridBg
import com.example.ui.theme.BlueprintGridLine
import com.example.ui.theme.BlueprintStrokeAccent
import com.example.ui.theme.BlueprintStrokePrimary
import com.example.ui.theme.BlueprintStrokeThermal
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class BlueprintSheet(val titleUk: String) {
    MAST_ELEVATION("Щогла і ротор"),
    BLADE_GEOMETRY("Профіль лопаті"),
    ELECTRICAL_SCHEME("Електросхема")
}

@Composable
fun BlueprintsScreen(
    state: MainUiState,
    modifier: Modifier = Modifier
) {
    var selectedSheet by remember { mutableStateOf(BlueprintSheet.MAST_ELEVATION) }
    var scale by remember(selectedSheet) { mutableFloatStateOf(1f) }
    var offsetX by remember(selectedSheet) { mutableFloatStateOf(0f) }
    var offsetY by remember(selectedSheet) { mutableFloatStateOf(0f) }

    val specs = state.calculation.blueprintSpecs
    val calc = state.calculation
    val inputs = state.inputs
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val contentMaxWidth = if (maxWidth >= 720.dp) 880.dp else 600.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = contentMaxWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = "Інженерні креслення проєкту",
                    subtitle = "Динамічні розміри під Ø ${inputs.rotorDiameterM} м / щогла ${inputs.mastHeightM.roundToInt()} м (кімната ${inputs.roomAreaSqm.roundToInt()} м²)",
                    icon = Icons.Filled.Architecture
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BlueprintSheet.entries.forEach { sheet ->
                        FilterChip(
                            selected = selectedSheet == sheet,
                            onClick = { selectedSheet = sheet },
                            label = { Text(sheet.titleUk, maxLines = 1) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("blueprint_tab_${sheet.name.lowercase()}")
                        )
                    }
                }
            }

            // Interactive Zoomable CAD Canvas
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(370.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(BlueprintGridBg)
                            .pointerInput(selectedSheet) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(0.85f, 3.2f)
                                    scale = newScale
                                    offsetX = (offsetX + pan.x).coerceIn(-380f, 380f)
                                    offsetY = (offsetY + pan.y).coerceIn(-380f, 380f)
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Background CAD grid
                            drawBlueprintGrid()

                            withTransform({
                                translate(left = offsetX, top = offsetY)
                                scale(
                                    scaleX = scale,
                                    scaleY = scale,
                                    pivot = Offset(size.width / 2f, size.height / 2f)
                                )
                            }) {
                                when (selectedSheet) {
                                    BlueprintSheet.MAST_ELEVATION ->
                                        drawMastAndRotorBlueprint(specs, inputs.mastHeightM, textMeasurer)

                                    BlueprintSheet.BLADE_GEOMETRY ->
                                        drawBladeGeometryBlueprint(specs, textMeasurer)

                                    BlueprintSheet.ELECTRICAL_SCHEME ->
                                        drawElectricalSchematicBlueprint(calc, inputs.roomAreaSqm.roundToInt(), textMeasurer)
                                }
                            }
                        }

                        // CAD sheet title stamp top-left
                        Surface(
                            color = BlueprintGridBg.copy(alpha = 0.85f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "МАСШТАБ ${(scale * 100).roundToInt()}% • ЖЕСТ: ЗУМ / ПЕРЕМІЩЕННЯ",
                                style = MaterialTheme.typography.labelSmall,
                                color = BlueprintStrokeAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Zoom buttons bottom-right
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilledIconButton(
                                onClick = { scale = (scale - 0.35f).coerceAtLeast(0.85f) },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = BlueprintGridLine,
                                    contentColor = BlueprintStrokePrimary
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.ZoomOut, contentDescription = "Зменшити")
                            }
                            FilledIconButton(
                                onClick = {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = BlueprintGridLine,
                                    contentColor = BlueprintStrokeAccent
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.CenterFocusStrong, contentDescription = "Скинути масштаб")
                            }
                            FilledIconButton(
                                onClick = { scale = (scale + 0.35f).coerceAtMost(3.2f) },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = BlueprintGridLine,
                                    contentColor = BlueprintStrokePrimary
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.ZoomIn, contentDescription = "Збільшити")
                            }
                        }
                    }
                }
            }

            // Dimensions & Specs Card matching selected blueprint
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Таблиця допусків і розмірів — ${selectedSheet.titleUk}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        when (selectedSheet) {
                            BlueprintSheet.MAST_ELEVATION -> {
                                SpecRow("Висота щогли (H)", "${specs.mastHeightMm} мм (${inputs.mastHeightM.roundToInt()} м)")
                                SpecRow("Труба щогли (зовн. Ø × стінка)", "Ø ${specs.mastPipeOuterMm} × ${specs.mastWallMm} мм")
                                SpecRow("Розмах ротора (D / R)", "${specs.rotorDiameterMm} мм / R = ${specs.rotorRadiusMm} мм")
                                SpecRow("Фундамент (Ш × Д × Г)", "${specs.foundationSideMm} × ${specs.foundationSideMm} × ${specs.foundationDepthMm} мм")
                                SpecRow("Радіус якорів розтяжок (3×120°)", "${String.format("%.2f", specs.guyWireRadiusMm / 1000.0)} м")
                                SpecRow("Загальна довжина троса Ø5 мм", "${specs.guyWireTotalLengthM.roundToInt()} м")
                                SpecRow("Довжина хвоста / площа пера", "${specs.tailVaneLengthMm} мм / ${specs.tailFinAreaCm2} см²")
                            }
                            BlueprintSheet.BLADE_GEOMETRY -> {
                                SpecRow("Повний радіус ротора (R)", "${specs.rotorRadiusMm} мм")
                                SpecRow("Чиста довжина лопаті (L)", "${specs.bladeLengthMm} мм")
                                SpecRow("Хорда біля кореня (C_root)", "${specs.rootChordMm} мм")
                                SpecRow("Хорда на кінчику (C_tip)", "${specs.tipChordMm} мм")
                                SpecRow("Кут закрутки / установки", "${specs.bladeTwistDeg}° (біля кореня 16° -> 2° на кінці)")
                                SpecRow("Діаметр розболтовки ступиці (PCD)", "${specs.hubPcdMm} мм (6×М8 кл. 8.8)")
                            }
                            BlueprintSheet.ELECTRICAL_SCHEME -> {
                                SpecRow("Генератор (3-фазний PMSG)", "${calc.recommendedGeneratorWatts} Вт / ${calc.systemVoltageV}В AC->DC")
                                SpecRow("Переріз кабелю спуску", "3 × ${calc.recommendedWireMm2.toInt()} мм² (мідь)")
                                SpecRow("Автомат аварійного гальмування", "3P 32A (КЗ фаз через резистор 0.5 Ом)")
                                SpecRow("Буфер АКБ", "${calc.systemVoltageV}В / ${calc.recommendedBatteryAh} А·год")
                                SpecRow("ТЕН обігріву (Dump Load)", "${calc.activeTargetHeatWatts} Вт / ${calc.systemVoltageV}В DC")
                                SpecRow("Захисний контур заземлення", "Опір ≤ 4 Ом (3 штирі по 1.5 м)")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

private fun DrawScope.drawBlueprintGrid() {
    val step = 24.dp.toPx()
    var x = 0f
    while (x <= size.width) {
        drawLine(BlueprintGridLine, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.8f)
        x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(BlueprintGridLine, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.8f)
        y += step
    }
}

private fun DrawScope.drawMastAndRotorBlueprint(
    specs: BlueprintSpecs,
    mastHeightM: Double,
    textMeasurer: TextMeasurer
) {
    val w = size.width
    val h = size.height
    val groundY = h * 0.78f
    val mastX = w * 0.45f
    val topY = h * 0.22f

    val labelStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 10.sp,
        color = BlueprintStrokeAccent,
        fontWeight = FontWeight.Medium
    )
    val thermalStyle = labelStyle.copy(color = BlueprintStrokeThermal)

    // Ground line
    drawLine(BlueprintStrokePrimary.copy(alpha = 0.7f), Offset(w * 0.08f, groundY), Offset(w * 0.92f, groundY), 2f)

    // Foundation concrete block underground
    val fndW = 36.dp.toPx()
    val fndH = 42.dp.toPx()
    drawRect(
        color = BlueprintStrokeAccent.copy(alpha = 0.2f),
        topLeft = Offset(mastX - fndW / 2f, groundY),
        size = Size(fndW, fndH)
    )
    drawRect(
        color = BlueprintStrokePrimary,
        topLeft = Offset(mastX - fndW / 2f, groundY),
        size = Size(fndW, fndH),
        style = Stroke(width = 1.8f)
    )

    // Guy-wire anchors left & right
    val anchorOffset = w * 0.27f
    val leftAnchor = Offset(mastX - anchorOffset, groundY)
    val rightAnchor = Offset(mastX + anchorOffset, groundY)
    drawRect(BlueprintStrokeThermal, Offset(leftAnchor.x - 6f, groundY), Size(12f, 16f), style = Stroke(2f))
    drawRect(BlueprintStrokeThermal, Offset(rightAnchor.x - 6f, groundY), Size(12f, 16f), style = Stroke(2f))

    // Guy wires attached at 78% mast height
    val collarY = groundY - (groundY - topY) * 0.78f
    drawLine(
        color = BlueprintStrokeThermal,
        start = Offset(mastX, collarY),
        end = leftAnchor,
        strokeWidth = 1.8f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
    )
    drawLine(
        color = BlueprintStrokeThermal,
        start = Offset(mastX, collarY),
        end = rightAnchor,
        strokeWidth = 1.8f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
    )

    // Mast steel pipe
    drawLine(BlueprintStrokePrimary, Offset(mastX - 3f, groundY), Offset(mastX - 3f, topY), 2.5f)
    drawLine(BlueprintStrokePrimary, Offset(mastX + 3f, groundY), Offset(mastX + 3f, topY), 2.5f)

    // Nacelle & Tail vane
    val tailEndX = mastX + 58.dp.toPx()
    drawLine(BlueprintStrokePrimary, Offset(mastX - 10.dp.toPx(), topY), Offset(tailEndX, topY), 3f)
    val tailFin = Path().apply {
        moveTo(tailEndX - 18.dp.toPx(), topY)
        lineTo(tailEndX, topY - 18.dp.toPx())
        lineTo(tailEndX, topY + 6.dp.toPx())
        close()
    }
    drawPath(tailFin, BlueprintStrokeAccent, style = Stroke(2f))

    // Rotor swept circle & 3 blades
    val hubCenter = Offset(mastX - 12.dp.toPx(), topY)
    val rotorPx = (specs.rotorRadiusMm / 1000f * 26f).coerceIn(34f, 68f).dp.toPx()
    drawCircle(
        color = BlueprintStrokeAccent.copy(alpha = 0.55f),
        radius = rotorPx,
        center = hubCenter,
        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
    )
    listOf(-90.0, 30.0, 150.0).forEach { deg ->
        val rad = Math.toRadians(deg)
        val tip = Offset(
            hubCenter.x + (rotorPx * cos(rad)).toFloat(),
            hubCenter.y + (rotorPx * sin(rad)).toFloat()
        )
        drawLine(BlueprintStrokePrimary, hubCenter, tip, strokeWidth = 4f)
    }
    drawCircle(BlueprintStrokeThermal, radius = 5.dp.toPx(), center = hubCenter)

    // Dimension lines & labels
    // Mast height dimension on right
    val dimX = w * 0.80f
    drawLine(BlueprintStrokeAccent, Offset(dimX, topY), Offset(dimX, groundY), 1.5f)
    drawLine(BlueprintStrokeAccent, Offset(dimX - 6f, topY), Offset(dimX + 6f, topY), 1.5f)
    drawLine(BlueprintStrokeAccent, Offset(dimX - 6f, groundY), Offset(dimX + 6f, groundY), 1.5f)
    drawText(
        textMeasurer.measure("H=${mastHeightM.roundToInt()}м\nØ${specs.mastPipeOuterMm}мм", labelStyle),
        topLeft = Offset(dimX + 6.dp.toPx(), (topY + groundY) / 2f - 14.dp.toPx())
    )

    // Rotor diameter label
    drawText(
        textMeasurer.measure("Ø Ротора = ${specs.rotorDiameterMm} мм", labelStyle),
        topLeft = Offset(w * 0.06f, topY - rotorPx - 20.dp.toPx())
    )

    // Anchor radius label
    drawLine(BlueprintStrokeThermal, Offset(mastX, groundY + 26.dp.toPx()), Offset(rightAnchor.x, groundY + 26.dp.toPx()), 1.4f)
    drawText(
        textMeasurer.measure("R якорів = ${String.format("%.1f", specs.guyWireRadiusMm / 1000.0)} м", thermalStyle),
        topLeft = Offset(mastX + 10.dp.toPx(), groundY + 30.dp.toPx())
    )

    // Foundation label
    drawText(
        textMeasurer.measure("Фунд. ${specs.foundationSideMm}×${specs.foundationDepthMm}", labelStyle),
        topLeft = Offset(w * 0.06f, groundY + 18.dp.toPx())
    )
}

private fun DrawScope.drawBladeGeometryBlueprint(
    specs: BlueprintSpecs,
    textMeasurer: TextMeasurer
) {
    val w = size.width
    val h = size.height
    val labelStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 10.sp,
        color = BlueprintStrokeAccent,
        fontWeight = FontWeight.Medium
    )
    val thermalStyle = labelStyle.copy(color = BlueprintStrokeThermal)

    // Blade planform horizontally
    val startX = w * 0.16f
    val endX = w * 0.86f
    val midY = h * 0.38f
    val rootHalfH = 38.dp.toPx()
    val tipHalfH = 16.dp.toPx()

    // Hub mounting tongue
    drawRect(
        color = BlueprintStrokeThermal.copy(alpha = 0.25f),
        topLeft = Offset(startX - 28.dp.toPx(), midY - 22.dp.toPx()),
        size = Size(28.dp.toPx(), 44.dp.toPx())
    )
    drawRect(
        color = BlueprintStrokeThermal,
        topLeft = Offset(startX - 28.dp.toPx(), midY - 22.dp.toPx()),
        size = Size(28.dp.toPx(), 44.dp.toPx()),
        style = Stroke(2f)
    )
    // Bolt holes
    drawCircle(BlueprintStrokeThermal, 3.5.dp.toPx(), Offset(startX - 14.dp.toPx(), midY - 11.dp.toPx()))
    drawCircle(BlueprintStrokeThermal, 3.5.dp.toPx(), Offset(startX - 14.dp.toPx(), midY + 11.dp.toPx()))

    // Tapered blade outline
    val bladePath = Path().apply {
        moveTo(startX, midY - rootHalfH)
        lineTo(endX, midY - tipHalfH)
        lineTo(endX, midY + tipHalfH)
        lineTo(startX, midY + rootHalfH)
        close()
    }
    drawPath(bladePath, BlueprintStrokeAccent.copy(alpha = 0.16f))
    drawPath(bladePath, BlueprintStrokePrimary, style = Stroke(2.4f))

    // Center axis line
    drawLine(
        BlueprintStrokeAccent,
        Offset(startX - 36.dp.toPx(), midY),
        Offset(endX + 16.dp.toPx(), midY),
        1.2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
    )

    // Dimension: Blade length
    val dimY = midY - rootHalfH - 22.dp.toPx()
    drawLine(BlueprintStrokeAccent, Offset(startX, dimY), Offset(endX, dimY), 1.6f)
    drawLine(BlueprintStrokeAccent, Offset(startX, dimY - 6f), Offset(startX, midY - rootHalfH), 1.2f)
    drawLine(BlueprintStrokeAccent, Offset(endX, dimY - 6f), Offset(endX, midY - tipHalfH), 1.2f)
    drawText(
        textMeasurer.measure("Довжина L = ${specs.bladeLengthMm} мм", labelStyle),
        topLeft = Offset((startX + endX) / 2f - 62.dp.toPx(), dimY - 18.dp.toPx())
    )

    // Root chord & Tip chord annotations
    drawText(
        textMeasurer.measure("Корінь\n${specs.rootChordMm} мм", thermalStyle),
        topLeft = Offset(startX + 4.dp.toPx(), midY + rootHalfH + 8.dp.toPx())
    )
    drawText(
        textMeasurer.measure("Кінчик\n${specs.tipChordMm} мм", thermalStyle),
        topLeft = Offset(endX - 48.dp.toPx(), midY + tipHalfH + 14.dp.toPx())
    )

    // Airfoil PVC Arc Cross-section at bottom
    val secCenter = Offset(w * 0.30f, h * 0.76f)
    drawArc(
        color = BlueprintStrokePrimary,
        startAngle = 210f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = Offset(secCenter.x - 50.dp.toPx(), secCenter.y - 25.dp.toPx()),
        size = Size(100.dp.toPx(), 75.dp.toPx()),
        style = Stroke(width = 4.dp.toPx())
    )
    drawText(
        textMeasurer.measure("Переріз ПВХ (дуга ~95°)\nЗакрутка: ${specs.bladeTwistDeg}°", labelStyle),
        topLeft = Offset(w * 0.48f, h * 0.71f)
    )
}

private fun DrawScope.drawElectricalSchematicBlueprint(
    calc: StationCalculationResult,
    roomAreaSqm: Int,
    textMeasurer: TextMeasurer
) {
    val w = size.width
    val h = size.height
    val labelStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 9.5.sp,
        color = BlueprintStrokePrimary,
        fontWeight = FontWeight.Medium
    )
    val accentStyle = labelStyle.copy(color = BlueprintStrokeAccent)
    val thermalStyle = labelStyle.copy(color = BlueprintStrokeThermal)

    fun drawBlock(x: Float, y: Float, bw: Float, bh: Float, title: String, sub: String, borderColor: Color) {
        drawRect(
            color = borderColor.copy(alpha = 0.16f),
            topLeft = Offset(x, y),
            size = Size(bw, bh)
        )
        drawRect(
            color = borderColor,
            topLeft = Offset(x, y),
            size = Size(bw, bh),
            style = Stroke(2f)
        )
        drawText(textMeasurer.measure(title, labelStyle), topLeft = Offset(x + 8.dp.toPx(), y + 8.dp.toPx()))
        drawText(textMeasurer.measure(sub, accentStyle), topLeft = Offset(x + 8.dp.toPx(), y + 24.dp.toPx()))
    }

    val bw = w * 0.36f
    val bh = 54.dp.toPx()

    // 1. 3-Phase Generator (Top Left)
    val genX = w * 0.08f
    val row1Y = h * 0.16f
    drawBlock(genX, row1Y, bw, bh, "PMSG ГЕНЕРАТОР", "${calc.recommendedGeneratorWatts}Вт • 3Ф AC", BlueprintStrokeAccent)

    // 2. 3-Pole Brake & Rectifier (Top Right)
    val brkX = w * 0.56f
    drawBlock(brkX, row1Y, bw, bh, "АВТОМАТ 3P + МІСТ", "Гальмо КЗ • ${calc.recommendedWireMm2.toInt()}мм²", BlueprintStrokePrimary)

    // 3-phase lines Gen -> Brake
    listOf(-8f, 0f, 8f).forEach { dy ->
        drawLine(
            BlueprintStrokeAccent,
            Offset(genX + bw, row1Y + bh / 2f + dy),
            Offset(brkX, row1Y + bh / 2f + dy),
            2f
        )
    }

    // 3. MPPT Wind Controller (Middle)
    val row2Y = h * 0.44f
    drawBlock(brkX, row2Y, bw, bh, "MPPT КОНТРОЛЕР", "${calc.systemVoltageV}В + Dump Load", BlueprintStrokeAccent)
    // Line Brake -> MPPT (+/-)
    drawLine(BlueprintStrokeThermal, Offset(brkX + bw * 0.35f, row1Y + bh), Offset(brkX + bw * 0.35f, row2Y), 2.5f)
    drawLine(BlueprintStrokePrimary, Offset(brkX + bw * 0.65f, row1Y + bh), Offset(brkX + bw * 0.65f, row2Y), 2.5f)

    // 4. Battery Bank (Middle Left)
    drawBlock(genX, row2Y, bw, bh, "АКБ БУФЕР", "${calc.systemVoltageV}В ${calc.recommendedBatteryAh}А·год", BlueprintStrokePrimary)
    // Line MPPT -> Battery
    drawLine(BlueprintStrokeThermal, Offset(brkX, row2Y + bh * 0.35f), Offset(genX + bw, row2Y + bh * 0.35f), 2.5f)
    drawLine(BlueprintStrokePrimary, Offset(brkX, row2Y + bh * 0.65f), Offset(genX + bw, row2Y + bh * 0.65f), 2.5f)

    // 5. Room Heater / Dump Load (Bottom Right)
    val row3Y = h * 0.72f
    drawBlock(
        brkX,
        row3Y,
        bw,
        bh,
        "ТЕН КІМНАТИ ${roomAreaSqm}м²",
        "${calc.activeTargetHeatWatts}Вт ${calc.systemVoltageV}В DC",
        BlueprintStrokeThermal
    )
    // Line MPPT -> Heater
    drawLine(BlueprintStrokeThermal, Offset(brkX + bw * 0.5f, row2Y + bh), Offset(brkX + bw * 0.5f, row3Y), 3f)

    // 6. Grounding symbol bottom-left
    val gndX = genX + bw * 0.45f
    val gndY = row3Y + 14.dp.toPx()
    drawLine(BlueprintStrokeAccent, Offset(gndX, row2Y + bh), Offset(gndX, gndY), 2f)
    drawLine(BlueprintStrokeAccent, Offset(gndX - 16.dp.toPx(), gndY), Offset(gndX + 16.dp.toPx(), gndY), 2.5f)
    drawLine(BlueprintStrokeAccent, Offset(gndX - 10.dp.toPx(), gndY + 6.dp.toPx()), Offset(gndX + 10.dp.toPx(), gndY + 6.dp.toPx()), 2f)
    drawLine(BlueprintStrokeAccent, Offset(gndX - 5.dp.toPx(), gndY + 12.dp.toPx()), Offset(gndX + 5.dp.toPx(), gndY + 12.dp.toPx()), 2f)
    drawText(textMeasurer.measure("Заземлення ≤4 Ом", labelStyle), topLeft = Offset(genX, gndY + 18.dp.toPx()))

    // Fuse note
    drawText(
        textMeasurer.measure("Запобіжник DC 63A", thermalStyle),
        topLeft = Offset(genX + bw + 4.dp.toPx(), row2Y - 16.dp.toPx())
    )
}
