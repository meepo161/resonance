package ru.avem.resonance.controllers

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.SortedList
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.AnchorPane
import javafx.stage.FileChooser
import ru.avem.resonance.Main
import ru.avem.resonance.Main.Companion.css
import ru.avem.resonance.db.ProtocolRepository
import ru.avem.resonance.db.model.Protocol
import ru.avem.resonance.logging.Logging
import ru.avem.resonance.model.MainModel
import ru.avem.resonance.utils.Toast
import ru.avem.resonance.utils.Utils.openFile
import java.io.File

class ProtocolEditorController {
    @FXML
    lateinit var root: AnchorPane

    @FXML
    lateinit var filterField: TextField

    @FXML
    lateinit var tableProtocols: TableView<Protocol>
    @FXML
    lateinit var columnProtocolID: TableColumn<Protocol, String>
    @FXML
    lateinit var columnProtocolSerialNumber: TableColumn<Protocol, Double>
    @FXML
    lateinit var columnProtocolDate: TableColumn<Protocol, String>
    @FXML
    lateinit var columnProtocolFullName1: TableColumn<Protocol, Double>
    @FXML
    lateinit var columnProtocolFullName2: TableColumn<Protocol, Double>


    private var mainModel: MainModel? = null
    private var protocols: ObservableList<Protocol>? = null

    @FXML
    private fun initialize() {
        if (css == "white") {
            root.stylesheets[0] = Main::class.java.getResource("styles/main_css.css").toURI().toString()
        } else {
            root.stylesheets[0] = Main::class.java.getResource("styles/main_css_black.css").toURI().toString()
        }
        mainModel = MainModel.instance
        initData()

        columnProtocolID.cellValueFactory = PropertyValueFactory("id")
        columnProtocolSerialNumber.cellValueFactory = PropertyValueFactory("serialNumber")
        columnProtocolDate.cellValueFactory = PropertyValueFactory("date")
        columnProtocolFullName1.cellValueFactory = PropertyValueFactory("position1FullName")
        columnProtocolFullName2.cellValueFactory = PropertyValueFactory("position2FullName")

        val filteredData = FilteredList(protocols!!) { p -> true }

        filterField.textProperty().addListener { observable, oldValue, newValue ->
            filteredData.setPredicate { protocol ->
                if (newValue == null || newValue.isEmpty()) {
                    return@setPredicate true
                }

                val lowerCaseFilter = newValue.toLowerCase()

                if (protocol.id.toString().contains(lowerCaseFilter)) {
                    return@setPredicate true
                } else if (protocol.serialNumber.toLowerCase().contains(lowerCaseFilter)) {
                    return@setPredicate true
                } else if (protocol.date.toLowerCase().contains(lowerCaseFilter)) {
                    return@setPredicate true
                } else if (protocol.position1FullName.toLowerCase().contains(lowerCaseFilter)) {
                    return@setPredicate true
                } else if (protocol.position2FullName.toLowerCase().contains(lowerCaseFilter)) {
                    return@setPredicate true
                }
                false
            }
        }

        val sortedData = SortedList(filteredData)

        sortedData.comparatorProperty().bind(tableProtocols.comparatorProperty())

        tableProtocols.items = sortedData
    }

    private fun initData() {
        val allProtocols = ProtocolRepository.getAllProtocols()
        protocols = FXCollections.observableArrayList(allProtocols)
    }

    @FXML
    private fun handleProtocolOpen() {
        val selectedIndex = tableProtocols.selectionModel.selectedIndex
        val protocol = tableProtocols.selectionModel.selectedItem
        if (selectedIndex >= 0) {
            openFile(Logging.getTempWorkbook(protocol))
        } else {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Не выбрано"
            alert.headerText = "Протокол не выбран"
            alert.contentText = "Пожалуйста выберите протокол в таблице"

            alert.showAndWait()
        }

    }

    @FXML
    private fun handleProtocolSaveAs() {
        if (tableProtocols.selectionModel.selectedIndex >= 0) {
            val protocolFileChooser = FileChooser()
            protocolFileChooser.initialDirectory = File(System.getProperty("user.home"))
            protocolFileChooser.extensionFilters.add(
                    FileChooser.ExtensionFilter("AVEM Protocol (*.xlsx)", "*.xlsx"))

            val file = protocolFileChooser.showSaveDialog(root.scene.window)
            if (!Logging.writeWorkbookToFile(tableProtocols.selectionModel.selectedItem, file)) {
                Toast.makeText("При попытке сохранения протокола произошла ошибка").show(Toast.ToastType.ERROR)
            } else {
                Toast.makeText("Протокол успешно сохранён").show(Toast.ToastType.INFORMATION)
            }
        } else {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Не выбрано"
            alert.headerText = "Протокол не выбран"
            alert.contentText = "Пожалуйста выберите протокол в таблице"

            alert.showAndWait()
        }

    }


    @FXML
    private fun handleProtocolDelete() {
        val selectedIndex = tableProtocols.selectionModel.selectedIndex
        val protocol = tableProtocols.selectionModel.selectedItem
        if (selectedIndex >= 0) {
            protocols!!.removeAt(selectedIndex)
            ProtocolRepository.deleteProtocol(protocol)
            mainModel!!.isNeedRefresh = true
        } else {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Не выбрано"
            alert.headerText = "Протокол не выбран"
            alert.contentText = "Пожалуйста выберите протокол в таблице"

            alert.showAndWait()
        }
    }
}
