package com.axiom.launcher.ui

import com.axiom.launcher.api.ApiClient
import com.axiom.launcher.config.ConfigManager
import com.axiom.launcher.minecraft.MinecraftLauncher
import com.axiom.launcher.model.*
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.effect.DropShadow
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.stage.Stage
import kotlinx.coroutines.*
import java.io.File

class LauncherApp : Application() {
    
    private val api = ApiClient()
    private val launcher = MinecraftLauncher("http://localhost:5000")
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var currentUser: User? = null
    private var servers: List<Server> = emptyList()
    private var newsList: List<NewsItem> = emptyList()
    private var selectedServer: Server? = null
    private var serverProcess: Process? = null
    private var autoLaunchTriggered = false
    
    private lateinit var stage: Stage
    private lateinit var contentArea: StackPane
    private lateinit var navItems: MutableList<HBox>
    private var currentTab = "servers"

    companion object {
        const val BG = "#0A0A0A"
        const val SURFACE = "#111111"
        const val CARD = "#1A1A1A"
        const val CARD_HOVER = "#222222"
        const val BORDER = "#2A2A2A"
        const val BORDER_LIGHT = "#3A3A3A"
        const val TEXT = "#FFFFFF"
        const val TEXT_SEC = "#888888"
        const val TEXT_DIM = "#555555"
        const val SUCCESS = "#4ADE80"
        const val ERROR = "#F87171"
    }
    
    override fun start(primaryStage: Stage) {
        stage = primaryStage
        ConfigManager.load()
        
        if (ConfigManager.config.token != null) {
            if (ConfigManager.config.token == "OFFLINE") {
                currentUser = User(0, ConfigManager.config.lastUser ?: "Player", "OFFLINE")
                showMainView()
            } else {
                api.setToken(ConfigManager.config.token)
                currentUser = User(0, ConfigManager.config.lastUser ?: "Player", ConfigManager.config.token)
                showMainView()
            }
        } else {
            if (ConfigManager.config.autoLaunch && !ConfigManager.config.lastUser.isNullOrBlank()) {
                currentUser = User(0, ConfigManager.config.lastUser ?: "Player", "OFFLINE")
                showMainView()
            } else {
                showLoginView()
            }
        }
        
        stage.title = "AXIOM"
        stage.show()
    }
    
    private fun showLoginView() {
        val root = StackPane().apply { style = "-fx-background-color: $BG;" }
        
        val card = VBox(24.0).apply {
            alignment = Pos.CENTER
            padding = Insets(48.0)
            maxWidth = 400.0
            style = "-fx-background-color: $SURFACE; -fx-background-radius: 16; -fx-border-color: $BORDER; -fx-border-radius: 16;"
            effect = DropShadow(40.0, Color.rgb(0, 0, 0, 0.5))
        }
        
        val logo = Label("AXIOM").apply { font = Font.font("System", FontWeight.BOLD, 32.0); textFill = Color.WHITE }
        val sub = Label("Войдите чтобы продолжить").apply { font = Font.font(14.0); textFill = Color.web(TEXT_SEC) }
        
        val loginField = createInput("Логин")
        val passField = createPasswordInput("Пароль")
        val error = Label().apply { font = Font.font(13.0); textFill = Color.web(ERROR) }
        
        val loginBtn = createButton("Войти", true).apply {
            setOnAction { doLogin(loginField.text, passField.text, error, this) }
        }
        val regBtn = createButton("Создать аккаунт", false).apply {
            setOnAction { doRegister(loginField.text, passField.text, error, this) }
        }
        
        val offlineBtn = createButton("Войти Оффлайн", false).apply {
            style = "-fx-background-color: transparent; -fx-text-fill: #555; -fx-font-size: 12px; -fx-border-width: 0;"
            cursor = Cursor.HAND
            setOnMouseEntered { textFill = Color.web(TEXT_SEC) }
            setOnMouseExited { textFill = Color.web("#555") }
            setOnAction { doOfflineLogin(loginField.text, error) }
        }
        
        passField.setOnAction { doLogin(loginField.text, passField.text, error, loginBtn) }
        
        card.children.addAll(logo, sub, Region().apply { prefHeight = 16.0 }, loginField, passField, error, Region().apply { prefHeight = 8.0 }, loginBtn, regBtn, offlineBtn)
        root.children.add(card)
        stage.scene = Scene(root, 960.0, 640.0)
    }
    
    private fun showMainView() {
        val root = BorderPane().apply { style = "-fx-background-color: $BG;" }
        
        navItems = mutableListOf()
        
        val sidebar = VBox(8.0).apply {
            prefWidth = 200.0
            padding = Insets(24.0, 12.0, 24.0, 12.0)
            style = "-fx-background-color: $SURFACE; -fx-border-color: $BORDER; -fx-border-width: 0 1 0 0;"
        }
        
        val logoBox = HBox(10.0).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(0.0, 0.0, 24.0, 8.0)
            children.addAll(
                Label("◈").apply { font = Font.font(18.0); textFill = Color.WHITE },
                Label("AXIOM").apply { font = Font.font("System", FontWeight.BOLD, 16.0); textFill = Color.WHITE }
            )
        }
        
        val navServers = createNavItem("⬡  Серверы", "servers")
        val navNews = createNavItem("◧  Новости", "news")
        val navSettings = createNavItem("⚙  Настройки", "settings")
        
        navItems.addAll(listOf(navServers, navNews, navSettings))
        updateNavSelection()
        
        sidebar.children.addAll(logoBox, navServers, navNews, navSettings, Region().apply { VBox.setVgrow(this, Priority.ALWAYS) }, createUserBox())
        
        contentArea = StackPane()
        showServersTab()
        
        root.left = sidebar
        root.center = contentArea
        
        stage.scene = Scene(root, 1000.0, 650.0)
        loadData()
    }
    
    private fun showServersTab() {
        currentTab = "servers"
        updateNavSelection()
        
        val content = VBox(20.0).apply { padding = Insets(32.0) }
        
        val header = Label("Серверы").apply { font = Font.font("System", FontWeight.BOLD, 24.0); textFill = Color.WHITE }
        
        val serverListBox = VBox(12.0)
        servers.forEach { serverListBox.children.add(createServerCard(it, serverListBox)) }
        if (servers.isEmpty()) serverListBox.children.add(Label("Загрузка...").apply { textFill = Color.web(TEXT_SEC) })
        
        val playCard = VBox(16.0).apply {
            padding = Insets(24.0)
            alignment = Pos.CENTER
            style = "-fx-background-color: $CARD; -fx-background-radius: 12; -fx-border-color: $BORDER; -fx-border-radius: 12;"
        }
        
        val statusLabel = Label(if (selectedServer != null) "Выбран: ${selectedServer?.name}" else "Выберите сервер").apply {
            font = Font.font(14.0); textFill = Color.web(TEXT_SEC)
        }

        val progressBar = ProgressBar(0.0).apply {
            prefWidth = 220.0
            isVisible = false
            style = "-fx-accent: $SUCCESS;"
        }
        
        val playBtn = createButton("▶  Играть", true).apply {
            prefWidth = 180.0
            isDisable = selectedServer?.online != true
            setOnAction { launchGame(statusLabel, progressBar, this) }
        }

        val startServerBtn = createButton("🛠  Запустить сервер", false).apply {
            prefWidth = 180.0
            isDisable = resolveServerStartScript() == null
            setOnAction { startLocalServer(statusLabel, this) }
        }
        
        playCard.children.addAll(statusLabel, progressBar, playBtn, startServerBtn)
        content.children.addAll(header, serverListBox, Region().apply { VBox.setVgrow(this, Priority.ALWAYS) }, playCard)
        
        contentArea.children.setAll(content)
        maybeAutoLaunch(statusLabel, progressBar, playBtn)
    }
    
    private fun showNewsTab() {
        currentTab = "news"
        updateNavSelection()
        
        val content = VBox(20.0).apply { padding = Insets(32.0) }
        
        val header = Label("Новости").apply { font = Font.font("System", FontWeight.BOLD, 24.0); textFill = Color.WHITE }
        
        val newsBox = VBox(12.0)
        if (newsList.isEmpty()) {
            newsBox.children.add(Label("Нет новостей").apply { textFill = Color.web(TEXT_SEC) })
        } else {
            newsList.forEach { news ->
                newsBox.children.add(VBox(8.0).apply {
                    padding = Insets(20.0)
                    style = "-fx-background-color: $CARD; -fx-background-radius: 10; -fx-border-color: $BORDER; -fx-border-radius: 10;"
                    children.addAll(
                        Label(news.title).apply { font = Font.font("System", FontWeight.BOLD, 16.0); textFill = Color.WHITE; isWrapText = true },
                        Label(news.content).apply { font = Font.font(14.0); textFill = Color.web(TEXT_SEC); isWrapText = true },
                        Label(news.date).apply { font = Font.font(12.0); textFill = Color.web(TEXT_DIM) }
                    )
                })
            }
        }
        
        content.children.addAll(header, newsBox)
        contentArea.children.setAll(content)
    }
    
    private fun showSettingsTab() {
        currentTab = "settings"
        updateNavSelection()
        
        val content = VBox(24.0).apply { padding = Insets(32.0) }
        
        val header = Label("Настройки").apply { font = Font.font("System", FontWeight.BOLD, 24.0); textFill = Color.WHITE }
        
        val card = VBox(20.0).apply {
            padding = Insets(24.0)
            style = "-fx-background-color: $CARD; -fx-background-radius: 12; -fx-border-color: $BORDER; -fx-border-radius: 12;"
        }
        
        val javaLabel = Label("Путь к Java").apply { font = Font.font(14.0); textFill = Color.WHITE }
        val javaField = createInput("java").apply { text = ConfigManager.config.javaPath; prefWidth = 400.0 }
        
        val ipLabel = Label("IP Адрес Сервера (для оффлайн)").apply { font = Font.font(14.0); textFill = Color.WHITE }
        val ipField = createInput("localhost").apply { text = ConfigManager.config.serverAddress; prefWidth = 400.0 }
        
        val ramLabel = Label("Оперативная память: ${ConfigManager.config.maxRam} MB").apply { font = Font.font(14.0); textFill = Color.WHITE }
        val ramSlider = Slider(1024.0, 16384.0, ConfigManager.config.maxRam.toDouble()).apply {
            prefWidth = 400.0
            isShowTickMarks = true
            majorTickUnit = 2048.0
            style = "-fx-control-inner-background: $CARD;"
        }
        ramSlider.valueProperty().addListener { _, _, v -> ramLabel.text = "Оперативная память: ${v.toInt()} MB" }

        val serverLabel = Label("Скрипт запуска сервера (для тестов)").apply { font = Font.font(14.0); textFill = Color.WHITE }
        val serverField = createInput("/path/to/server/start.sh").apply { text = ConfigManager.config.serverStartPath; prefWidth = 400.0 }

        val autoLaunchCheck = CheckBox("Автозапуск игры").apply {
            isSelected = ConfigManager.config.autoLaunch
            textFill = Color.WHITE
        }
        val autoStartServerCheck = CheckBox("Автозапуск локального сервера").apply {
            isSelected = ConfigManager.config.autoStartServer
            textFill = Color.WHITE
        }
        val autoUiTestsCheck = CheckBox("Авто-UI тесты").apply {
            isSelected = ConfigManager.config.autoUiTests
            textFill = Color.WHITE
        }
        
        val saveBtn = createButton("Сохранить", true).apply {
            prefWidth = 150.0
            setOnAction {
                ConfigManager.update { copy(
                    javaPath = javaField.text, 
                    maxRam = ramSlider.value.toInt(), 
                    serverStartPath = serverField.text,
                    serverAddress = ipField.text,
                    autoLaunch = autoLaunchCheck.isSelected,
                    autoStartServer = autoStartServerCheck.isSelected,
                    autoUiTests = autoUiTestsCheck.isSelected
                ) }
                text = "Сохранено ✓"
                scope.launch { delay(1500); Platform.runLater { text = "Сохранить" } }
                loadData()
            }
        }
        
        card.children.addAll(
            javaLabel, javaField, Region().apply { prefHeight = 8.0 },
            ipLabel, ipField, Region().apply { prefHeight = 8.0 },
            ramLabel, ramSlider, Region().apply { prefHeight = 12.0 },
            serverLabel, serverField, Region().apply { prefHeight = 16.0 },
            autoLaunchCheck,
            autoStartServerCheck,
            autoUiTestsCheck,
            Region().apply { prefHeight = 8.0 },
            saveBtn
        )
        
        val accCard = VBox(16.0).apply {
            padding = Insets(24.0)
            style = "-fx-background-color: $CARD; -fx-background-radius: 12; -fx-border-color: $BORDER; -fx-border-radius: 12;"
        }
        
        accCard.children.addAll(
            Label("Аккаунт").apply { font = Font.font("System", FontWeight.BOLD, 16.0); textFill = Color.WHITE },
            Label("Вы вошли как: ${currentUser?.login}").apply { font = Font.font(14.0); textFill = Color.web(TEXT_SEC) },
            createButton("Выйти из аккаунта", false).apply {
                prefWidth = 180.0
                setOnAction { logout() }
            }
        )
        
        content.children.addAll(header, card, accCard)
        contentArea.children.setAll(content)
    }

    private fun maybeAutoLaunch(status: Label, progress: ProgressBar, btn: Button) {
        if (autoLaunchTriggered) return
        if (!ConfigManager.config.autoLaunch) return
        if (currentUser == null) return
        if (selectedServer == null) return
        autoLaunchTriggered = true
        val delayMs = ConfigManager.config.autoStartServerDelayMs.coerceAtLeast(0)
        if (ConfigManager.config.autoStartServer) {
            val started = startLocalServerAuto(status)
            scope.launch {
                if (started) delay(delayMs)
                Platform.runLater { launchGame(status, progress, btn) }
            }
        } else {
            launchGame(status, progress, btn)
        }
    }
    
    private fun createNavItem(text: String, tab: String): HBox = HBox().apply {
        padding = Insets(10.0, 12.0, 10.0, 12.0)
        alignment = Pos.CENTER_LEFT
        cursor = Cursor.HAND
        style = "-fx-background-radius: 8;"
        children.add(Label(text).apply { font = Font.font(14.0); textFill = Color.web(TEXT_SEC) })
        setOnMouseClicked {
            when (tab) {
                "servers" -> showServersTab()
                "news" -> showNewsTab()
                "settings" -> showSettingsTab()
            }
        }
        setOnMouseEntered { if (currentTab != tab) style = "-fx-background-color: $CARD; -fx-background-radius: 8;" }
        setOnMouseExited { if (currentTab != tab) style = "-fx-background-radius: 8;" }
    }
    
    private fun updateNavSelection() {
        navItems.forEachIndexed { i, item ->
            val tab = listOf("servers", "news", "settings")[i]
            val active = tab == currentTab
            item.style = if (active) "-fx-background-color: $CARD; -fx-background-radius: 8;" else "-fx-background-radius: 8;"
            (item.children[0] as Label).apply {
                textFill = Color.web(if (active) TEXT else TEXT_SEC)
                font = Font.font("System", if (active) FontWeight.MEDIUM else FontWeight.NORMAL, 14.0)
            }
        }
    }
    
    private fun createInput(prompt: String): TextField = TextField().apply {
        promptText = prompt
        prefHeight = 44.0
        font = Font.font(14.0)
        style = "-fx-background-color: $CARD; -fx-text-fill: white; -fx-prompt-text-fill: #666; -fx-background-radius: 8; -fx-border-color: $BORDER; -fx-border-radius: 8; -fx-padding: 10 14;"
    }
    
    private fun createPasswordInput(prompt: String): PasswordField = PasswordField().apply {
        promptText = prompt
        prefHeight = 44.0
        font = Font.font(14.0)
        style = "-fx-background-color: $CARD; -fx-text-fill: white; -fx-prompt-text-fill: #666; -fx-background-radius: 8; -fx-border-color: $BORDER; -fx-border-radius: 8; -fx-padding: 10 14;"
    }
    
    private fun createButton(text: String, primary: Boolean): Button = Button(text).apply {
        prefWidth = 320.0
        prefHeight = 44.0
        font = Font.font("System", FontWeight.MEDIUM, 14.0)
        cursor = Cursor.HAND
        val baseStyle = if (primary) "-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 8;"
            else "-fx-background-color: transparent; -fx-text-fill: #888; -fx-border-color: $BORDER; -fx-border-radius: 8; -fx-background-radius: 8;"
        style = baseStyle
        setOnMouseEntered { style = if (primary) "-fx-background-color: #E0E0E0; -fx-text-fill: black; -fx-background-radius: 8;" else "-fx-background-color: $CARD; -fx-text-fill: white; -fx-border-color: $BORDER_LIGHT; -fx-border-radius: 8; -fx-background-radius: 8;" }
        setOnMouseExited { style = baseStyle }
    }
    
    private fun createUserBox(): HBox = HBox(10.0).apply {
        padding = Insets(12.0)
        alignment = Pos.CENTER_LEFT
        style = "-fx-background-color: $CARD; -fx-background-radius: 8;"
        children.addAll(
            VBox(2.0).apply {
                HBox.setHgrow(this, Priority.ALWAYS)
                children.addAll(
                    Label(currentUser?.login ?: "").apply { font = Font.font("System", FontWeight.MEDIUM, 13.0); textFill = Color.WHITE },
                    Label("В сети").apply { font = Font.font(11.0); textFill = Color.web(SUCCESS) }
                )
            },
            Label("↩").apply {
                font = Font.font(14.0); textFill = Color.web(TEXT_SEC); cursor = Cursor.HAND
                setOnMouseClicked { logout() }
                setOnMouseEntered { textFill = Color.WHITE }
                setOnMouseExited { textFill = Color.web(TEXT_SEC) }
            }
        )
    }
    
    private fun createServerCard(server: Server, container: VBox): HBox = HBox(14.0).apply {
        padding = Insets(16.0)
        alignment = Pos.CENTER_LEFT
        cursor = Cursor.HAND
        style = "-fx-background-color: $CARD; -fx-background-radius: 10; -fx-border-color: $BORDER; -fx-border-radius: 10;"
        children.addAll(
            Label(if (server.online) "●" else "○").apply { font = Font.font(10.0); textFill = Color.web(if (server.online) SUCCESS else TEXT_DIM) },
            VBox(4.0).apply {
                HBox.setHgrow(this, Priority.ALWAYS)
                children.addAll(
                    Label(server.name).apply { font = Font.font("System", FontWeight.MEDIUM, 15.0); textFill = Color.WHITE },
                    Label("${server.players} / ${server.maxPlayers} игроков").apply { font = Font.font(12.0); textFill = Color.web(TEXT_SEC) }
                )
            },
            Label("→").apply { font = Font.font(16.0); textFill = Color.web(TEXT_DIM) }
        )
        setOnMouseClicked { selectedServer = server; showServersTab() }
        setOnMouseEntered { style = "-fx-background-color: $CARD_HOVER; -fx-background-radius: 10; -fx-border-color: $BORDER_LIGHT; -fx-border-radius: 10;" }
        setOnMouseExited { style = "-fx-background-color: $CARD; -fx-background-radius: 10; -fx-border-color: $BORDER; -fx-border-radius: 10;" }
    }
    
    private fun loadData() {
        scope.launch {
            api.getServers()
                .onSuccess { 
                    servers = it
                    if (selectedServer == null && servers.isNotEmpty()) {
                        selectedServer = servers.first()
                    }
                    Platform.runLater { if (currentTab == "servers") showServersTab() } 
                }
                .onFailure {
                    // Fallback: Default to the local network server
                    // Это "Нормальный вариант": сервер прописан по умолчанию
                    val cfg = ConfigManager.config
                    servers = listOf(
                        Server("1", "AXIOM Server (Local)", cfg.serverAddress, cfg.serverPort, "default", true)
                    )
                    selectedServer = servers.first()
                    Platform.runLater { if (currentTab == "servers") showServersTab() }
                }
            api.getNews().onSuccess { newsList = it }
        }
    }
    
    private fun launchGame(status: Label, progress: ProgressBar, btn: Button) {
        val server = selectedServer ?: return
        btn.isDisable = true
        status.text = "Подготовка..."
        progress.progress = 0.0
        progress.isVisible = true
        
        scope.launch {
            val modpack = api.getModpack(server.modpack).getOrNull()
            
            launcher.launch(
                server = server,
                username = currentUser?.login ?: "Player",
                token = currentUser?.token ?: "",
                modpack = modpack
            ) { msg, p ->
                Platform.runLater {
                    status.text = msg
                    if (p >= 0f) {
                        progress.progress = p.toDouble()
                    }
                }
            }.onSuccess {
                Platform.runLater {
                    status.text = "Игра запущена! Подключение к ${server.address}..."
                    btn.isDisable = false
                    progress.progress = 1.0
                }
            }.onFailure { e ->
                Platform.runLater {
                    status.text = "Ошибка: ${e.message?.take(80)}"
                    btn.isDisable = false
                    progress.isVisible = false
                }
            }
        }
    }

    private fun startLocalServer(status: Label, btn: Button) {
        val existing = serverProcess
        if (existing != null && existing.isAlive) {
            status.text = "Сервер уже запущен."
            return
        }

        val script = resolveServerStartScript()
        if (script == null) {
            status.text = "Скрипт запуска сервера не найден."
            return
        }

        btn.isDisable = true
        status.text = "Запуск сервера..."
        scope.launch {
            val process = withContext(Dispatchers.IO) {
                val builder = ProcessBuilder("bash", script.absolutePath)
                    .directory(script.parentFile)
                    .redirectErrorStream(true)
                com.axiom.launcher.ServerStartEnv.apply(builder)
                builder.start()
            }
            serverProcess = process
            Platform.runLater { status.text = "Сервер запускается..." }

            val code = withContext(Dispatchers.IO) { process.waitFor() }
            Platform.runLater {
                status.text = "Сервер остановлен (код $code)"
                btn.isDisable = false
            }
        }
    }

    private fun startLocalServerAuto(status: Label): Boolean {
        val existing = serverProcess
        if (existing != null && existing.isAlive) {
            status.text = "Сервер уже запущен."
            return false
        }

        val script = resolveServerStartScript()
        if (script == null) {
            status.text = "Скрипт запуска сервера не найден."
            return false
        }

        status.text = "Запуск сервера..."
        scope.launch {
            val process = withContext(Dispatchers.IO) {
                val builder = ProcessBuilder("bash", script.absolutePath)
                    .directory(script.parentFile)
                    .redirectErrorStream(true)
                com.axiom.launcher.ServerStartEnv.apply(builder)
                builder.start()
            }
            serverProcess = process
            Platform.runLater { status.text = "Сервер запускается..." }

            scope.launch {
                val code = withContext(Dispatchers.IO) { process.waitFor() }
                Platform.runLater { status.text = "Сервер остановлен (код $code)" }
            }
        }
        return true
    }

    private fun resolveServerStartScript(): File? {
        val configPath = ConfigManager.config.serverStartPath.trim()
        val envPath = System.getenv("AXIOM_SERVER_START")?.trim().orEmpty()
        val cwd = File(System.getProperty("user.dir"))
        val candidates = listOfNotNull(
            if (configPath.isNotBlank()) File(configPath) else null,
            if (envPath.isNotBlank()) File(envPath) else null,
            File(cwd, "../server/start.sh"),
            File(cwd, "server/start.sh"),
            File(System.getProperty("user.home"), "axiom plugin/server/start.sh")
        )
        return candidates.firstOrNull { it.exists() && it.isFile }
    }
    
    private fun doLogin(login: String, pass: String, err: Label, btn: Button) {
        if (login.isBlank() || pass.isBlank()) { err.text = "Заполните все поля"; return }
        btn.isDisable = true; btn.text = "Вход..."
        scope.launch {
            api.login(login, pass).onSuccess { r -> Platform.runLater {
                if (r.success && r.token != null) { api.setToken(r.token); currentUser = r.user; ConfigManager.update { copy(token = r.token, lastUser = r.user?.login) }; showMainView() }
                else { err.text = r.message; btn.text = "Войти"; btn.isDisable = false }
            }}.onFailure { Platform.runLater { err.text = "Сервер недоступен"; btn.text = "Войти"; btn.isDisable = false } }
        }
    }
    
    private fun doRegister(login: String, pass: String, err: Label, btn: Button) {
        if (login.length < 3 || pass.length < 8) { err.text = "Логин 3+, пароль 8+ символов"; return }
        btn.isDisable = true; btn.text = "Регистрация..."
        scope.launch {
            api.register(login, pass).onSuccess { r -> Platform.runLater {
                if (r.success && r.token != null) { api.setToken(r.token); currentUser = r.user; ConfigManager.update { copy(token = r.token, lastUser = r.user?.login) }; showMainView() }
                else { err.text = r.message; btn.text = "Создать аккаунт"; btn.isDisable = false }
            }}.onFailure { Platform.runLater { err.text = "Ошибка"; btn.text = "Создать аккаунт"; btn.isDisable = false } }
        }
    }

    private fun doOfflineLogin(login: String, err: Label) {
        if (login.isBlank()) { err.text = "Введите логин"; return }
        val offlineUser = User(0, login, "OFFLINE")
        currentUser = offlineUser
        ConfigManager.update { copy(token = "OFFLINE", lastUser = login) }
        showMainView()
    }
    
    private fun logout() { ConfigManager.update { copy(token = null, lastUser = null) }; api.setToken(null); currentUser = null; selectedServer = null; showLoginView() }
    override fun stop() { scope.cancel(); api.close() }
}
