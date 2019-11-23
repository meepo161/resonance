package ru.avem.resonance.controllers

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import ru.avem.resonance.Constants
import ru.avem.resonance.Constants.Ends.*
import ru.avem.resonance.Constants.Vfd.VFD_FORWARD
import ru.avem.resonance.Constants.Vfd.VFD_REVERSE
import ru.avem.resonance.Main
import ru.avem.resonance.communication.CommunicationModel
import ru.avem.resonance.communication.devices.DeviceController.*
import ru.avem.resonance.communication.devices.avem_voltmeter.AvemVoltmeterModel
import ru.avem.resonance.communication.devices.deltaC2000.DeltaCP2000Model
import ru.avem.resonance.communication.devices.latr.LatrModel
import ru.avem.resonance.communication.devices.pm130.PM130Model
import ru.avem.resonance.communication.devices.pr200.OwenPRModel
import ru.avem.resonance.communication.modbus.utils.Utils
import ru.avem.resonance.db.TestItemRepository
import ru.avem.resonance.db.model.TestItem
import ru.avem.resonance.model.Experiment2Model
import ru.avem.resonance.model.MainModel
import ru.avem.resonance.model.Point
import ru.avem.resonance.utils.Toast
import ru.avem.resonance.utils.Utils.sleep
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class Experiment2Controller : DeviceState(), ExperimentController {

    @FXML
    lateinit var tableViewExperiment2: TableView<Experiment2Model>
    @FXML
    lateinit var tableColumnU: TableColumn<Experiment2Model, String>
    @FXML
    lateinit var tableColumnIB: TableColumn<Experiment2Model, String>
    @FXML
    lateinit var tableColumnUOI: TableColumn<Experiment2Model, String>
    @FXML
    lateinit var tableColumnIOI: TableColumn<Experiment2Model, String>
    @FXML
    lateinit var tableColumnResultExperiment2: TableColumn<Experiment2Model, String>
    @FXML
    lateinit var textAreaExperiment2Log: TextArea
    @FXML
    lateinit var lineChartExperiment2: LineChart<Number, Number>
    @FXML
    lateinit var xAxis: NumberAxis
    @FXML
    lateinit var yAxis: NumberAxis
    @FXML
    lateinit var buttonStartStop: Button
    @FXML
    lateinit var buttonNext: Button
    @FXML
    lateinit var root: AnchorPane
    @FXML
    lateinit var gridPaneTimeTorque: GridPane
    @FXML
    lateinit var vBoxTime: VBox
    @FXML
    lateinit var vBoxVoltage: VBox
    @FXML
    lateinit var vBoxSpeed: VBox
    @FXML
    lateinit var anchorPaneTimeTorque: AnchorPane
    @FXML
    lateinit var scrollPaneTimeTorque: ScrollPane

    private lateinit var lastTriple: Triple<TextField, TextField, TextField>
    private val stackTriples: Stack<Triple<TextField, TextField, TextField>> = Stack()

    private val mainModel = MainModel.instance
    private val currentProtocol = mainModel.currentProtocol
    private val communicationModel = CommunicationModel.getInstance()
    private var experiment2Model: Experiment2Model? = null
    private val experiment2Data = FXCollections.observableArrayList<Experiment2Model>()

    private var dialogStage: Stage? = null
    private var isCanceled: Boolean = false

    @Volatile
    private var isNeedToRefresh: Boolean = false
    @Volatile
    private var isExperimentRunning: Boolean = false
    @Volatile
    private var isExperimentEnded = true
    @Volatile
    private var isNeedCheckLatrStatus: Boolean = false

    @Volatile
    private var isOwenPRResponding: Boolean = false
    @Volatile
    private var isDeltaResponding: Boolean = false
    @Volatile
    private var isParmaResponding: Boolean = false
    @Volatile
    private var isLatrResponding: Boolean = false
    @Volatile
    private var isAvemResponding: Boolean = false
    @Volatile
    private var isKiloAvemResponding: Boolean = false
    @Volatile
    private var latrStatus: String = ""

    private val sdf = SimpleDateFormat("HH:mm:ss-SSS")
    private var logBuffer: String? = null
    @Volatile
    private var cause: String? = null
    @Volatile
    private var measuringU: Float = 0.0f
    @Volatile
    private var measuringULatr: Float = 0.0f
    @Volatile
    private var measuringIB: Float = 0.0f
    @Volatile
    private var measuringIC: Float = 0.0f
    @Volatile
    private var isSchemeReady: Boolean = false
    @Volatile
    private var isControlRubilNeed: Boolean = false

    @Volatile
    private var ткзДоТрансформатора: Boolean = false
    @Volatile
    private var ткзОИ: Boolean = false
    @Volatile
    private var ткзПослеТрансформатора: Boolean = false
    @Volatile
    private var контрольДверей: Boolean = false
    @Volatile
    private var контрольПуска: Boolean = false
    @Volatile
    private var контрольРубильника: Boolean = false
    @Volatile
    private var ручнойРежимСПК: Boolean = false
    @Volatile
    private var переменное: Boolean = false
    @Volatile
    private var постоянное: Boolean = false
    @Volatile
    private var резонанс: Boolean = false
    @Volatile
    private var старт: Boolean = false
    @Volatile
    private var стоп: Boolean = false
    @Volatile
    private var стопИспытания: Boolean = false
    @Volatile
    private var подъемНапряжения: Boolean = false
    @Volatile
    private var уменьшениеНапряжения: Boolean = false
    @Volatile
    private var statusEndsVFD: Short = 0
    @Volatile
    private var statusVFD: Short = 0

    private var coef: Double = 0.0

    private var voltageList: ArrayList<Double> = ArrayList()
    private var timeList: ArrayList<Double> = ArrayList()
    private var speedList: ArrayList<Double> = ArrayList()

    private var currentTestItem: TestItem = mainModel.currentTestItem
    private var timePassed = 0.0
    private var time = 0.0
    private var timeSum = 0.0
    private var seriesTimesAndVoltage = XYChart.Series<Number, Number>()
    private var realTime = 0.0

    var size = currentTestItem.voltageViu.size

    private val isDevicesResponding: Boolean
        get() = isOwenPRResponding && isAvemResponding && isDeltaResponding && isLatrResponding
                && isParmaResponding && isKiloAvemResponding

    private val points = ArrayList<Point>()

    @FXML
    private fun initialize() {
        if (Main.css == "white") {
            root.stylesheets[0] = Main::class.java.getResource("styles/main_css.css").toURI().toString()
        } else {
            root.stylesheets[0] = Main::class.java.getResource("styles/main_css_black.css").toURI().toString()
        }
        cause = ""
        experiment2Model = mainModel.experiment2Model
        experiment2Data.add(experiment2Model)
        tableViewExperiment2.items = experiment2Data
        tableViewExperiment2.selectionModel = null
        communicationModel.addObserver(this)
        voltageList = currentProtocol.voltageViu
        timeList = currentProtocol.timesViu
        speedList = currentProtocol.speedViu
        tableColumnU.setCellValueFactory { cellData -> cellData.value.voltageARNProperty() }
        tableColumnIB.setCellValueFactory { cellData -> cellData.value.currentBProperty() }
        tableColumnUOI.setCellValueFactory { cellData -> cellData.value.voltageProperty() }
        tableColumnIOI.setCellValueFactory { cellData -> cellData.value.currentOIProperty() }
        tableColumnResultExperiment2.setCellValueFactory { cellData -> cellData.value.resultProperty() }
        fillStackPairs()
        lineChartExperiment2.data.add(seriesTimesAndVoltage)
        Platform.runLater {
            buttonStartStop.style = "-fx-background-color: linear-gradient(#55d43d, #8ce17d);"
        }
    }

    private fun fillStackPairs() {
        for (i in 0 until currentTestItem.timesViu.size) {
            addPair()
            lastTriple.first.text = currentTestItem.voltageViu[i].toString()
            lastTriple.second.text = currentTestItem.timesViu[i].toString()
            lastTriple.third.text = currentTestItem.speedViu[i].toString()
        }
    }

    @FXML
    fun handleAddPair() {
        addPair()
        size++
    }

    private fun addPair() {
        lastTriple = newTextFieldsForChart()
        stackTriples.push(lastTriple)
        vBoxVoltage.children.add(lastTriple.first)
        vBoxTime.children.add(lastTriple.second)
        vBoxSpeed.children.add(lastTriple.third)
        anchorPaneTimeTorque.prefHeight += MainViewController.HEIGHT_VBOX
    }

    @FXML
    fun handleRemovePair() {
        if (stackTriples.isNotEmpty()) {
            removePair()
            saveTestItemPoints()
            size--
        } else {
            Toast.makeText("Нет полей для удаления").show(Toast.ToastType.ERROR)
        }
    }

    private fun removePair() {
        lastTriple = stackTriples.pop()
        Platform.runLater {
            vBoxVoltage.children.remove(lastTriple.first)
            vBoxTime.children.remove(lastTriple.second)
            vBoxSpeed.children.remove(lastTriple.third)
            anchorPaneTimeTorque.prefHeight -= MainViewController.HEIGHT_VBOX
        }
    }

    private fun newTextFieldsForChart(): Triple<TextField, TextField, TextField> {
        val time = TextField()
        time.isEditable = true
        time.prefWidth = 72.0
        time.maxWidth = 72.0
        time.setOnAction {
            if (time.text.toDouble() * 1000 > timePassed) {
                saveTestItemPoints()
            } else {
                Toast.makeText("Введенное значение меньше пройденного значения времени").show(Toast.ToastType.ERROR)
            }
        }

        val voltage = TextField()
        voltage.isEditable = true
        voltage.prefWidth = 72.0
        voltage.maxWidth = 72.0
        voltage.setOnAction {
            saveTestItemPoints()
        }

        val speed = TextField()
        speed.isEditable = true
        speed.prefWidth = 72.0
        speed.maxWidth = 72.0
        speed.setOnAction {
            saveTestItemPoints()
        }
        return Triple(time, voltage, speed)
    }

    private fun saveTestItemPoints() {
        val times: ArrayList<Double> = ArrayList()
        val voltages: ArrayList<Double> = ArrayList()
        val speeds: ArrayList<Double> = ArrayList()

        stackTriples.forEach {
            if (it.first.text.isNullOrEmpty() && it.second.text.isNullOrEmpty() && it.third.text.isNullOrEmpty() &&
                    it.first.text.toDoubleOrNull() == null && it.second.text.toDoubleOrNull() == null && it.third.text.toDoubleOrNull() == null) {
                handleRemovePair()
            } else if (it.first.text.toDoubleOrNull()!! > 60.0) {
                Toast.makeText("Сохранить не удалось. Напряжение в этом опыте не может быть больше 60кВ.").show(Toast.ToastType.ERROR)
            } else if (it.third.text.toDoubleOrNull()!! > 2.0) {
                Toast.makeText("Сохранить не удалось. Скорость не должна быть больше 2кВ/с").show(Toast.ToastType.ERROR)
            } else {
                voltages.add(it.first.text.toDouble())
                times.add(it.second.text.toDouble())
                speeds.add(it.third.text.toDouble())
            }
        }
        currentTestItem.voltageViu = voltages
        currentTestItem.timesViu = times
        currentTestItem.speedViu = speeds

        voltageList = currentProtocol.voltageViu
        timeList = currentProtocol.timesViu
        speedList = currentProtocol.speedViu

        TestItemRepository.updateTestItem(currentTestItem)
        Toast.makeText("Сделаны изменения").show(Toast.ToastType.CONFIRM)
    }

    private fun createLoadDiagram() {
        Thread {
            while (isExperimentRunning) {
                if (realTime < 400) {
                    Platform.runLater {
                        seriesTimesAndVoltage.data.add(XYChart.Data<Number, Number>(realTime, measuringU))
                    }
                } else {
                    Platform.runLater {
                        seriesTimesAndVoltage.data.clear()
                    }
                    realTime = 0.0
                }
                sleep(1000)
                realTime += 1
            }
        }.start()
    }

    private fun fillProtocolExperimentFields() {
        val currentProtocol = mainModel.currentProtocol
        currentProtocol.typeExperiment = "ВИУ переменным напряжением"
    }

    @FXML
    private fun handleNextExperiment() {
        fillProtocolExperimentFields()
        dialogStage!!.close()
    }

    @FXML
    private fun handleRunStopExperiment() {
        if (isExperimentEnded) {
            startExperiment()
        } else {
            stopExperiment()
        }
    }

    private fun stopExperiment() {
        isNeedToRefresh = false
        setCause("Отменено оператором")
        isExperimentRunning = false
    }


    private fun startExperiment() {
        setCause("")
        points.clear()
        isNeedToRefresh = true
        isNeedCheckLatrStatus = false
        isExperimentRunning = true
        isExperimentEnded = false
        Platform.runLater {
            buttonStartStop.text = "Остановить"
            buttonStartStop.style = "-fx-background-color: linear-gradient(#ff6f8c, #FF6F61);"
        }
        buttonNext.isDisable = true
        experiment2Model!!.clearProperties()
        isSchemeReady = false
        cause = ""
        isControlRubilNeed = false

        Thread {

            if (isExperimentRunning) {
                appendOneMessageToLog("Инициализация системы")
                communicationModel.initOwenPrController()
                communicationModel.initExperimentDevices()
            }

            while (!isDevicesResponding) {
                sleep(10)
            }

            if (isExperimentRunning) {
                communicationModel.setKiloAvemShowValue(Constants.Avem.VOLTAGE_RMS.ordinal)
                communicationModel.resetLATR()
                communicationModel.таймер_On()
                communicationModel.таймер_Off()
                communicationModel.таймер_On()
                communicationModel.таймер_Off()
            }

            if (!контрольРубильника && isExperimentRunning && isDevicesResponding) {
                appendOneMessageToLog("Поднимите рубильник силового питания")
            }

            while (!контрольРубильника && isExperimentRunning && isDevicesResponding) {
                sleep(10)
            }

            if (isExperimentRunning && isDevicesResponding) {
                appendOneMessageToLog("Нажмите кнопку ПУСК")
            }

            while (!контрольПуска && isExperimentRunning && isDevicesResponding) {
                communicationModel.разрешениеНаЗапуск_On()
                sleep(10)
            }

            if (isExperimentRunning) {
                appendOneMessageToLog("Начало испытания")
                isControlRubilNeed = true
            }

            if (isExperimentRunning) {
                appendOneMessageToLog("Устанавливаем начальные точки для ЧП")
                communicationModel.setObjectParams(50 * 100, 380 * 10, 50 * 100)
                appendOneMessageToLog("Запускаем ЧП")
                resetOmik()
                communicationModel.параллельнаяСхема_On()
                communicationModel.короткозамыкатель_On()
            }


            if (isExperimentRunning && isDevicesResponding) {
                communicationModel.приемКоманды_On()
                appendOneMessageToLog("Поднимаем напряжение на объекте испытания для поиска резонанса")
                communicationModel.resetLATR()
                putUpLatr(1100f)
                findResonance()
            }

            timeSum = 0.0

            if (isExperimentRunning && isDevicesResponding) {
                createLoadDiagram()
                voltageList = currentTestItem.voltageViu
                timeList = currentProtocol.timesViu
                speedList = currentProtocol.speedViu
                var i = 0
                while (size-- > 0) {
                    if (currentTestItem.voltageViu.size == i && (stackTriples[i].first.text.isNullOrEmpty()
                                    || stackTriples[i].second.text.isNullOrEmpty() || stackTriples[i].third.text.isNullOrEmpty())) {
                        handleRemovePair()
                        break
                    }
                    stackTriples[i].first.isDisable = true
                    stackTriples[i].third.isDisable = true
                    timePassed = 0.0
                    if (isExperimentRunning && isDevicesResponding) {
                        appendOneMessageToLog("Началась регулировка")
                        putUpLatr(voltageList[i].toFloat() * 1000)
                        if (measuringULatr < measuringU * 0.5 && measuringULatr * 0.5 > measuringU) {
                            setCause("Коэфицент трансформации сильно отличается")
                        }
                        appendOneMessageToLog("Регулировка окончена")
                    }
                    communicationModel.таймер_On()

                    appendOneMessageToLog("Начался отсчет времени")
                    time = currentTestItem . timesViu [i]
                    while (isExperimentRunning && timePassed < time) {
                        time = currentTestItem.timesViu[i]
                        sleep(1000)
                        timePassed += 1
                        if (time != stackTriples[i].second.text.toDouble()) {
                            time = currentTestItem.timesViu[i]
                        }
                    }
                    fillPointData()

                    voltageList = currentTestItem.voltageViu
                    timeSum += currentTestItem.timesViu[i]
                    stackTriples[i].second.isDisable = true
                    i++
                    communicationModel.таймер_Off()
                }
            }

            isNeedToRefresh = false
            communicationModel.startUpLATRUp(0f, true)

            while (measuringU > 1300) {
                sleep(10)
            }
            communicationModel.stopLATR()
            resetOmik()
            var timeToSleep = 300
            while (isExperimentRunning && (timeToSleep-- > 0)) {
                sleep(10)
            }
            isControlRubilNeed = false

            communicationModel.offAllKms()

            if (контрольРубильника && isDevicesResponding) {
                appendOneMessageToLog("Отключите рубильник")
                communicationModel.внимание_On()
            }

            while (контрольРубильника && isDevicesResponding) {
                sleep(10)
            }

            if (isExperimentRunning && isDevicesResponding) {
                communicationModel.внимание_Off()
            }


            communicationModel.finalizeAllDevices()

            if (cause != "") {
                appendMessageToLog(String.format("Испытание прервано по причине: %s", cause))
                experiment2Model!!.result = "Завершено"
            } else if (!isDevicesResponding) {
                appendMessageToLog(getNotRespondingDevicesString())
                experiment2Model!!.result = "Завершено"
            } else {
                experiment2Model!!.result = "Завершено"
                appendMessageToLog("Испытание завершено успешно")
            }
            appendMessageToLog("\n------------------------------------------------\n")

            Platform.runLater()
            {
                buttonStartStop.text = "Запустить"
                buttonStartStop.style = "-fx-background-color: linear-gradient(#55d43d, #8ce17d);"
                isExperimentEnded = true
                isExperimentRunning = false
                buttonNext.isDisable = false
            }
        }.start()
    }

    private fun findResonance() {
        appendOneMessageToLog("Идет поиск резонанса")
        if (statusVFD == VFD_REVERSE && isExperimentRunning && isDevicesResponding) {
            communicationModel.changeRotation()
            sleep(2000)
        }
        communicationModel.startObject()
        var highestU = measuringU
        var lowestI = measuringIB
        var step = 5
        while ((step-- > 0) && isExperimentRunning && isDevicesResponding) {
            if (measuringU > highestU) {
                highestU = measuringU
                step = 5
            }
            if (measuringIB < lowestI) {
                lowestI = measuringIB
                step = 5
            }
            sleep(500)
        }
        communicationModel.stopObject()
        sleep(3000)
        communicationModel.changeRotation()
        communicationModel.setObjectParams(25 * 100, 380 * 10, 25 * 100)
        communicationModel.startObject()
        while (measuringU < highestU && measuringIB > lowestI && isExperimentRunning && isDevicesResponding) { //Из-за инерции
            if (statusEndsVFD == OMIK_DOWN_END) {
                setCause("Не удалось подобрать резонанс")
            }
            sleep(10)
        }
        communicationModel.stopObject()
        appendOneMessageToLog("Поиск завершен")
        sleep(1000)
    }

    private fun fillPointData() {
        points.add(Point(measuringU.toDouble(), measuringIC.toDouble(), String.format("%s", sdf.format(System.currentTimeMillis()))))
        currentProtocol.points = points
    }

    private fun putUpLatr(voltage: Float) {
        communicationModel.startUpLATRUp((voltage / coef).toFloat(), false)
        waitingLatrCoarse(voltage)
        fineLatr(voltage)
    }

    private fun waitingLatrCoarse(voltage: Float) {
        appendOneMessageToLog("Грубая регулировка")
        while (isExperimentRunning && isDevicesResponding && (measuringU <= voltage * 0.8 || measuringU > voltage * 1.2)) {
            if (measuringU <= voltage * 0.8) {
                communicationModel.startUpLATRWithRegulationSpeed(440f, false, 50f, 80f)
            } else if (measuringU > voltage * 1.2) {
                communicationModel.startUpLATRWithRegulationSpeed(1f, false, 50f, 80f)
            } else {
                break
            }
        }
        communicationModel.stopLATR()
        appendOneMessageToLog("Грубая регулировка окончена")
    }

    private fun fineLatr(voltage: Float) {
        appendOneMessageToLog("Точная регулировка")
        communicationModel.stopLATR()
        while ((measuringU <= voltage * 0.95 || measuringU > voltage * 1.05) && isExperimentRunning) {
            if (measuringU * 1.05 > voltage && measuringU * 0.95 < voltage) {
                communicationModel.stopLATR()
                break
            }
            if (measuringU <= voltage * 0.95) {
                communicationModel.startUpLATRCharge(440f, false)
                if (measuringU + 1000 < voltage) {
                    sleep(2200)
                } else {
                    sleep(1600)
                }
                communicationModel.stopLATR()
            } else if (measuringU >= voltage * 1.05) {
                communicationModel.startUpLATRCharge(1f, false)
                if (measuringU - 1000 > voltage) {
                    sleep(2200)
                } else {
                    sleep(1600)
                }
                communicationModel.stopLATR()
            }
        }
        appendOneMessageToLog("Точная регулировка закончена")

        communicationModel.stopLATR()
    }

    private fun resetOmik() {
        communicationModel.setObjectParams(50 * 100, 380 * 10, 50 * 100)
        if (statusEndsVFD != OMIK_DOWN_END && isDevicesResponding) {
            appendOneMessageToLog("Возвращаем магнитопровод в исходное состояние")
            if (statusVFD != VFD_REVERSE && isDevicesResponding) {
                communicationModel.changeRotation()
            }
            communicationModel.startObject()
            var waitingTime = 30
            while (isDevicesResponding && (waitingTime-- > 0)) {
                sleep(100)
            }
            while (statusEndsVFD != OMIK_DOWN_END && isDevicesResponding) {
                sleep(10)
                if (statusEndsVFD == OMIK_UP_END && isDevicesResponding) {
                    setCause("Омик в верхнем положенении, двигаясь вниз")
                    break
                }
            }
            communicationModel.stopObject()
        }
        if (statusEndsVFD == OMIK_DOWN_END) {
            appendOneMessageToLog("ОМИК в нижнем положении")
        }
        communicationModel.stopObject()
    }

    private fun appendMessageToLog(message: String) {
        Platform.runLater {
            textAreaExperiment2Log.appendText(String.format("%s \t| %s\n", sdf.format(System.currentTimeMillis()), message))
        }
    }

    private fun appendOneMessageToLog(message: String) {
        if (logBuffer == null || logBuffer != message) {
            logBuffer = message
            appendMessageToLog(message)
        }
    }

    private fun getNotRespondingDevicesString(): String {
        return String.format("%s %s%s%s%s%s%s",
                "Испытание прервано по причине: потеряна связь с устройствами",
                if (isOwenPRResponding) "" else "Овен ПР ",
                if (isParmaResponding) "" else "Парма ",
                if (isDeltaResponding) "" else "Дельта ",
                if (isLatrResponding) "" else "Латр ",
                if (isAvemResponding) "" else "АВЭМ ",
                if (isKiloAvemResponding) "" else "КилоАВЭМ ")
    }

    private fun setCause(cause: String) {
        this.cause = cause
        if (cause.isNotEmpty()) {
            isExperimentRunning = false
        }
    }

    override fun update(o: Observable, values: Any) {
        val modelId = (values as Array<*>)[0] as Int
        val param = values[1] as Int
        val value = values[2]

        when (modelId) {
            PR200_ID -> when (param) {
                OwenPRModel.RESPONDING_PARAM -> {
                    isOwenPRResponding = value as Boolean
                    Platform.runLater { deviceStateCirclePR200.fill = if (value) Color.LIME else Color.RED }
                }
                OwenPRModel.ТКЗ_ДО_ТРАНСФОРМАТОРА -> {
                    ткзДоТрансформатора = value as Boolean
                    if (ткзДоТрансформатора) {
                        communicationModel.offAllKms()
                        setCause("ткзДоТрансформатора")
                    }
                }
                OwenPRModel.ТКЗ_ОИ -> {
                    ткзОИ = value as Boolean
                    if (ткзОИ) {
                        communicationModel.offAllKms()
                        setCause("ткзОИ")
                    }
                }
                OwenPRModel.ТКЗ_ПОСЛЕ_ТРАНСФОРМАТОРА -> {
                    ткзПослеТрансформатора = value as Boolean
                    if (ткзПослеТрансформатора) {
                        communicationModel.offAllKms()
                        setCause("ткзПослеТрансформатора")
                    }
                }
                OwenPRModel.КОНТРОЛЬ_ДВЕРЕЙ_ШСО -> {
                    контрольДверей = value as Boolean
                    if (контрольДверей) {
                        communicationModel.offAllKms()
                        setCause("контрольДверей")
                    }
                }
                OwenPRModel.КОНТРОЛЬ_ПУСКА -> {
                    контрольПуска = value as Boolean
                    if (!контрольПуска && isControlRubilNeed) {
                        communicationModel.offAllKms()
                        setCause("Сработала защита")
                    }
                }
                OwenPRModel.КОНТРОЛЬ_РУБИЛЬНИКА -> {
                    контрольРубильника = value as Boolean
                    if (!контрольРубильника && isControlRubilNeed) {
                        setCause("Во время испытания отключен рубильник силового питания")
                    }
                }
                OwenPRModel.РУЧНОЙ_РЕЖИМ_С_ПК -> {
                    ручнойРежимСПК = value as Boolean
                }
                OwenPRModel.ПЕРЕМЕННОЕ -> {
                    переменное = value as Boolean
                }
                OwenPRModel.ПЕРЕМЕННОЕ_С_РЕЗОНАНСОМ -> {
                    резонанс = value as Boolean
                }
                OwenPRModel.ПОСТОЯННОЕ -> {
                    постоянное = value as Boolean
                }
                OwenPRModel.СТАРТ_ТАЙМЕР -> {
                    старт = value as Boolean
                }
                OwenPRModel.СТОП_ТАЙМЕР -> {
                    стоп = value as Boolean
                }
                OwenPRModel.СТОП_ИСПЫТАНИЯ -> {
                    стопИспытания = value as Boolean
                    if (стопИспытания) {
                        setCause("Во время испытания была нажата кнопка СТОП")
                    }
                }
                OwenPRModel.ПОДЪЕМ_НАПРЯЖЕНИЯ -> {
                    подъемНапряжения = value as Boolean
                }
                OwenPRModel.УМЕНЬШЕНИЕ_НАПРЯЖЕНИЯ -> {
                    уменьшениеНапряжения = value as Boolean
                }
            }

            PM130_ID -> when (param) {
                PM130Model.RESPONDING_PARAM -> {
                    isParmaResponding = value as Boolean
                    Platform.runLater { deviceStateCirclePM130.fill = if (value) Color.LIME else Color.RED }
                }
                PM130Model.I2_PARAM -> {
                    measuringIB = value as Float * 20
                    val iB = String.format("%.2f", measuringIB)
                    experiment2Model!!.currentB = iB
                    if (measuringIB > 45) {
                        appendMessageToLog("Ток B превышает 45А")
                    }
                }
                PM130Model.I3_PARAM -> {
                    measuringIC = value as Float
                    val iC = String.format("%.2f", measuringIC)
                    experiment2Model!!.currentOI = iC
                    if (measuringIC > 45) {
                        appendMessageToLog("Ток C превышает 45А")
                    }
                }
            }

            DELTACP2000_ID -> when (param) {
                DeltaCP2000Model.RESPONDING_PARAM -> {
                    isDeltaResponding = value as Boolean
                    Platform.runLater { deviceStateCircleDelta.fill = if (value) Color.LIME else Color.RED }
                }
                DeltaCP2000Model.ENDS_STATUS_PARAM -> {
                    statusEndsVFD = value as Short
                    checkEndsVFDStatus()
                }
                DeltaCP2000Model.STATUS_VFD -> {
                    statusVFD = value as Short
                    checkVFDStatus()
//                    when {
//                        statusEndsVFD == OMIK_DOWN_END -> communicationModel.stopObject()
//                        statusVFD == OMIK_UP_END -> communicationModel.stopObject()
//                        statusVFD == OMIK_BOTH_END -> communicationModel.stopObject()
//                    }
                }
            }

            AVEM_ID -> when (param) {
                AvemVoltmeterModel.RESPONDING_PARAM -> {
                    isAvemResponding = value as Boolean
                    Platform.runLater { deviceStateCircleAvem.fill = if (value) Color.LIME else Color.RED }
                }
            }

            KILOAVEM_ID -> when (param) {
                AvemVoltmeterModel.RESPONDING_PARAM -> {
                    isKiloAvemResponding = value as Boolean
                    Platform.runLater { deviceStateCircleKiloAvem.fill = if (value) Color.LIME else Color.RED }
                }
                AvemVoltmeterModel.U_RMS_PARAM -> {
                    measuringU = (value as Float) * 1000
                    coef = (measuringU / (measuringULatr / 140)).toDouble()
                    val kiloAvemU = String.format("%.2f", measuringU)
                    experiment2Model!!.voltage = kiloAvemU
                }
            }

            LATR_ID -> when (param) {
                LatrModel.RESPONDING_PARAM -> {
                    isLatrResponding = value as Boolean
                    Platform.runLater { deviceStateCircleLatr.fill = if (value) Color.LIME else Color.RED }
                }
                LatrModel.STATUS_PARAM -> {
                    latrStatus = Utils.toHexString(value as Byte)
                    checkLatrError()
                    checkLatrStatus()
                }
                LatrModel.U_PARAM -> {
                    measuringULatr = (value as Float) * 140
                    val uLatr = String.format("%.2f", measuringULatr / 140)
                    experiment2Model!!.voltageARN = uLatr
                }
            }
        }
    }

    private fun checkLatrStatus() {
        when (latrStatus) {
            LATR_STARTED -> {
//                appendOneMessageToLog("Выход ЛАТРа на заданное напряжение")
            }
            LATR_WAITING -> {
                appendOneMessageToLog("Выдерживаем заданное напряжение на ЛАТРе")
            }
            LATR_CONFIG -> {
                appendOneMessageToLog("Режим кофигурации ЛАТР")
            }
            LATR_STOP_RESET -> {
//                appendOneMessageToLog("Стоп/Ресет ЛАТР")
            }
        }
    }

    private fun checkLatrError() {
        when (latrStatus) {
            LATR_UP_END -> {
                appendOneMessageToLog("Сработал верхний концевик ЛАТРа.")
            }
            LATR_DOWN_END -> {
                appendOneMessageToLog("Сработал нижний концевик ЛАТРа.")
            }
            LATR_BOTH_END -> {
                setCause("Сработали оба концевика ЛАТРа.")
            }
            LATR_TIME_ENDED -> {
                setCause("Время регулирования ЛАТРа превысило заданное.")
            }
            LATR_ZASTRYAL -> {
                appendOneMessageToLog("Застревание ЛАТРа.")
            }
        }
    }

    private fun checkEndsVFDStatus() {
        when (statusEndsVFD) {
            OMIK_UP_END -> {
//                Log.d("", "Замкнут верхний концевик ОМИКа.")
            }
            OMIK_DOWN_END -> {
//                Log.d("", "Замкнут нижний концевик ОМИКа.")
            }
            OMIK_BOTH_END -> {
                setCause("Замкнуты оба концевика ОМИКа.")
            }
            OMIK_NOONE_END -> {
//                Log.d("", "Оба концевика ОМИКа не замкнуты")
            }
        }
    }

    private fun checkVFDStatus() {
        when (statusVFD) {
            VFD_FORWARD -> {
//                Log.d("", "FORWARD")
            }
            VFD_REVERSE -> {
//                Log.d("", "REVERSE")
            }
        }
    }

    override fun setDialogStage(dialogStage: Stage) {
        this.dialogStage = dialogStage
    }

    override fun isCanceled(): Boolean {
        return isCanceled
    }
}