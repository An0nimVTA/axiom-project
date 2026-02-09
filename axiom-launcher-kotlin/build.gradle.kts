plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("edu.sc.seis.launch4j") version "3.0.5"
}

group = "com.axiom"
version = "1.0.0"

repositories {
    mavenCentral()
}

launch4j {
    mainClassName.set("com.axiom.launcher.MainKt")
    outfile.set("AxiomLauncher.exe")
    headerType.set("gui") // <--- Changed from "console" to "gui"
    bundledJrePath.set("runtime") 
    jreMinVersion.set("17")
    errTitle.set("Axiom Launcher Error")
    downloadUrl.set("https://adoptium.net/temurin/releases/")
    supportUrl.set("https://github.com/An0nimVTA/axiom-project")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web")
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // HTTP Client
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    
    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Cross-platform JavaFX
    val javafxVersion = "21"
    val javafxModules = listOf("base", "controls", "fxml", "graphics", "media", "swing", "web")
    
    for (module in javafxModules) {
        implementation("org.openjfx:javafx-$module:$javafxVersion:linux")
        implementation("org.openjfx:javafx-$module:$javafxVersion:win")
        implementation("org.openjfx:javafx-$module:$javafxVersion:mac")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

application {
    mainClass.set("com.axiom.launcher.MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.axiom.launcher.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
