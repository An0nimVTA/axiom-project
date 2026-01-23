package com.axiom.launcher.ui

import com.axiom.launcher.ServerManager
import com.axiom.launcher.GameLauncher
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.stage.Stage

class LauncherUI : Application() {
    private val serverManager = ServerManager()
    private val gameLauncher = GameLauncher()
    
    private lateinit var serverLogArea: TextArea
    private lateinit var gameLogArea: TextArea
    private lateinit var serverButton: Button
    private lateinit var gameButton: Button
    private lateinit var usernameField: TextField
    
    override fun start(primaryStage: Stage) {
        primaryStage.title = "AXIOM Launcher"
        
        val root = VBox(20.0)
        root.padding = Insets(20.0)
        root.style = "-fx-background-color: #1e1e1e;"
        
        // Заголовок
        val title = Label("AXIOM LAUNCHER")
        title.font = Font.font("Arial", FontWeight.BOLD, 32.0)
        title.textFill = Color.GOLD
        
        // Имя игрока
        val usernameBox = HBox(10.0)
        usernameBox.alignment = Pos.CENTER
        val usernameLabel = Label("Имя игрока:")
        usernameLabel.textFill = Color.WHITE
        usernameField = TextField("Player")
        usernameField.prefWidth = 200.0
        usernameBox.children.addAll(usernameLabel, usernameField)
        
        // Кнопки управления
        val controlBox = HBox(20.0)
        controlBox.alignment = Pos.CENTER
        
        serverButton = Button("▶ Запустить сервер")
        serverButton.prefWidth = 200.0
        serverButton.style = "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;"
        serverButton.setOnAction { toggleServer() }
        
        gameButton = Button("▶ Запустить игру")
        gameButton.prefWidth = 200.0
        gameButton.style = "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px;"
        gameButton.setOnAction { toggleGame() }
        gameButton.isDisable = true // Включить после запуска сервера
        
        val playButton = Button("🎮 ИГРАТЬ")
        playButton.prefWidth = 200.0
        playButton.prefHeight = 50.0
        playButton.style = "-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;"
        playButton.setOnAction { playGame() }
        
        controlBox.children.addAll(serverButton, gameButton, playButton)
        
        // Логи сервера
        val serverLogLabel = Label("Логи сервера:")
        serverLogLabel.textFill = Color.WHITE
        serverLogArea = TextArea()
        serverLogArea.isEditable = false
        serverLogArea.prefHeight = 200.0
        serverLogArea.style = "-fx-control-inner-background: #2e2e2e; -fx-text-fill: #00ff00;"
        
        // Логи игры
        val gameLogLabel = Label("Логи игры:")
        gameLogLabel.textFill = Color.WHITE
        gameLogArea = TextArea()
        gameLogArea.isEditable = false
        gameLogArea.prefHeight = 200.0
        gameLogArea.style = "-fx-control-inner-background: #2e2e2e; -fx-text-fill: #00aaff;"
        
        root.children.addAll(
            title,
            usernameBox,
            controlBox,
            serverLogLabel,
            serverLogArea,
            gameLogLabel,
            gameLogArea
        )
        
        val scene = Scene(root, 800.0, 700.0)
        primaryStage.scene = scene
        primaryStage.show()
        
        // Закрыть всё при выходе
        primaryStage.setOnCloseRequest {
            serverManager.stopServer()
            gameLauncher.stopGame()
            Platform.exit()
        }
    }
    
    private fun toggleServer() {
        if (serverManager.isRunning()) {
            serverManager.stopServer()
            serverButton.text = "▶ Запустить сервер"
            serverButton.style = "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;"
            gameButton.isDisable = true
        } else {
            serverManager.startServer { log ->
                Platform.runLater {
                    serverLogArea.appendText("$log\n")
                    
                    // Включить кнопку игры когда сервер готов
                    if (log.contains("Done") && log.contains("For help")) {
                        gameButton.isDisable = false
                    }
                }
            }
            serverButton.text = "⏹ Остановить сервер"
            serverButton.style = "-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px;"
        }
    }
    
    private fun toggleGame() {
        if (gameLauncher.isRunning()) {
            gameLauncher.stopGame()
            gameButton.text = "▶ Запустить игру"
            gameButton.style = "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px;"
        } else {
            val username = usernameField.text.ifEmpty { "Player" }
            gameLauncher.launchGame(username) { log ->
                Platform.runLater {
                    gameLogArea.appendText("$log\n")
                }
            }
            gameButton.text = "⏹ Остановить игру"
            gameButton.style = "-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px;"
        }
    }
    
    private fun playGame() {
        // Запустить всё одной кнопкой
        if (!serverManager.isRunning()) {
            toggleServer()
        }
        
        // Подождать 5 секунд и запустить игру
        Thread {
            Thread.sleep(5000)
            if (!gameLauncher.isRunning()) {
                Platform.runLater { toggleGame() }
            }
        }.start()
    }
}
