// Two modules:
//   :core   — the pure st/1 parser + model, plain Java, unit-tested with plain JUnit
//   :plugin — the IntelliJ tool window, built against the IntelliJ Platform
// Keeping the parser out of the plugin module lets it be tested without the platform.
allprojects {
    group = "io.github.gabrielbbaldez"
    version = "0.1.0"
}
