package ru.avem.resonance.controllers

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.chart.LineChart
import javafx.scene.chart.XYChart
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import ru.avem.resonance.Constants
import ru.avem.resonance.Exitappable
import ru.avem.resonance.Main
import ru.avem.resonance.Main.Companion.PRIMARY_STAGE
import ru.avem.resonance.Main.Companion.css
import ru.avem.resonance.communication.CommunicationModel
import ru.avem.resonance.db.DataBaseRepository
import ru.avem.resonance.db.ProtocolRepository
import ru.avem.resonance.db.TestItemRepository
import ru.avem.resonance.db.model.Protocol
import ru.avem.resonance.db.model.TestItem
import ru.avem.resonance.model.MainModel
import ru.avem.resonance.model.ResultModel
import ru.avem.resonance.states.main.*
import ru.avem.resonance.utils.Toast
import ru.avem.resonance.utils.Utils
import java.io.File
import java.io.IOException
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import kotlin.collections.ArrayList
import kotlin.math.abs

class MainViewController : Statable {
    //region FXML
    @FXML
    lateinit var buttonProtocolCancel: Button
    @FXML
    lateinit var buttonProtocolNext: Button
    @FXML
    lateinit var buttonAdd: Button
    @FXML
    lateinit var buttonRemove: Button
    @FXML
    lateinit var menuBarProtocolSaveAs: MenuItem
    @FXML
    lateinit var editTestItem: MenuItem
    @FXML
    lateinit var root: AnchorPane

    @FXML
    lateinit var radioResonance: RadioButton
    @FXML
    lateinit var radioViu: RadioButton
    @FXML
    lateinit var radioViuDC: RadioButton

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
    @FXML
    lateinit var comboBoxTestItem: ComboBox<TestItem>
    @FXML
    lateinit var textFieldSerialNumber: TextField
    @FXML
    lateinit var loadDiagram: LineChart<Number, Number>
    @FXML
    lateinit var checkMenuItemTheme: CheckMenuItem

    //endregion

    //region vars
    private var mainModel: MainModel = MainModel.instance
    private var communicationModel: CommunicationModel? = null
    private var exitappable: Exitappable? = null
    private var protocolFileChooser: FileChooser? = null
    private var DBFileChooser: FileChooser? = null

    private lateinit var lastTriple: Triple<TextField, TextField, TextField>
    private val stackTriples: Stack<Triple<TextField, TextField, TextField>> = Stack()

    private var allTestItems = TestItemRepository.getAllTestItems()
    var currentTestItem: TestItem = mainModel.currentTestItem
    private val resultData = FXCollections.observableArrayList<ResultModel>()

    private val idleState = IdleState(this)
    private val waitState = WaitState(this)
    private val resultState = ResultState(this)
    private var currentState: State = idleState


    //endregion

    companion object {
        const val HEIGHT_VBOX: Int = 57
    }

    @FXML
    private fun initialize() {
        css = "white"
        if (css == "white") {
            root.stylesheets[0] = Main::class.java.getResource("styles/main_css.css").toURI().toString()
        } else {
            root.stylesheets[0] = Main::class.java.getResource("styles/main_css_black.css").toURI().toString()
        }

        mainModel = MainModel.instance
        communicationModel = CommunicationModel.getInstance()
        initData()
        protocolFileChooser = FileChooser()
        protocolFileChooser!!.initialDirectory = File(System.getProperty("user.home"))
        protocolFileChooser!!.extensionFilters.add(FileChooser.ExtensionFilter("AVEM Protocol (*.axml)", "*.axml"))

        DBFileChooser = FileChooser()
        DBFileChooser!!.initialDirectory = File(System.getProperty("user.home"))
        DBFileChooser!!.extensionFilters.add(FileChooser.ExtensionFilter("AVEM Database (*.adb)", "*.adb"))
        checkMenuItemTheme.isSelected = false
        toIdleState()
    }


    private fun toInitIdleState() {
        menuBarProtocolSaveAs.isDisable = true
        buttonProtocolCancel.isDisable = true
    }

    override fun toIdleState() {
        toInitIdleState()
        textFieldSerialNumber.clear()
        textFieldSerialNumber.isDisable = false
        comboBoxTestItem.isDisable = false
        buttonProtocolCancel.text = "Очистить"
        buttonProtocolNext.text = "Создать"
        mainModel.currentProtocol = Protocol()
        buttonProtocolCancel.isDisable = false
        currentState = idleState
        initData()
        radioResonance.isSelected = true
    }

    override fun toWaitState() {
        comboBoxTestItem.isDisable = true
        buttonProtocolCancel.text = "Новый"
        textFieldSerialNumber.isDisable = true
        buttonProtocolNext.text = "Далее"
        menuBarProtocolSaveAs.isDisable = false
        currentState = waitState
        buttonProtocolCancel.isDisable = false
    }

    override fun toResultState() {
        val currentProtocol = mainModel.currentProtocol
        currentProtocol.millis = System.currentTimeMillis()
        ProtocolRepository.insertProtocol(currentProtocol)
        Toast.makeText("Результаты проведенных испытаний сохранены").show(Toast.ToastType.INFORMATION)
        currentState = resultState
        initData()
    }

    private fun initData() {
        radioResonance.isSelected = true
        allTestItems = TestItemRepository.getAllTestItems()
        comboBoxTestItem.items.clear()
        comboBoxTestItem.selectionModel.clearSelection()
        comboBoxTestItem.items.setAll(allTestItems)
        comboBoxTestItem.selectionModel.clearSelection()
        comboBoxTestItem.selectionModel.selectFirst()
        resultData.clear()
        handleSelectTestItemExperiment()
    }

    private fun saveTestItemPoints() {
        val times: ArrayList<Double> = ArrayList()
        val voltages: ArrayList<Double> = ArrayList()
        val speeds: ArrayList<Double> = ArrayList()

        stackTriples.forEach {
            if (it.first.text.isNullOrEmpty() && it.second.text.isNullOrEmpty() && it.third.text.isNullOrEmpty() &&
                    it.first.text.toDoubleOrNull() == null && it.second.text.toDoubleOrNull() == null && it.third.text.toDoubleOrNull() == null) {
                Toast.makeText("Проверьте правильность введенных напряжений и времени проверки").show(Toast.ToastType.ERROR)
            } else if (radioResonance.isSelected && it.first.text.toDoubleOrNull()!! > 43.0) {
                Toast.makeText("Сохранить не удалось. Напряжение в этом опыте не может быть больше 43кВ. Измените данные для продолжения").show(Toast.ToastType.ERROR)
            } else if (radioViu.isSelected && it.first.text.toDoubleOrNull()!! > 60.0) {
                Toast.makeText("Сохранить не удалось. Напряжение в этом опыте не может быть больше 60кВ. Измените данные для продолжения").show(Toast.ToastType.ERROR)
            } else if (it.third.text.toDoubleOrNull()!! > 2.0) {
                Toast.makeText("Сохранить не удалось. Скорость не должна быть больше 2кВ/с").show(Toast.ToastType.ERROR)
            } else {
                voltages.add(it.first.text.toDouble())
                times.add(it.second.text.toDouble())
                speeds.add(it.third.text.toDouble())
            }
        }
        when {
            radioResonance.isSelected -> {
                currentTestItem.timesResonance = times
                currentTestItem.voltageResonance = voltages
                currentTestItem.speedResonance = speeds
            }
            radioViu.isSelected -> {
                currentTestItem.timesViu = times
                currentTestItem.voltageViu = voltages
                currentTestItem.speedViu = speeds
            }
            radioViuDC.isSelected -> {
                currentTestItem.timesViuDC = times
                currentTestItem.voltageViuDC = voltages
                currentTestItem.speedViuDC = speeds
            }
        }
        TestItemRepository.updateTestItem(currentTestItem)
    }

    @FXML
    fun handleAddTriple() {
        addTriple()
    }

    private fun addTriple() {
        lastTriple = newTextFieldsForChart()
        stackTriples.push(lastTriple)
        vBoxVoltage.children.add(lastTriple.first)
        vBoxTime.children.add(lastTriple.second)
        vBoxSpeed.children.add(lastTriple.third)
        anchorPaneTimeTorque.prefHeight += HEIGHT_VBOX
    }

    @FXML
    fun handleRemoveTriple() {
        if (stackTriples.isNotEmpty()) {
            removeTriple()
            saveTestItemPoints()
            createLoadDiagram()
        } else {
            Toast.makeText("Нет полей для удаления").show(Toast.ToastType.ERROR)
        }
    }

    private fun removeTriple() {
        lastTriple = stackTriples.pop()
        vBoxVoltage.children.remove(lastTriple.first)
        vBoxTime.children.remove(lastTriple.second)
        vBoxSpeed.children.remove(lastTriple.third)
        anchorPaneTimeTorque.prefHeight -= HEIGHT_VBOX
    }

    private fun newTextFieldsForChart(): Triple<TextField, TextField, TextField> {
        val voltage = TextField()
        voltage.isEditable = true
        voltage.prefWidth = 72.0
        voltage.maxWidth = 72.0
        voltage.setOnAction {
            changeValuesInDB()
        }

        val time = TextField()
        time.isEditable = true
        time.prefWidth = 72.0
        time.maxWidth = 72.0
        time.setOnAction {
            changeValuesInDB()
        }

        val speed = TextField()
        speed.isEditable = true
        speed.prefWidth = 72.0
        speed.maxWidth = 72.0
        speed.setOnAction {
            changeValuesInDB()
        }
        return Triple(voltage, time, speed)
    }

    private fun changeValuesInDB() {
        saveTestItemPoints()
        createLoadDiagram()
        Toast.makeText("Сделаны изменения").show(Toast.ToastType.CONFIRM)
    }

    @FXML
    fun handleSelectTestItemExperiment() {
        mainModel.currentTestItem = comboBoxTestItem.selectionModel.selectedItem
        currentTestItem = comboBoxTestItem.selectionModel.selectedItem
        removeData()
        fillStackPairs()
        createLoadDiagram()
    }

    private fun fillStackPairs() {
        when {
            radioResonance.isSelected -> {
                loadDiagram.isVisible = true
                scrollPaneTimeTorque.isVisible = true
                buttonAdd.isVisible = true
                buttonRemove.isVisible = true
                for (i in 0 until currentTestItem.timesResonance.size) {
                    handleAddTriple()
                    lastTriple.first.text = currentTestItem.voltageResonance[i].toString()
                    lastTriple.second.text = currentTestItem.timesResonance[i].toString()
                    lastTriple.third.text = currentTestItem.speedResonance[i].toString()
                }
            }
            radioViu.isSelected -> {
                loadDiagram.isVisible = true
                scrollPaneTimeTorque.isVisible = true
                buttonAdd.isVisible = true
                buttonRemove.isVisible = true
                for (i in 0 until currentTestItem.timesViu.size) {
                    handleAddTriple()
                    lastTriple.first.text = currentTestItem.voltageViu[i].toString()
                    lastTriple.second.text = currentTestItem.timesViu[i].toString()
                    lastTriple.third.text = currentTestItem.speedViu[i].toString()
                }
            }
            radioViuDC.isSelected -> {
                loadDiagram.isVisible = true
                scrollPaneTimeTorque.isVisible = true
                buttonAdd.isVisible = true
                buttonRemove.isVisible = true
                for (i in 0 until currentTestItem.timesViuDC.size) {
                    handleAddTriple()
                    lastTriple.first.text = currentTestItem.voltageViuDC[i].toString()
                    lastTriple.second.text = currentTestItem.timesViuDC[i].toString()
                    lastTriple.third.text = currentTestItem.speedViuDC[i].toString()
                }
            }
        }
    }

    private fun createLoadDiagram() {
        loadDiagram.data.clear()
        val seriesTimesAndVoltage = XYChart.Series<Number, Number>()
        var desperateDot = 0.0

        when {
            radioResonance.isSelected -> {
                if (currentTestItem.voltageResonance.isNotEmpty()) {
                    seriesTimesAndVoltage.data.add(XYChart.Data(0, 0))
                    desperateDot += abs(currentTestItem.voltageResonance[0] - 0) / currentTestItem.speedResonance[0]
                    seriesTimesAndVoltage.data.add(XYChart.Data(desperateDot, currentTestItem.voltageResonance[0]))
                    desperateDot += currentTestItem.timesResonance[0]
                    seriesTimesAndVoltage.data.add(XYChart.Data(desperateDot, currentTestItem.voltageResonance[0]))
                    for (i in 1 until currentTestItem.timesResonance.size) {
                        desperateDot += abs((currentTestItem.voltageResonance[i] - currentTestItem.voltageResonance[i - 1]) / currentTestItem.speedResonance[i])
                        seriesTimesAndVoltage.data.add(XYChart.Data(desperateDot, currentTestItem.voltageResonance[i]))
                        desperateDot += currentTestItem.timesResonance[i]
                        seriesTimesAndVoltage.data.add(XYChart.Data(desperateDot, currentTestItem.voltageResonance[i]))
                    }
                    seriesTimesAndVoltage.data.add(XYChart.Data(desperateDot + currentTestItem.voltageResonance.last() / 2, 0))
                }
            }
            radioViu.isSelected -> {
                if (currentTestItem.voltageViu.isNotEmpty()) {
                    seriesTimesAndVoltage.data.add(XYChart.Data(0, 0))
                    desperateDot += abs(currentTestItem.voltageViu[0] - 0) / currentTestItem.speedViu[0]
                    seriesTimesAndVoltage.data.add(XYChart.Data(desperateDot, currentTestItem.voltageViu[0]))
                    desperateDot += currentTestItem.timesViu[0]
                    seriesTimesAndVoltage.data.add(XYChart.Data(desperateDot, currentTestItem.voltageViu[0]))
                    for (i in 1 until currentTestItem.timesViu.size) {
                        desperateDot += abs((currentTestItem.voltageViu[i] - currentTestItem.voltageViu[i - 1]) / currentTestItem.speedViu[i])
                        seriesTimesAndVoltage.data.add(XYChart.Data(desperateDot, currentTestItem.voltageViu[i]))
                        desperateDot += currentTestItem.timesViu[i]
                        seriesTimesAndVoltage.data.add(XYChart.Data(desperateDot, currentTestItem.voltageViu[i]))
                    }
                    seriesTimesAndVoltage.data.add(XYChart.Data(desperateDot + currentTestItem.voltageViu.last() / 2, 0))
                }
            }
            radioViuDC.isSelected -> {
                if (currentTestItem.voltageViuDC.isNotEmpty()) {
                    seriesTimesAndVoltage.data.add(XYChart.Data(0, 0))
                    desperateDot += abs(currentTestItem.voltageViuDC[0] - 0) / currentTestItem.speedViuDC[0]
                    seriesTimesAndVoltage.data.add(XYChart.Data(desperateDot, currentTestItem.voltageViuDC[0]))
                    desperateDot += currentTestItem.timesViuDC[0]
                    seriesTimesAndVoltage.data.add(XYChart.Data(desperateDot, currentTestItem.voltageViuDC[0]))
                    for (i in 1 until currentTestItem.timesViuDC.size) {
                        desperateDot += abs((currentTestItem.voltageViuDC[i] - currentTestItem.voltageViuDC[i - 1]) / currentTestItem.speedViuDC[i])
                        seriesTimesAndVoltage.data.add(XYChart.Data(desperateDot, currentTestItem.voltageViuDC[i]))
                        desperateDot += currentTestItem.timesViuDC[i]
                        seriesTimesAndVoltage.data.add(XYChart.Data(desperateDot, currentTestItem.voltageViuDC[i]))
                    }
                    seriesTimesAndVoltage.data.add(XYChart.Data(desperateDot + currentTestItem.voltageViuDC.last() / 2, 0))
                }
            }
        }
        loadDiagram.data.addAll(seriesTimesAndVoltage)
    }

    private fun removeData() {
        for (i in 0 until stackTriples.size) {
            removeTriple()
        }
    }

    @FXML
    private fun handleCreateNewProtocol() {
        currentState.toIdleState()
    }

    @FXML
    private fun handleMenuBarProtocolOpen() {
        currentState.toIdleState()
        protocolFileChooser!!.title = "Выберите файл протокола"
        val file = protocolFileChooser!!.showOpenDialog(PRIMARY_STAGE)
        if (file != null) {
            openProtocolFromFile(file)
        }
    }

    private fun openProtocolFromFile(file: File) {
        try {
            val context = JAXBContext.newInstance(Protocol::class.java)
            val um = context.createUnmarshaller()
            val protocol = um.unmarshal(file) as Protocol
            comboBoxTestItem.selectionModel.select(protocol.getObject())
            mainModel.currentProtocol = protocol
            currentState.toWaitState()
            Toast.makeText(String.format("Протокол %s успешно загружен", file.name)).show(Toast.ToastType.INFORMATION)
        } catch (e: Exception) {
            Toast.makeText("Ошибка загрузки протокола").show(Toast.ToastType.ERROR)
        }
    }

    @FXML
    @Throws(IOException::class)
    private fun handleMenuBarProtocolOpenFromDB() {
        currentState.toIdleState()
        val loader = FXMLLoader()
        loader.location = Main::class.java.getResource("layouts/protocolSelector.fxml")
        val page = loader.load<Parent>()

        val dialogStage = Stage()
        dialogStage.title = "Выберите протокол из списка"
        dialogStage.initModality(Modality.WINDOW_MODAL)
        dialogStage.initOwner(PRIMARY_STAGE)
        dialogStage.isResizable = false
        val scene = Scene(page)
        dialogStage.scene = scene
        val controller = loader.getController<ProtocolSelectorController>()
        controller.setDialogStage(dialogStage)

        dialogStage.showAndWait()
        if (!controller.isCanceled) {
            mainModel.applyIntermediateProtocol()
            comboBoxTestItem.selectionModel.select(mainModel.currentProtocol.getObject())
            currentState.toWaitState()
        }
    }

    @FXML
    private fun handleMenuBarProtocolSaveAs() {
        protocolFileChooser!!.title = "Сохраните файл протокола"
        var file: File? = protocolFileChooser!!.showSaveDialog(PRIMARY_STAGE)
        if (file != null) {
            if (!file.path.endsWith(".axml")) {
                file = File(file.path + ".axml")
            }
            saveProtocolToFile(file)
        }
    }

    private fun saveProtocolToFile(file: File) {
        try {
            val context = JAXBContext.newInstance(Protocol::class.java)
            val m = context.createMarshaller()
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            m.marshal(mainModel.currentProtocol, file)
            Toast.makeText(String.format("Протокол %s успешно сохранён", file.name)).show(Toast.ToastType.INFORMATION)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(String.format("Ошибка при сохранении протокола %s", file.name)).show(Toast.ToastType.ERROR)
        }
    }

    @FXML
    fun handleExit() {
        currentState.toIdleState()
        Platform.exit()
    }

    @FXML
    @Throws(IOException::class)
    private fun handleProtocols() {
        val loader = FXMLLoader()
        loader.location = Main::class.java.getResource("layouts/protocolEditor.fxml")
        val page = loader.load<Parent>()

        val dialogStage = Stage()
        dialogStage.title = "Протоколы"
        dialogStage.initModality(Modality.WINDOW_MODAL)
        dialogStage.initOwner(PRIMARY_STAGE)
        val scene = Scene(page)
        dialogStage.scene = scene

        dialogStage.showAndWait()
    }

    @FXML
    @Throws(IOException::class)
    private fun handleMenuProfiles() {
        val loader = FXMLLoader()
        loader.location = Main::class.java.getResource("layouts/registerWindow.fxml")
        val page = loader.load<Parent>()

        val dialogStage = Stage()
        dialogStage.title = "Редактировать профиль"
        dialogStage.initModality(Modality.WINDOW_MODAL)
        dialogStage.initOwner(PRIMARY_STAGE)
        dialogStage.isResizable = false
        val scene = Scene(page)
        dialogStage.scene = scene
        dialogStage.isResizable = false
        dialogStage.showAndWait()
    }

    @FXML
    private fun handleImportDB() {
        DBFileChooser!!.title = "Выберите файл базы данных для импорта"
        val file = DBFileChooser!!.showOpenDialog(PRIMARY_STAGE)
        if (file != null) {
            importDBFromFile(file)
        }
    }

    @FXML
    private fun importDBFromFile(file: File) {
        try {
            Utils.copyFileFromFile(file, File(DataBaseRepository.DATABASE_NAME))
            Toast.makeText(String.format("База успешно импортирована из файла %s", file.absolutePath)).show(Toast.ToastType.INFORMATION)
        } catch (e: IOException) {
            Toast.makeText("Ошибка при импорте базы данных").show(Toast.ToastType.ERROR)
        }
    }

    @FXML
    private fun handleExportDB() {
        DBFileChooser!!.title = "Сохраните базу данных в файл"
        var file: File? = DBFileChooser!!.showSaveDialog(PRIMARY_STAGE)
        if (file != null) {
            if (!file.path.endsWith(".adb")) {
                file = File(file.path + ".adb")
            }
            exportDBToFile(file)
        }
    }

    private fun exportDBToFile(file: File) {
        try {
            Utils.copyFileFromFile(File(DataBaseRepository.DATABASE_NAME), file)
            Toast.makeText(String.format("База успешно экспортирована в файл %s", file.absolutePath)).show(Toast.ToastType.INFORMATION)
        } catch (e: IOException) {
            Toast.makeText("Ошибка при экспорте базы данных").show(Toast.ToastType.ERROR)
        }
    }

    @FXML
    @Throws(IOException::class)
    private fun handleDeviceState() {
        val loader = FXMLLoader()
        loader.location = Main::class.java.getResource("layouts/deviceStateWindow.fxml")
        val page = loader.load<Parent>()

        val dialogStage = Stage()
        dialogStage.title = "Состояние устройств"
        dialogStage.initModality(Modality.WINDOW_MODAL)
        dialogStage.initOwner(PRIMARY_STAGE)
        val scene = Scene(page)
        dialogStage.isResizable = false
        dialogStage.scene = scene

        dialogStage.setOnCloseRequest {
            val communicationModel = CommunicationModel.getInstance()
            communicationModel.finalizeAllDevices()
            communicationModel.deleteObservers()
            communicationModel.setDeviceStateOn(false)
        }
        dialogStage.showAndWait()
    }

    @FXML
    @Throws(IOException::class)
    private fun handleCurrentProtection() {
        val loader = FXMLLoader()
        loader.location = Main::class.java.getResource("layouts/currentProtectionWindow.fxml")
        val page = loader.load<Parent>()

        val dialogStage = Stage()
        dialogStage.title = "Состояние защит"
        dialogStage.initModality(Modality.APPLICATION_MODAL)
        dialogStage.isResizable = false
        dialogStage.scene = Scene(page)

        dialogStage.setOnCloseRequest {
            communicationModel!!.finalizeAllDevices()
            communicationModel!!.deleteObservers()
        }
        dialogStage.showAndWait()
    }

    @FXML
    private fun handleButtonProtocolCancel() {
        toIdleState()
    }

    @FXML
    private fun handleButtonProtocolNext() {
        if (comboBoxTestItem.selectionModel.selectedItem != null) {
            mainModel.createNewProtocol(textFieldSerialNumber.text, currentTestItem)
            startExperiment()
            toResultState()
        } else {
            Toast.makeText("Выберите объект испытания").show(Toast.ToastType.INFORMATION)
        }
    }

    fun handleMenuBarProtocolNew() {
        toIdleState()
    }

    private fun startExperiment() {
        if (radioResonance.isSelected) {
            startExperiment("layouts/experiment1View.fxml")
        }
        if (radioViu.isSelected) {
            startExperiment("layouts/experiment2View.fxml")
        }
        if (radioViuDC.isSelected) {
            startExperiment("layouts/experiment3View.fxml")
        }
    }

    private fun startExperiment(layout: String): Boolean {
        val loader = FXMLLoader()
        loader.location = Main::class.java.getResource(layout)
        var controller: ExperimentController? = null
        try {
            val page = loader.load<Parent>()

            val dialogStage = Stage()
            dialogStage.title = "Опыт"
            dialogStage.initModality(Modality.WINDOW_MODAL)
            dialogStage.initOwner(PRIMARY_STAGE)
            val scene = Scene(page, Constants.Display.WIDTH.toDouble(), Constants.Display.HEIGHT.toDouble())
            dialogStage.scene = scene
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            controller = loader.getController<ExperimentController>()
            controller!!.setDialogStage(dialogStage)

            dialogStage.showAndWait()


        } catch (e: IOException) {
            e.printStackTrace()
        }

        communicationModel!!.finalizeAllDevices()
        communicationModel!!.deleteObservers()

        return controller != null && controller.isCanceled
    }

    fun setMain(exitappable: Exitappable) {
        this.exitappable = exitappable
    }

    @FXML
    fun handleCheckMenuItemTheme() {
        css = if (!checkMenuItemTheme.isSelected) { //сначала галочка ставится, потом срабатывает handle
            root.stylesheets[0] = Main::class.java.getResource("styles/main_css.css").toURI().toString()
            "white"
        } else {
            root.stylesheets[0] = Main::class.java.getResource("styles/main_css_black.css").toURI().toString()
            "black"
        }
    }

    @FXML
    fun handleEditTestItem() {
        val loader = FXMLLoader()
        loader.location = Main::class.java.getResource("layouts/testItemEditor.fxml")
        val page = loader.load<Parent>()

        val dialogStage = Stage()
        dialogStage.title = "Редактор объекта испытания"
        dialogStage.initModality(Modality.WINDOW_MODAL)
        dialogStage.initOwner(PRIMARY_STAGE)
        dialogStage.isResizable = false
        val scene = Scene(page)
        dialogStage.scene = scene
        dialogStage.isResizable = false
        dialogStage.showAndWait()
        toIdleState()
    }

    @FXML
    fun handleAddTestItem() {
        val dialog = TextInputDialog("Двигатель")
        dialog.title = "Редактор типов двигателя"
        dialog.headerText = "Добавить новый тип двигателя"
        dialog.contentText = "Введите тип: "
        val result = dialog.showAndWait()
        if (result.isPresent) {
            var name = result.get()
            if (name.trim().isNotBlank()) {
                name = checkName((comboBoxTestItem.items.map { it.type }).toMutableList(), name)
                val testItem = TestItem(name)
                TestItemRepository.insertTestItem(testItem)
                comboBoxTestItem.items.add(testItem)
                comboBoxTestItem.selectionModel.selectLast()
            } else {
                Toast.makeText("Введите корректное наименование типа").show(Toast.ToastType.INFORMATION)
                handleAddTestItem()
            }
        }
    }

    private fun checkName(map: MutableList<String>, name: String, _i: Int = 1): String {
        var i = _i
        return if (map.contains(name)) {
            map.removeAll { it == name }
            val j = i - 1
            checkName(map, "${name.removeSuffix("($j)")}($i)", ++i)
        } else {
            name
        }
    }

    fun handleDeleteTestItem() {
        if (comboBoxTestItem.selectionModel.selectedItem != null) {
            TestItemRepository.deleteTestItem(currentTestItem)
            initDataForTestItem()
        } else {
            Toast.makeText("Выберите ОИ для удаления").show(Toast.ToastType.INFORMATION)
        }
    }

    private fun initDataForTestItem() {
        val allTestItems = TestItemRepository.getAllTestItems()
        comboBoxTestItem.items.clear()
        if (allTestItems.isNotEmpty()) {
            comboBoxTestItem.items.setAll(allTestItems)
            comboBoxTestItem.selectionModel.selectFirst()
        }
    }

    @FXML
    fun handleAbout() {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = "Версия ПО"
        alert.headerText = "Версия: 0.1.0"
        alert.contentText = "Дата: 02.08.2019"
        alert.showAndWait()
    }

}
