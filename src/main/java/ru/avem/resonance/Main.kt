package ru.avem.resonance

import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import javafx.stage.StageStyle
import ru.avem.resonance.communication.CommunicationModel
import ru.avem.resonance.controllers.LoginController
import ru.avem.resonance.controllers.MainViewController
import ru.avem.resonance.db.DataBaseRepository
import java.io.IOException


class Main : Application(), Exitappable {

    private var loginScene: Scene? = null
    private var loginWindowController: LoginController? = null
    private var mainViewScene: Scene? = null

    private var mainViewController: MainViewController? = null
    private var communicationModel: CommunicationModel? = null
    private var firstRun = true

    @Throws(IOException::class)
    override fun init() {
        communicationModel = CommunicationModel.getInstance()
        DataBaseRepository.init(false)

        createLoginScene()
        createMainViewScene()
    }


    override fun start(primaryStage: Stage) {
        PRIMARY_STAGE = primaryStage
        showMainView()
        PRIMARY_STAGE.initStyle(StageStyle.TRANSPARENT)
        PRIMARY_STAGE.show()
    }

    @Throws(Exception::class)
    override fun stop() {
        super.stop()
        communicationModel!!.setFinished(true)
    }

    @Throws(IOException::class)
    private fun createLoginScene() {
        val loginWindowLoader = FXMLLoader()
        loginWindowLoader.location = javaClass.getResource("layouts/loginWindow.fxml")
        val loginWindowParent = loginWindowLoader.load<Parent>()
        loginWindowController = loginWindowLoader.getController<LoginController>()
        loginWindowController!!.setMainApp(this)

        loginScene = Scene(loginWindowParent, Constants.Display.WIDTH.toDouble(), Constants.Display.HEIGHT.toDouble())
        loginScene!!.setOnKeyPressed { event ->
            when (event.code) {
                KeyCode.ESCAPE -> if (event.target !is TextField) {
                    Platform.exit()
                }
                else -> print("нажата кнопка не ESCAPE")
            }
        }
    }

    @Throws(IOException::class)
    private fun createMainViewScene() {
        val mainViewLoader = FXMLLoader()
        mainViewLoader.location = javaClass.getResource("layouts/mainView.fxml")
        val mainViewParent = mainViewLoader.load<AnchorPane>()
        mainViewController = mainViewLoader.getController<MainViewController>()
        mainViewController!!.setMain(this)
        mainViewScene = Scene(mainViewParent, Constants.Display.WIDTH.toDouble(), Constants.Display.HEIGHT.toDouble())
        mainViewScene!!.setOnKeyPressed { event ->
            when (event.code) {
                KeyCode.ESCAPE -> if (event.target !is TextField) {
                    exitApp()
                }
                else -> print("нажата кнопка не ESCAPE")
            }
        }
    }

    private fun showLoginView() {
        PRIMARY_STAGE.title = "Авторизация"
        loginWindowController!!.fillFields()
        if (firstRun) {
            firstRun = false
        } else {
            loginWindowController!!.clearFields()
        }
        PRIMARY_STAGE.scene = loginScene
        PRIMARY_STAGE
    }

    fun showMainView() {
        PRIMARY_STAGE.title = "ВИУ"
        PRIMARY_STAGE.scene = mainViewScene
    }

    override fun exitApp() {
        Platform.exit()
    }

    companion object {
        lateinit var PRIMARY_STAGE: Stage
        lateinit var css: String

        @JvmStatic
        fun main(args: Array<String>) {
            launch(Main::class.java)
        }
    }
}