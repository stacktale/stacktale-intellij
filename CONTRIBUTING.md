# Contributing to stacktale-intellij

Thanks for stopping by the campfire. 🔥

This is the IntelliJ IDEA / JetBrains plugin for
[**stacktale**](https://github.com/stacktale/stacktale). The library lives in the main repo;
this repo is just the plugin that surfaces its `errors-ai.log` inside the IDE.

New here? The [`good first issue`](https://github.com/stacktale/stacktale-intellij/labels/good%20first%20issue)
label marks issues written to be picked up cold — each one names the files to touch and how
to verify.

## Claim the issue before you start

**Comment on the issue saying you'd like to take it, and wait for a reply before writing
code.** A short "I'd like to work on this" is enough — no need to restate the issue or
explain your plan. It lets us tell you upfront if an issue is already half-done or narrower
than it looks. If nobody replies in a day or two, open the PR anyway. Exceptions where you
should just open the PR: an obvious typo, and anything already assigned to you.

## Build & test

The project is two Gradle modules: `:core` (a pure-Java parser, unit-tested in plain JVM)
and `:plugin` (everything that touches the IntelliJ Platform).

```bash
# fast: build against an installed IDE (no ~1 GB SDK download)
./gradlew buildPlugin -PlocalIdePath="C:/Program Files/JetBrains/IntelliJ IDEA 2024.3.5"

# CI / no local IDE: omit the property and Gradle downloads IntelliJ IDEA Community
./gradlew buildPlugin

./gradlew test          # runs the :core parser tests
./gradlew runIde        # launches a sandbox IDE with the plugin loaded
```

The installable zip lands in `build/distributions/`.

## Where the pieces live

- **`core/src/main/java/.../StReportParser.java`** — the `errors-ai.log` (`st/1`) parser.
  No IntelliJ API, so it's unit-testable without the platform. This mirrors the parser in
  the [VS Code extension](https://github.com/stacktale/stacktale-vscode); keep them in step.
- **`plugin/`** — the tool window, actions, and file poll.
- **`core/src/test/java/.../StReportParserTest.java`** — the parser tests.

Prefer putting logic in `:core` (fast, testable) and keeping `:plugin` thin.

## The report format is a public API

The `errors-ai.log` format (`st/1`, and the opt-in `st-json/1`) is specified in the main
repo's [docs/FORMAT.md](https://github.com/stacktale/stacktale/blob/main/docs/FORMAT.md).
Parse defensively — a malformed or partially-written line must never crash the tool window —
and when in doubt about a field, check FORMAT.md rather than guessing.

## Working style

- New parsing behavior arrives with a `:core` test that demanded it.
- Commits: conventional prefixes (`feat:`, `fix:`, `docs:`, `chore:`, `test:`), imperative
  mood, reference issues (`Closes #N`).
- One logical change per PR; explain the *why*, link the issue.

By taking part you agree to the main repo's
[Code of Conduct](https://github.com/stacktale/stacktale/blob/main/CODE_OF_CONDUCT.md).
