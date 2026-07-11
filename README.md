# stacktale-intellij

IntelliJ IDEA / JetBrains plugin for [**stacktale**](https://github.com/GabrielBBaldez/stacktale) —
the Java logging library that turns errors into AI-ready reports (`errors-ai.log`).

The report already knows the root cause and the exact culprit frame; this plugin makes that
navigable inside the IDE:

- a **Stacktale** tool window listing reports, newest first, root cause up front;
- **double-click** (or *Jump to Culprit*) opens the editor at the exact `File.java:line`;
- **Copy Report for AI** puts the whole `st/1` block on the clipboard for your assistant;
- it re-reads `errors-ai.log` on a light poll, so new errors appear on their own.

It only reads the report file — no agent, no network, nothing written back.

## Build

The parser is pure Java and unit-tested; the rest builds against the IntelliJ Platform.

```bash
# build against an installed IDE (no ~1 GB SDK download)
./gradlew buildPlugin -PlocalIdePath="C:/Program Files/JetBrains/IntelliJ IDEA 2024.3.5"

# or let Gradle download IntelliJ IDEA Community
./gradlew buildPlugin
```

The installable zip lands in `build/distributions/`. In the IDE:
*Settings → Plugins → ⚙ → Install Plugin from Disk…* and pick that zip.

Run the parser tests with `./gradlew test`.

## Status

v0.1.0 — the navigation loop (list → jump → copy). Compatible with IDEs build 242–261
(2024.2 through 2025.x). Ideas for next: gutter markers on lines that recently threw,
a "send to the IDE's AI" action, and a status-bar count.
