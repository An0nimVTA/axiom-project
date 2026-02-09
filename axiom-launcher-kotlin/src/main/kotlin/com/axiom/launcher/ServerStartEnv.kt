package com.axiom.launcher

object ServerStartEnv {
    private const val SAFE_PARALLELISM = "-Djava.util.concurrent.ForkJoinPool.common.parallelism=1"

    fun apply(processBuilder: ProcessBuilder) {
        val env = processBuilder.environment()
        val enabled = env["AXIOM_MOHIST_SAFE_START"]?.trim()?.lowercase()
            ?.let { it == "1" || it == "true" || it == "yes" }
            ?: true
        if (!enabled) return

        val existing = env["JAVA_TOOL_OPTIONS"]?.trim().orEmpty()
        if (existing.contains(SAFE_PARALLELISM)) return
        env["JAVA_TOOL_OPTIONS"] = if (existing.isBlank()) {
            SAFE_PARALLELISM
        } else {
            "$existing $SAFE_PARALLELISM"
        }
    }
}
