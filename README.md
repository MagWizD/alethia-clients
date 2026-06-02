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
  "AlethiaVersion": "0.1.0",
  "commit": "<sha>",
  "generatedAt": "<iso timestamp>",
  "extensionActive": true,
  "flagCount": 2,
  "flaggedRegions": [
    {
      "file": "src/auth.js",
      "startLine": 42,
      "endLine": 67,
      "charCount": 1204,
      "reasonFlagged": "Large instant insertion - 1204 chars in 80ms",
      "timeStamp": "<iso timestamp>"
    }
  ],
  "chatHistory": []
}
```

A commit with `extensionActive: true` and `flagCount: 0` means the extension was running and found nothing suspicious. A commit with no note at all means the extension was not active - Themis treats this as a signal in itself.

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
- [ ] Define package structure under `com.alethia`
- [ ] Define `FlaggedRegion` data class matching VSCode schema
- [ ] Implement passive text change detection
- [ ] Implement paste interception
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

## Contributing

Setup instructions coming soon...working out the process

---

## Setup

Coming later this week!

---

## License

GNU GENERAL PUBLIC LICENSE Version 3
