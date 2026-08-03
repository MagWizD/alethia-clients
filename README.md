# alethia-clients

Client-side detection for the Alethia platform. This repository contains the IDE integrations responsible for passively monitoring developer activity, detecting suspicious code insertions, and serializing flagged metadata as git notes on each commit. The notes are then pushed to remote where the Themis reporting engine reads them on incoming pull requests.

Alethia clients are designed to be passive - they require no action from the developer and do not block or modify any part of the normal development workflow.

---

## Repository Structure

```
Alethia-clients/
├── _vscode/          # VSCode extension (JavaScript / Node.js)
└── _jetbrains/       # JetBrains plugin (Kotlin / Gradle)
```

Each client is an independent project with its own build system, dependencies, and deployment target. They share the same detection logic conceptually but are implemented separately for each platform. THis repository will split once both extensions are stabe enough to version independently, for a small team a single repo enables us to more easily managed the currently evolving schema

---

## How It Works

1. The client activates when the developer opens a repository in their IDE
2. It listens passively for suspicious code insertions using the IDE's text change API
3. When a suspicious insertion is detected a `FlaggedRegion` is created and queued in session storage
4. On commit, all queued flags are serialized to JSON and written as a git note on the commit SHA
5. On push, the git notes are pushed to remote alongside the commits
6. The Themis reporting engine reads the notes from the remote and generates a report on the pull request

Alethia clients communicate with Themis exclusively through git notes - there is no direct connection between the two systems.

---

## Clients

### VSCode Extension (`/vscode`)

Built with the VSCode Extension API and Node.js.

| | |
|---|---|
| Language | JavaScript |
| Runtime | Node.js |
| API | VSCode Extension API |
| Detection | `onDidChangeTextDocument` + clipboard API |
| Commit hook | `repo.state.onDidChange` via `vscode.git` |
| Push hook | `onPush()` + `.git/hooks/pre-push` |

### JetBrains Plugin (`/jetbrains`)

Built with the IntelliJ Platform SDK and Kotlin. A single plugin that targets all JetBrains IDEs that include bundled Git support - IntelliJ IDEA, PyCharm, WebStorm, GoLand, CLion, Rider, RubyMine, PhpStorm, and RustRover.

| | |
|---|---|
| Language | Kotlin |
| Build | Gradle |
| API | IntelliJ Platform SDK / git4idea |
| Detection | `DocumentListener` + `CopyPastePreProcessor` |
| Commit hook | `GitCheckinHandlerFactory` |
| Push hook | `GitPushListener` |

---

## What Gets Collected

Both clients collect the same categories of metadata and write them as a structured JSON git note on each commit:

```json
{
    "alethia": {
        "version": "0.1.0",
        "generatedAt": "2026-08-03T00:00:00Z",
        "flagCount": 2,              <- //INT
        "flaggedRegions": [          <- //ARRAY
            {
                "file": "src/auth.kt",
                "startLine": 10,     <- //INT
                "endLine": 20,       <- //INT
                "charCount": 500,    <- //LONG 
                "rationale": "Large clipboard paste - 500 chars.",
                "timeStamp": "2026-08-03T00:00:00Z"
            },
            {...}
        ]
    }
}
```

This simple structure has multiple benefits:
- A commit with no `alethia` object means that `Alethia` was not active -> `Themis` can flag this
- A commit with an `alethia` object but no flags means `Alethia` was active but found nothing suspicious
- A commit with a non-zero `flagCount` and populated `flaggedRegions` means `Alethia` created flags
  - Additional note: `Themis` should flag if `array size` and `flagCount` do not match -> could indicate a transfer error or tampering

---
 
## Roadmap
 
### VSCode Extension
 
- [ ] Define and lock `FlaggedRegion` schema
- [ ] Define and lock git note JSON schema
- [ ] Implement passive text change detection
- [ ] Implement clipboard comparison to distinguish paste from inline AI completion
- [ ] Implement session persistence - flags survive VSCode restarts
- [ ] Implement file signing for tamper detection on saved metadata
- [ ] Implement commit detection
- [ ] Implement git note writing on commit
- [ ] Implement push detection
- [ ] Implement git notes push to remote on push
- [ ] Implement pre-push hook installer
- [ ] Write `extensionActive: true` on every commit regardless of flag count
- [ ] Implement `.alethia.yml` config file reader
- [ ] Apply maintainer config to detection thresholds
- [ ] Design chat overlay sidebar panel (Is it needed? Re-evaulate once we reach this point.)
- [ ] Implement chat overlay sidebar panel (Conditional - Depends on previous milestone)
- [ ] Remove all debug-only code and notifications
- [ ] Test on Windows, Mac, and Linux

---
 
### JetBrains Plugin
 
- [X] Configure plugin metadata - `plugin.xml`, `gradle.properties`
- [X] Define package structure under `com.alethia`
- [X] Define `FlaggedRegion` data class matching VSCode schema
- [X] Implement passive text change detection
- [X] Implement debounce to prevent duplicate flags from large change events
- [X] Implement paste interception
- [ ] Implement session persistence - flags survive IDE restarts
- [ ] Implement file signing for tamper detection on saved metadata
- [ ] Implement commit detection
- [ ] Implement git note writing on commit
- [ ] Implement push detection
- [ ] Implement git notes push to remote on push
- [ ] Write `extensionActive: true` on every commit regardless of flag count
- [ ] Implement `.alethia.yml` config file reader
- [ ] Apply maintainer config to detection thresholds
- [ ] Test across IntelliJ IDEA, PyCharm, WebStorm, GoLand, Rider

---

## JetBrains Plugin Structure

### File Structure

```
_jetbrains/src/main
├── resources/META-INF
│   └── plugin.xml
└── kotlin/com/alethia
    ├── config/
    │   └── DetectionConfig.kt
    ├── detection/
    │   ├── AlethiaEventHandler.kt
    │   ├── events/
    │   │   ├── DetectionEvent.kt
    │   │   └── EventSource.kt
    │   ├── listeners/
    │   │   ├── AlethiaDocumentListener.kt
    │   │   └── AlethiaPasteProcessor.kt
    │   └── rules/
    │       ├── DetectionRule.kt
    │       ├── LargePasteRule.kt
    │       └── RuleEngine.kt
    ├── model/
    │   ├── FlaggedRegion.kt
    │   └── SerializableFlaggedRegion.kt
    └── session/
        ├── SessionState.kt
        └── AlethiaStateService.kt

```

---

### Project Organization

**plugin.xml**
Configuration file read by the IntelliJ Platform at startup. Declares the plugin identity, dependencies, registered services, listeners, and extension points. IntelliJ will not know any of the plugin's components exist without being declared here.

**DetectionConfig.kt : Data Class**
Data class holding all configurable thresholds and rules used by the detection system. Uses constructor parameters with default values so it works out of the box. Passed into rules via constructor injection so rules never hardcode thresholds directly.

**AlethiaEventHandler.kt : Class**
Single entry point for all detection events. Receives `SessionState` via constructor injection. Responsible for two things: deduplicating events by source priority, and delegates to `RuleEngine` for rule evaluation.

**DetectionEvent.kt : Data Class**
Data class that wraps all raw information from any listener into a structured object. Listeners build one of these and hand it to `AlethiaEventHandler`, they make no decisions themselves.

**EventSource.kt : Enum Class**
An enum defining the possible sources of a detection event. Each value carries a priority integer so the event handler can determine which source is more specific when deduplication is needed. `CLIPBOARD_PASTE` has higher priority than `DOCUMENT_CHANGE` because a confirmed paste is more specific than an inferred insertion.

**DetectionRule.kt : Interface**
The contract that all detection rules must implement. Defines a single method `evaluate(event: DetectionEvent): String?` that returns a rationale string if the event should be flagged or null if it should be ignored. Adding a new detection rule means creating a new class that implements this interface and registering it in `RuleEngine`, no other files need to change.

**LargePasteRule.kt : Class**
The first concrete implementation of `DetectionRule`. Evaluates if an insertion exceeds the configured character threshold and whether the file should be ignored. Returns different rationale strings depending on the event source.

**RuleEngine.kt : Object**
Singleton that holds a list of all registered `DetectionRule` implementations and evaluates them against incoming events. Returns the rationale from the first rule that fires or null if no rules match. New rules are added by instantiating them in the rules list.

**FlaggedRegion.kt : Data Class**
The core model that represents a single region of code that has been flagged as suspicious. Immutable, all fields are `val`. A data class because it is pure data, auto-generated `equals()` and `hashCode()` are needed for test assertions, and `toString()` is useful for logging and debugging.

**SerializableFlaggedRegion.kt : Class**
An XML-serializable wrapper for `FlaggedRegion` used exclusively inside `AlethiaStateService`. Cannot be a data class because IntelliJ's XML serializer requires mutable fields with no-arg defaults. Contains `companion object` with `from()` factory method to convert from `FlaggedRegion`, and a `toFlaggedRegion()` method to convert back.

**SessionState.kt : Interface**
Contract for Alethia session state. Defines the methods and properties any session state implementation must provide, `addFlag`, `getFlags`, `flagCount`, `clearFlags`, and `lastCommitSha`. `AlethiaEventHandler` and any other component that needs session state is coupled only to this interface. This makes the persistence mechanism swappable and allows tests to inject a simple in-memory mock.

**AlethiaStateService.kt : Class**
Implementation of `SessionState` and the single source of truth for all session persistence. A project-scoped IntelliJ Platform service managed by the IntelliJ container, never instantiated directly, always accessed via `project.service<AlethiaStateService>()`. Project-scoped to prevent state conflicts between multiple open repositories.

---

## Contributing

Setup instructions coming soon...working out the process

---

## Setup

Coming later!

---

## License

GNU GENERAL PUBLIC LICENSE Version 3
