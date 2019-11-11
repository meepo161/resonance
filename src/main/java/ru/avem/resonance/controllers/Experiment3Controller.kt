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
import ru.avem.resonance.Constants.Time.MILLS_IN_SEC
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
import ru.avem.resonance.model.Experiment3Model
import ru.avem.resonance.model.MainModel
import ru.avem.resonance.model.Point
import ru.avem.resonance.utils.Toast
import ru.avem.resonance.utils.Utils.sleep
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class Experiment3Controller : DeviceState(), ExperimentController {

    @FXML
    lateinit var tableViewExperiment3: TableView<Experiment3Model>
    @FXML
    lateinit var tableColumnU: TableColumn<Experiment3Model, String>
    @FXML
    lateinit var tableColumnIB: TableColumn<Experiment3Model, String>
    @FXML
    lateinit var tableColumnUOI: TableColumn<Experiment3Model, String>
    @FXML
    lateinit var tableColumnIOI: TableColumn<Experiment3Model, String>
    @FXML
    lateinit var tableColumnResultExperiment3: TableColumn<Experiment3Model, String>
    @FXML
    lateinit var textAreaExperiment3Log: TextArea
    @FXML
    lateinit var lineChartExperiment3: LineChart<Number, Number>
    @FXML
    lateinit var xAxis: NumberAxis
    @FXML
    lateinit var yAxis: NumberAxis
    @FXML
    lateinit var buttonStartStop: Button
    @FXML
    lateinit var buttonNext: Button
    @FXML
    lateinit var buttonCancelAll: Button
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
    private var experiment3Model: Experiment3Model? = null
    private val experiment3Data = FXCollections.observableArrayList<Experiment3Model>()

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
    private var isDeltaReady50: Boolean = false
    @Volatile
    private var isDeltaReady0: Boolean = false
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
    private var measuringIAvem: Float = 0.0f
    @Volatile
    private var measuringIA: Float = 0.0f
    @Volatile
    private var measuringIB: Float = 0.0f
    @Volatile
    private var measuringIC: Float = 0.0f
    @Volatile
    private var isSchemeReady: Boolean = false
    @Volatile
    private var isStartButtonOn: Boolean = false
    @Volatile
    private var measuringF: Float = 0.0f
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
    private var ручнойРежим: Boolean = false
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
    private var currentDot = XYChart.Series<Number, Number>()

    private val firstVoltage = 1500.0f

    private var currentTestItem: TestItem = mainModel.currentTestItem
    private var timePassed = 0.0
    private var time = 0.0
    private var timeSum = 0.0
    private var seriesTimesAndVoltage = XYChart.Series<Number, Number>()
    private var realTime = 0.0

    private var r: Random = Random()

    private var duty: Float = 0.0f
    private var pulse: Float = 0.0f

    private val isThereAreAccidents: Boolean
        get() {
            if (isCanceled) {
                isExperimentRunning = false
                isExperimentEnded = true
            }
            return !isCanceled
        }

    private val isDevicesResponding: Boolean
        //        get() = true
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
        experiment3Model = mainModel.experiment3Model
        experiment3Data.add(experiment3Model)
        tableViewExperiment3.items = experiment3Data
        tableViewExperiment3.selectionModel = null
        communicationModel.addObserver(this)
        voltageList = currentProtocol.voltageViuDC
        timeList = currentProtocol.timesViuDC
        speedList = currentProtocol.speedViuDC
        tableColumnU.setCellValueFactory { cellData -> cellData.value.voltageARNProperty() }
        tableColumnIB.setCellValueFactory { cellData -> cellData.value.currentBProperty() }
        tableColumnUOI.setCellValueFactory { cellData -> cellData.value.voltageProperty() }
        tableColumnIOI.setCellValueFactory { cellData -> cellData.value.currentOIProperty() }
        tableColumnResultExperiment3.setCellValueFactory { cellData -> cellData.value.resultProperty() }
        fillStackPairs()
        lineChartExperiment3.data.add(seriesTimesAndVoltage)

    }

    private fun fillStackPairs() {
        for (i in 0 until currentTestItem.timesViuDC.size) {
            addPair()
            lastTriple.first.text = currentTestItem.timesViuDC[i].toString()
            lastTriple.second.text = currentTestItem.voltageViuDC[i].toString()
            lastTriple.third.text = currentTestItem.speedViuDC[i].toString()
        }
    }

    @FXML
    fun handleAddPair() {
        addPair()
    }

    private fun addPair() {
        lastTriple = newTextFieldsForChart()
        stackTriples.push(lastTriple)
        vBoxTime.children.add(lastTriple.first)
        vBoxVoltage.children.add(lastTriple.second)
        vBoxSpeed.children.add(lastTriple.third)
        anchorPaneTimeTorque.prefHeight += MainViewController.HEIGHT_VBOX
    }

    @FXML
    fun handleRemovePair() {
        if (stackTriples.isNotEmpty()) {
            removePair()
            saveTestItemPoints()
        } else {
            Toast.makeText("Нет полей для удаления").show(Toast.ToastType.ERROR)
        }
    }

    private fun removePair() {
        lastTriple = stackTriples.pop()
        vBoxTime.children.remove(lastTriple.first)
        vBoxVoltage.children.remove(lastTriple.second)
        vBoxSpeed.children.remove(lastTriple.third)
        anchorPaneTimeTorque.prefHeight -= MainViewController.HEIGHT_VBOX
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
            if (!it.first.text.isNullOrEmpty() && !it.second.text.isNullOrEmpty() && !it.third.text.isNullOrEmpty() &&
                    it.first.text.toDoubleOrNull() != null && it.second.text.toDoubleOrNull() != null && it.third.text.toDoubleOrNull() != null) {
                times.add(it.first.text.toDouble())
                voltages.add(it.second.text.toDouble())
                speeds.add(it.third.text.toDouble())
            } else {
                Toast.makeText("Проверьте правильность введенных напряжений и времени проверки").show(Toast.ToastType.WARNING)
            }
        }
        currentTestItem.timesViuDC = times
        currentTestItem.voltageViuDC = voltages
        currentTestItem.speedViuDC = speeds

        voltageList = currentProtocol.voltageViuDC
        timeList = currentProtocol.timesViuDC
        speedList = currentProtocol.speedViuDC

        TestItemRepository.updateTestItem(currentTestItem)
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
        currentProtocol.typeExperiment = "ВИУ постоянным напряжением"
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

    @FXML
    private fun handleExperimentCancel() {
        fillProtocolExperimentFields()
        isExperimentRunning = false
        dialogStage!!.close()
    }

    private fun stopExperiment() {
        isNeedToRefresh = false
        buttonStartStop.isDisable = true
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
        buttonStartStop.text = "Остановить"
        buttonNext.isDisable = true
        buttonCancelAll.isDisable = true
        experiment3Model!!.clearProperties()
        isSchemeReady = false
        cause = ""
        isControlRubilNeed = false

        Thread {

            if (isExperimentRunning) {
                appendOneMessageToLog("Визуально осматривайте трансфоматор на наличие потеков масла перед каждым опытом")
                appendOneMessageToLog("Инициализация системы")
                communicationModel.initOwenPrController()
                communicationModel.initExperimentDevices()
            }

            while (!isDevicesResponding) {
                sleep(10)
            }

            if (isExperimentRunning) {
                communicationModel.setKiloAvemShowValue(Constants.Avem.VOLTAGE_AMP.ordinal)
                communicationModel.resetLATR()
                communicationModel.таймер_On()
                communicationModel.таймер_Off()
                communicationModel.таймер_On()
                communicationModel.таймер_Off()
                communicationModel.разрешениеНаЗапуск_On()
            }

            if (isExperimentRunning && isDevicesResponding) {
                appendOneMessageToLog("Нажмите кнопку ПУСК")
            }

            while (!контрольПуска && isExperimentRunning && isDevicesResponding) {
                communicationModel.разрешениеНаЗапуск_On()
                sleep(10)
            }

            if (!контрольРубильника && isExperimentRunning && isDevicesResponding) {
                sleep(1000)
                communicationModel.разрешениеНаЗапуск_Off()
                appendOneMessageToLog("Поднимите рубильник силового питания")
            }

            while (!контрольРубильника && isExperimentRunning && isDevicesResponding) {
                sleep(10)
            }

            if (!контрольПуска && isExperimentRunning && isDevicesResponding) {
                communicationModel.разрешениеНаЗапуск_On()
                appendOneMessageToLog("Нажмите кнопку ПУСК")
            }

            while (!контрольПуска && isExperimentRunning && isDevicesResponding) {
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
//                communicationModel.последовательнаяСхема_On()
                communicationModel.короткозамыкатель_On()
                communicationModel.авэм_On()
            }

            timeSum = 0.0

            if (isExperimentRunning && isDevicesResponding) {
                createLoadDiagram()
                for (i in voltageList.indices) {
                    stackTriples[i].second.isDisable = true
                    timePassed = 0.0
                    if (isExperimentRunning && isDevicesResponding) {
                        appendOneMessageToLog("Началась регулировка")
                        putUpLatr(voltageList[i].toFloat() * 1000, 150)
                        if (measuringULatr < measuringU * 0.5 && measuringULatr * 0.5 > measuringU) {
                            setCause("Коэфицент трансформации сильно отличается")
                        }
                        appendOneMessageToLog("Регулировка окончена")
                    }

                    communicationModel.таймер_On()
                    time = currentTestItem.timesViuDC[i]
                    while (isExperimentRunning && timePassed < time) {
                        time = currentTestItem.timesViuDC[i]
                        sleep(1000)
                        timePassed += 1
                        if (time != stackTriples[i].second.text.toDouble()) {
                            time = currentTestItem.timesViuDC[i]
                        }
                    }
                    fillPointData()

                    voltageList = currentTestItem.voltageViuDC
                    timeSum += currentTestItem.timesViuDC[i]
                    stackTriples[i].first.isDisable = true
                    communicationModel.таймер_Off()
                }
            }

            isNeedToRefresh = false
            communicationModel.resetLATR()
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

            timeToSleep = 200
            while (isExperimentRunning && (timeToSleep-- > 0)) {
                sleep(10)
            }

            communicationModel.finalizeAllDevices()

            if (cause != "") {
                appendMessageToLog(String.format("Испытание прервано по причине: %s", cause))
                experiment3Model!!.result = "Завершено"
            } else if (!isDevicesResponding) {
                appendMessageToLog(getNotRespondingDevicesString("Испытание прервано по причине: потеряна связь с устройствами"))
                experiment3Model!!.result = "Завершено"
            } else {
                experiment3Model!!.result = "Завершено"
                appendMessageToLog("Испытание завершено успешно")
            }
            appendMessageToLog("\n------------------------------------------------\n")

            Platform.runLater()
            {
                buttonStartStop.text = "Запустить"
                isExperimentEnded = true
                isExperimentRunning = false
                buttonStartStop.isDisable = false
                buttonNext.isDisable = false
                buttonCancelAll.isDisable = false
            }
        }.start()
    }

    private fun fillPointData() {
        points.add(Point(measuringU.toDouble(), measuringIC.toDouble(), currentProtocol.dayTime))
        currentProtocol.points = points
    }

    private fun putUpLatr(voltage: Float, difference: Int) {
        communicationModel.startUpLATRUp((voltage / coef).toFloat(), false)
        while (measuringU < voltage * 0.5 && measuringU < voltage * 1.5 && isExperimentRunning) {
            sleep(10)
        }
        waitingLatrCoarse(voltage)
        fineLatr(voltage)
    }

    private fun selectDutyAndPulseForLatr(i: Int) {
        if (currentTestItem.speedViuDC[i] < 0.4) {
            duty = 25.0f
            pulse = 50.0f
        } else if (currentTestItem.speedViuDC[i] >= 0.4 && currentTestItem.speedViuDC[i] < 0.5) {
            duty = 25.0f
            pulse = 56.0f
        } else if (currentTestItem.speedViuDC[i] >= 0.5 && currentTestItem.speedViuDC[i] < 0.6) {
            duty = 25.0f
            pulse = 64.0f
        } else if (currentTestItem.speedViuDC[i] >= 0.6 && currentTestItem.speedViuDC[i] < 0.7) {
            duty = 25.0f
            pulse = 70.0f
        } else if (currentTestItem.speedViuDC[i] >= 0.7 && currentTestItem.speedViuDC[i] < 0.8) {
            duty = 25.0f
            pulse = 74.0f
        } else if (currentTestItem.speedViuDC[i] >= 0.8 && currentTestItem.speedViuDC[i] < 0.9) {
            duty = 25.0f
            pulse = 79.0f
        } else if (currentTestItem.speedViuDC[i] >= 0.9 && currentTestItem.speedViuDC[i] < 1.0) {
            duty = 25.0f
            pulse = 85.0f
        } else if (currentTestItem.speedViuDC[i] >= 1.0 && currentTestItem.speedViuDC[i] < 1.1) {
            duty = 25.0f
            pulse = 89.0f
        } else if (currentTestItem.speedViuDC[i] >= 1.1 && currentTestItem.speedViuDC[i] < 1.2) {
            duty = 25.0f
            pulse = 94.0f
        } else if (currentTestItem.speedViuDC[i] >= 1.2 && currentTestItem.speedViuDC[i] < 1.3) {
            duty = 28.0f
            pulse = 86.0f
        } else if (currentTestItem.speedViuDC[i] >= 1.3 && currentTestItem.speedViuDC[i] < 1.4) {
            duty = 28.0f
            pulse = 88.0f
        } else if (currentTestItem.speedViuDC[i] >= 1.4 && currentTestItem.speedViuDC[i] < 1.5) {
            duty = 28.0f
            pulse = 90.0f
        } else if (currentTestItem.speedViuDC[i] >= 1.5 && currentTestItem.speedViuDC[i] < 1.6) {
            duty = 28.0f
            pulse = 92.0f
        } else if (currentTestItem.speedViuDC[i] >= 1.6 && currentTestItem.speedViuDC[i] < 1.7) {
            duty = 28.0f
            pulse = 95.0f
        } else if (currentTestItem.speedViuDC[i] >= 1.7 && currentTestItem.speedViuDC[i] < 1.8) {
            duty = 32.0f
            pulse = 90.0f
        } else if (currentTestItem.speedViuDC[i] >= 1.8 && currentTestItem.speedViuDC[i] < 1.9) {
            duty = 33.0f
            pulse = 91.0f
        } else if (currentTestItem.speedViuDC[i] <= 2.0) {
            duty = 34.0f
            pulse = 91.0f
        }
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
            textAreaExperiment3Log.appendText(String.format("%s \t| %s\n", sdf.format(System.currentTimeMillis()), message))
        }
    }

    private fun appendOneMessageToLog(message: String) {
        if (logBuffer == null || logBuffer != message) {
            logBuffer = message
            appendMessageToLog(message)
        }
    }

    private fun getAccidentsString(mainText: String): String {
        return String.format("%s: %s",
                mainText,
//                if (is1DoorOn) "" else "открыта дверь, ",
//                if (is2OIOn) "" else "открыты концевики ОИ, ",
//                if (is6KM1_2_On) "" else "не замкнулся КМ1, ",
//                if (is7KM2_2_On) "" else "не замкнулся КМ2, ",
                if (isCanceled) "" else "нажата кнопка отмены, ")
    }

    private fun getNotRespondingDevicesString(mainText: String): String {
        return String.format("%s %s%s%s%s%s%s",
                mainText,
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
        val modelId = (values as Array<Any>)[0] as Int
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
                    val IB = String.format("%.2f", measuringIB)
                    experiment3Model!!.currentB = IB
                    if (measuringIB > 45) {
                        appendMessageToLog("Ток B превышает 45А")
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
                AvemVoltmeterModel.U_RMS_PARAM -> {
                    measuringIC = value as Float
                    val IC = String.format("%.4f", measuringIC)
                    experiment3Model!!.currentOI = IC
                    if (measuringIC > 45) {
                        appendMessageToLog("Ток C превышает 45А")
                    }
                }
            }

            KILOAVEM_ID -> when (param) {
                AvemVoltmeterModel.RESPONDING_PARAM -> {
                    isKiloAvemResponding = value as Boolean
                    Platform.runLater { deviceStateCircleKiloAvem.fill = if (value) Color.LIME else Color.RED }
                }
                AvemVoltmeterModel.U_AMP_PARAM -> {
                    measuringU = abs((value as Float) * 1000)
                    coef = (measuringU / (measuringULatr / 102)).toDouble()
                    val kiloAvemU = String.format("%.2f", measuringU)
                    experiment3Model!!.voltage = kiloAvemU
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
                    measuringULatr = (value as Float) * 102
                    val uLatr = String.format("%.2f", measuringULatr / 102)
                    experiment3Model!!.voltageARN = uLatr
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