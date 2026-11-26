package com.example.data.catalog

import com.example.domain.model.CalculatorInputs
import com.example.domain.model.StationCalculationResult
import com.example.domain.model.TurbineType
import kotlin.math.ceil
import kotlin.math.roundToInt

enum class MaterialCategory(val titleUk: String) {
    ELECTRICAL("Електрика та опалення"),
    ROTOR("Ротор та лопаті"),
    MAST("Щогла та розтяжки"),
    FOUNDATION("Фундамент та захист")
}

data class MaterialItem(
    val id: String,
    val category: MaterialCategory,
    val nameUk: String,
    val specUk: String,
    val quantityUk: String,
    val estimatedCostUah: Int
)

data class InstructionStep(
    val id: String,
    val stepNumber: Int,
    val titleUk: String,
    val durationUk: String,
    val summaryUk: String,
    val detailsUk: List<String>,
    val safetyNoteUk: String,
    val toolsUk: String,
    val hasFoundationImage: Boolean = false
)

object MaterialsAndInstructionsCatalog {

    fun buildMaterialsList(
        inputs: CalculatorInputs,
        calc: StationCalculationResult
    ): List<MaterialItem> {
        val specs = calc.blueprintSpecs
        val voltage = calc.systemVoltageV
        val genW = calc.recommendedGeneratorWatts
        val wireMm2 = calc.recommendedWireMm2.toInt()
        val cableLenM = (inputs.mastHeightM + 10.0).roundToInt()
        val pvcPipeDiam = if (inputs.rotorDiameterM >= 2.8) 200 else 160
        val pvcLenM = ceil((specs.bladeLengthMm * 3.0) / 1000.0 + 0.3).toInt()
        val diodeRating = if (calc.maxCurrentAmps > 35) "SQL100A 1000V" else "SQL50A 1000V"
        val heaterWatts = calc.activeTargetHeatWatts.coerceIn(400, 2000)

        return listOf(
            // ELECTRICAL & HEATING
            MaterialItem(
                id = "el_generator",
                category = MaterialCategory.ELECTRICAL,
                nameUk = "Низькооборотний 3-фазний генератор (PMSG)",
                specUk = "Постійні магніти, номінал $genW Вт / ${voltage}В, старт від 250 об/хв",
                quantityUk = "1 шт",
                estimatedCostUah = when {
                    genW <= 600 -> 6800
                    genW <= 1000 -> 9500
                    genW <= 1500 -> 13200
                    else -> 17500
                }
            ),
            MaterialItem(
                id = "el_controller",
                category = MaterialCategory.ELECTRICAL,
                nameUk = "MPPT вітроконтролер з виходом на баласт",
                specUk = "${voltage}В, до ${genW + 400} Вт, функція автоматичного Dump Load",
                quantityUk = "1 шт",
                estimatedCostUah = if (voltage == 24) 3400 else 4800
            ),
            MaterialItem(
                id = "el_rectifier",
                category = MaterialCategory.ELECTRICAL,
                nameUk = "3-фазний діодний міст на радіаторі",
                specUk = "$diodeRating (для прямого випрямлення або резерву)",
                quantityUk = "1 шт",
                estimatedCostUah = 450
            ),
            MaterialItem(
                id = "el_heater",
                category = MaterialCategory.ELECTRICAL,
                nameUk = "Низьковольтний ТЕН для кімнати ${inputs.roomAreaSqm.roundToInt()} м²",
                specUk = "Сухий або водяний ТЕН ${voltage}В DC, ${heaterWatts} Вт (працює також як Dump Load)",
                quantityUk = "1 шт",
                estimatedCostUah = 1450
            ),
            MaterialItem(
                id = "el_battery",
                category = MaterialCategory.ELECTRICAL,
                nameUk = "Буферний акумуляторний блок",
                specUk = "${voltage}В ${calc.recommendedBatteryAh} А·год (${String.format("%.1f", calc.batteryBankKwh)} кВт·год, AGM/LiFePO4)",
                quantityUk = if (voltage == 24) "2 × 12В" else "4 × 12В",
                estimatedCostUah = if (voltage == 24) 11500 else 21000
            ),
            MaterialItem(
                id = "el_cable",
                category = MaterialCategory.ELECTRICAL,
                nameUk = "Силовий мідний кабель (щогла -> будинок)",
                specUk = "3-жильний гнучкий ПВС/КГ 3×$wireMm2 мм² (стійкий до УФ і морозу)",
                quantityUk = "$cableLenM м",
                estimatedCostUah = cableLenM * (if (wireMm2 >= 10) 145 else 85)
            ),
            MaterialItem(
                id = "el_breakers",
                category = MaterialCategory.ELECTRICAL,
                nameUk = "3-полюсний автомат гальмування + DC запобіжники",
                specUk = "Автомат 3P 32A (КЗ обмоток для зупинки) + DC 63A",
                quantityUk = "1 компл.",
                estimatedCostUah = 920
            ),

            // ROTOR & BLADES
            MaterialItem(
                id = "rot_blades",
                category = MaterialCategory.ROTOR,
                nameUk = if (inputs.turbineType == TurbineType.SAVONIUS_VAWT) {
                    "Пластикова бочка / ПВХ циліндр 200л"
                } else {
                    "Товстостінна ПВХ труба SN8 для лопатей"
                },
                specUk = if (inputs.turbineType == TurbineType.SAVONIUS_VAWT) {
                    "ПНД бочка або оцинкований лист 1.2 мм під ротор ${inputs.rotorDiameterM} м"
                } else {
                    "Ø$pvcPipeDiam мм, стінка 4.9–5.5 мм (довжина лопаті ${specs.bladeLengthMm} мм)"
                },
                quantityUk = if (inputs.turbineType == TurbineType.SAVONIUS_VAWT) "2 шт" else "$pvcLenM м",
                estimatedCostUah = if (inputs.turbineType == TurbineType.SAVONIUS_VAWT) 1800 else pvcLenM * 520
            ),
            MaterialItem(
                id = "rot_hub",
                category = MaterialCategory.ROTOR,
                nameUk = "Ступиця ротора (сталевий фланець)",
                specUk = "Сталь 5–6 мм, Ø${specs.hubPcdMm + 60} мм, розболтовка PCD ${specs.hubPcdMm} мм",
                quantityUk = "1 шт",
                estimatedCostUah = 780
            ),
            MaterialItem(
                id = "rot_hardware",
                category = MaterialCategory.ROTOR,
                nameUk = "Кріплення лопатей (нержавіюче/високоміцне)",
                specUk = "Болти М8×35 клас 8.8, самоконтрящі гайки, широкі шайби",
                quantityUk = "24 компл.",
                estimatedCostUah = 380
            ),
            MaterialItem(
                id = "rot_tail",
                category = MaterialCategory.ROTOR,
                nameUk = "Рама поворотного вузла та хвостовий флюгер",
                specUk = "Профтруба 40×25 мм + оцинковка 1.5 мм (площа пера ${specs.tailFinAreaCm2} см²)",
                quantityUk = "1 компл.",
                estimatedCostUah = 1250
            ),

            // MAST & GUY WIRES
            MaterialItem(
                id = "mast_pipe",
                category = MaterialCategory.MAST,
                nameUk = "Сталева труба для щогли (2–3 секції)",
                specUk = "Ø${specs.mastPipeOuterMm} мм, стінка ${specs.mastWallMm} мм, висота ${inputs.mastHeightM} м",
                quantityUk = "${inputs.mastHeightM.roundToInt()} м",
                estimatedCostUah = inputs.mastHeightM.roundToInt() * (if (specs.mastPipeOuterMm >= 76) 490 else 360)
            ),
            MaterialItem(
                id = "mast_bearing",
                category = MaterialCategory.MAST,
                nameUk = "Поворотний вузол на двох підшипниках + щітковий знімач",
                specUk = "Конічні підшипники 30206 або ступиця авто + 3-кільцевий токознімач",
                quantityUk = "1 компл.",
                estimatedCostUah = 1650
            ),
            MaterialItem(
                id = "mast_wires",
                category = MaterialCategory.MAST,
                nameUk = "Сталевий оцинкований трос для розтяжок",
                specUk = "DIN 3055 Ø5 мм (радіус якорів ${(specs.guyWireRadiusMm / 1000.0)} м)",
                quantityUk = "${specs.guyWireTotalLengthM.roundToInt()} м",
                estimatedCostUah = specs.guyWireTotalLengthM.roundToInt() * 32
            ),
            MaterialItem(
                id = "mast_turnbuckles",
                category = MaterialCategory.MAST,
                nameUk = "Талрепи М10, коуші та затискачі для троса",
                specUk = "3 талрепи (крюк-кільце), 6 коушів, 18 затискачів DIN 741",
                quantityUk = "1 компл.",
                estimatedCostUah = 840
            ),

            // FOUNDATION & PROTECTION
            MaterialItem(
                id = "fnd_concrete",
                category = MaterialCategory.FOUNDATION,
                nameUk = "Суміш для фундаменту щогли (${specs.foundationSideMm}×${specs.foundationSideMm}×${specs.foundationDepthMm} мм)",
                specUk = "Цемент М500 (5 мішків) + щебінь 5–20 + пісок + арматура Ø12 мм",
                quantityUk = "1 компл. (~0.75 м³)",
                estimatedCostUah = 2900
            ),
            MaterialItem(
                id = "fnd_anchors",
                category = MaterialCategory.FOUNDATION,
                nameUk = "Анкерні закладні М16 + петлі під розтяжки",
                specUk = "Шпильки М16 (4 шт у центральний блок) + 3 грунтові якорі",
                quantityUk = "1 компл.",
                estimatedCostUah = 1100
            ),
            MaterialItem(
                id = "fnd_grounding",
                category = MaterialCategory.FOUNDATION,
                nameUk = "Контур заземлення та грозозахист",
                specUk = "3 оцинковані штирі по 1.5 м + смуга 25×4 мм + розрядник DC",
                quantityUk = "1 компл.",
                estimatedCostUah = 1550
            )
        )
    }

    fun buildInstructions(
        inputs: CalculatorInputs,
        calc: StationCalculationResult
    ): List<InstructionStep> {
        val specs = calc.blueprintSpecs
        val roomArea = inputs.roomAreaSqm.roundToInt()
        return listOf(
            InstructionStep(
                id = "step_1",
                stepNumber = 1,
                titleUk = "Вибір майданчика та підготовка фундаменту",
                durationUk = "1–2 дні (+ 7 днів витримки бетону)",
                summaryUk = "Встановіть щоглу на відстані не менше 10 м від високих перешкод так, щоб вісь ротора (${inputs.mastHeightM} м) була мінімум на 2.5 м вище коника даху.",
                detailsUk = listOf(
                    "Викопайте центральний котлован ${specs.foundationSideMm}×${specs.foundationSideMm} мм глибиною ${specs.foundationDepthMm} мм нижче глибини промерзання ґрунту.",
                    "Зваріть армокаркас із прутка Ø12 мм та зафіксуйте 4 закладні шпильки М16 під шарнірну п'яту щогли (зручно піднімати лебідкою).",
                    "Розмітьте 3 якорі для розтяжок під кутом 120° на радіусі ${String.format("%.1f", specs.guyWireRadiusMm / 1000.0)} м від центру щогли.",
                    "Залийте бетон марки М250–М300 і залиште набирати міцність мінімум на 5–7 діб перед підйомом щогли."
                ),
                safetyNoteUk = "Обов'язково закладіть сталеву смугу контуру заземлення поруч із фундаментом до початку бетонування.",
                toolsUk = "Лопата, рівень, зварювальний апарат, бетономішалка",
                hasFoundationImage = true
            ),
            InstructionStep(
                id = "step_2",
                stepNumber = 2,
                titleUk = "Виготовлення або підготовка лопатей ротора",
                durationUk = "4–6 годин",
                summaryUk = "Ротор діаметром ${inputs.rotorDiameterM} м (${inputs.turbineType.shortTitleUk}) формує потужність ${calc.expectedPowerAtHubWindW} Вт при вітрі ${String.format("%.1f", calc.effectiveWindAtHubMs)} м/с.",
                detailsUk = listOf(
                    "Розмітьте ПВХ трубу Ø${if (inputs.rotorDiameterM >= 2.8) 200 else 160} мм (товщина стінки ≥ 5 мм) по шаблону: довжина лопаті ${specs.bladeLengthMm} мм, корінь ${specs.rootChordMm} мм, кінчик ${specs.tipChordMm} мм.",
                    "Виріжте 3 однакові лопаті електролобзиком або УШМ з кругом по пластику; зніміть фаски на передній та задній кромках (профіль крила).",
                    "Вирівняйте вагу кожної лопаті на електронних вагах з точністю до ±3 грамів (дошліфуйте або додайте епоксидну балансувальну шайбу).",
                    "Покрийте ПВХ автоґрунтом та двокомпонентною УФ-стійкою фарбою (білий/світло-сірий колір), щоб пластик не деградував на сонці."
                ),
                safetyNoteUk = "Не використовуйте тонку сіру каналізаційну ПВХ трубу — на морозі вона крихка. Беріть лише руду трубу зовнішньої каналізації SN8.",
                toolsUk = "Електролобзик, шліфмашина, електронні ваги (точність 1 г), штангенциркуль"
            ),
            InstructionStep(
                id = "step_3",
                stepNumber = 3,
                titleUk = "Збірка ступиці, генератора та хвоста-флюгера",
                durationUk = "3–4 години",
                summaryUk = "З'єднайте 3-фазний генератор (${calc.recommendedGeneratorWatts} Вт, ${calc.systemVoltageV}В) зі ступицею та системою штормового складання хвоста.",
                detailsUk = listOf(
                    "Закріпіть лопаті до сталевої ступиці (болти М8×35 класу міцності 8.8, отвори по діаметру PCD ${specs.hubPcdMm} мм) та перевірте відсутність биття площини обертання.",
                    "Зберіть гондолу з профільної труби 40×25 мм та приєднайте поворотний вузол на двох конічних підшипниках із щітковим токознімачем (або вільним витком кабелю всередині щогли).",
                    "Змонтуйте хвіст довжиною ${specs.tailVaneLengthMm} мм на шарнірі під кутом 15–20° (фурлінг): при вітрі понад 12 м/с вітрове навантаження відвертає ротор боком до потоку."
                ),
                safetyNoteUk = "Перед встановленням ротора на вал генератора замкніть усі 3 фазні виводи генератора між собою перемичкою — це загальмує вал під час монтажу.",
                toolsUk = "Набір ключів, динамометричний ключ, зварювання, дриль"
            ),
            InstructionStep(
                id = "step_4",
                stepNumber = 4,
                titleUk = "Протягування кабелю та підйом щогли",
                durationUk = "3–4 години (потрібно 2–3 помічники)",
                summaryUk = "Прокладіть кабель 3×${calc.recommendedWireMm2.toInt()} мм² всередині щогли висотою ${inputs.mastHeightM} м, встановіть гондолу та натягніть розтяжки.",
                detailsUk = listOf(
                    "Протягніть силовий кабель через трубу щогли, зафіксувавши його хомутом зверху, щоб вага кабелю не обірвала клеми токознімача.",
                    "Прикріпіть хомут під 3 розтяжки на висоті 75–80% щогли (${String.format("%.1f", inputs.mastHeightM * 0.78)} м) та зберіть троси з коушами й 3 затискачами на кожну петлю.",
                    "Підніміть щоглу на шарнірній п'яті за допомогою падаючої стріли та лебідки або двох помічників на бічних страхувальних розтяжках.",
                    "Виставте вертикаль будівельним рівнем у двох площинах та підтягніть талрепи без надмірного перетягування."
                ),
                safetyNoteUk = "Категорично заборонено проводити підйом щогли при поривах вітру понад 5 м/с та поблизу повітряних ліній електропередач.",
                toolsUk = "Лебідка 1 т, рівень, ключі для талрепів, страхувальні пояси"
            ),
            InstructionStep(
                id = "step_5",
                stepNumber = 5,
                titleUk = "Електромонтаж,Dump Load та обігрів кімнати $roomArea м²",
                durationUk = "3–5 годин",
                summaryUk = "Підключіть схему: Генератор -> 3P автомат гальмування -> Контролер -> АКБ ${calc.systemVoltageV}В -> DC ТЕН у кімнаті $roomArea м².",
                detailsUk = listOf(
                    "У сухому технічному приміщенні встановіть 3-полюсний автомат аварійного гальмування: у положенні «ВИМК» або через перемикач він закорочує фази через баластний резистор.",
                    "Підключіть 3 фази до MPPT-контролера (або через діодний міст ${if (calc.maxCurrentAmps > 35) "SQL100A" else "SQL50A"} у спрощеній схемі прямого нагріву).",
                    "Підключіть буферний АКБ (${calc.systemVoltageV}В ${calc.recommendedBatteryAh} А·год) через запобіжник DC 63A (підключати АКБ ДО під'єднання вітряка!).",
                    "Змонтуйте ТЕН на ${calc.activeTargetHeatWatts} Вт (${calc.systemVoltageV}В) у радіаторі або масляному конвекторі кімнати $roomArea м² як основне/баластне навантаження (Dump Load) — надлишок вітру одразу гріє кімнату."
                ),
                safetyNoteUk = "Ніколи не залишайте вітряк на холостому ходу без АКБ чи баластного ТЕНа — при пориві вітру напруга перевищить 100В і проб'є контролер.",
                toolsUk = "Мультиметр, прес-кліщі для гільз, викрутки, ізолента, термоусадка"
            ),
            InstructionStep(
                id = "step_6",
                stepNumber = 6,
                titleUk = "Пусконалагодження та контроль нагріву",
                durationUk = "1–2 години",
                summaryUk = "Перевірте баланс ротора, фазні напруги, роботу термостата кімнати $roomArea м² та аварійну зупинку.",
                detailsUk = listOf(
                    "Зніміть гальмівну перемичку при помірному вітрі (3–5 м/с) і прослухайте роботу підшипників — не повинно бути вібрації щогли чи свисту лопатей.",
                    "Заміряйте струм заряду та нагрів ТЕНа кліщами постійного струму: при вітрі ${String.format("%.1f", calc.effectiveWindAtHubMs)} м/с очікувана потужність ~${calc.expectedPowerAtHubWindW} Вт.",
                    "Перевірте спрацювання термостата в кімнаті та перемикання контролера на резервний баласт при повному заряді АКБ."
                ),
                safetyNoteUk = "Якщо на високих обертах виникає вібрація щогли — негайно загальмуйте ротор 3P автоматом і перевірте кріплення та баланс лопатей.",
                toolsUk = "Струмовимірювальні кліщі DC, пірометр/термометр"
            ),
            InstructionStep(
                id = "step_7",
                stepNumber = 7,
                titleUk = "Регламентне обслуговування (раз на півроку)",
                durationUk = "1 година восени та навесні",
                summaryUk = "Перед зимовим опалювальним сезоном перевірте натяг тросів, затяжку болтів і стан контактів.",
                detailsUk = listOf(
                    "Огляньте талрепи та затискачі розтяжок щогли, за потреби підтягніть провислі троси.",
                    "Перевірте опір заземлення та відсутність окислення на клемах АКБ і ТЕНа.",
                    "Огляньте передню кромку лопатей на наявність мікротріщин після сильних морозів або граду."
                ),
                safetyNoteUk = "Перед будь-яким оглядом гондоли обов'язково переведіть рубильник гальмування у положення «СТОП».",
                toolsUk = "Бінокль, гайкові ключі, мультиметр, мастило для підшипників"
            )
        )
    }
}
