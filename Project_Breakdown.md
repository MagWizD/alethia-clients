# alethia-clients

Alethia clients require no action from the contributor and do not block or modify any part of the normal development workflow.

---

## Class Descriptions

### Config

**DetectionConfig.kt  :  Data Class**
Defines the thresholds and patterns that determine what Alethia considers suspicious. Maintainers will eventually be able to tune these values per-repo via `.alethia.yml`.

Centralizing these values means rules never need to be changed to adjust sensitivity.

**AlethiaConstants.kt  :  Object**
A single source of truth for string values used across multiple files; git ref names, hook markers, config keys, and the plugin version. 

Prevents the same string from being duplicated across files and becoming inconsistent over time.

---

### Detection

**AlethiaEventHandler.kt  :  Class**
The central coordinator for all detection activity. Every event from every listener flows through here before any flag is created. 

Having a single entry point means deduplication, rule evaluation, and flag creation logic lives in one place, adding a new listener in the future requires no changes here.

**DetectionRule.kt  :  Interface**
Defines the contract every detection rule must satisfy. The interface exists so new rules can be added without touching any existing code; implement the interface, register in `RuleEngine`, done. 

This keeps detection logic modular and independently testable.

**LargePasteRule.kt  :  Class**
The first detection rule, flags insertions above a configurable character threshold. 

Distinguishes between confirmed clipboard pastes and inferred document changes, reporting each with an appropriate confidence level so Themis can weight them differently.

**RuleEngine.kt  :  Object**
Runs all registered rules against each incoming event and returns the first match. 

Centralizing rule orchestration here means `AlethiaEventHandler` never needs to know which rules exist or how many there are.

**AlethiaDocumentListener.kt  :  Class**
Listens for all text changes in open documents. Exists as a thin adapter because IntelliJ requires a specific interface to hook into document events.

Keeping it thin means all actual decision-making stays in `AlethiaEventHandler` where it belongs.

**AlethiaPasteListener.kt  :  Class**
Intercepts clipboard paste events before the text lands in the editor. 

A paste confirmed by this listener carries higher confidence than an insertion detected by the document listener alone, because there is no ambiguity about where the text came from.

---

### Events

**DetectionEvent.kt  :  Data Class**
A structured container that carries everything needed to evaluate a detection event; file path, repo root, character count, line numbers, elapsed time, and source. 

Listeners build one of these and hand it off, keeping their own logic to a minimum.

**EventSource.kt  :  Enum Class**
Each source carries a priority so the event handler can resolve conflicts when multiple listeners fire for the same insertion, a confirmed paste always takes precedence over an inferred document change.

---

### Model

**FlaggedRegion.kt  :  Data Class**
Represents a single suspicious code insertion that Alethia has decided to record. 

This is the core unit of data that flows from detection through to the git note, everything Themis needs to evaluate a flagged insertion is here.

**RuleResult.kt  :  Data Class**
Carries the output of a rule evaluation: a standardized `eventType` identifier and a human-readable rationale. 

The `eventType` exists specifically so Themis can categorize flags programmatically without parsing text, making the Themis implementation simpler and more reliable.

---

### Session

**AlethiaStateService.kt  :  Class**
Keeps track of everything Alethia has observed during the current coding session, flagged regions and the SHA of the last processed commit. 

Persisting this state means flags are never lost if the IDE closes unexpectedly before a commit, and the last commit SHA prevents duplicate git notes from being written if the handler fires more than once.

---

### Startup

**AlethiaInstaller.kt  :  Object**
Sets up everything Alethia needs in a repository the first time a developer opens it. The pre-push hook ensures notes reach the remote alongside normal pushes. The git config entries ensure notes survive rebases and amends. 

Without these, rewriting commit history would silently orphan the notes Themis depends on.

**AlethiaStartupActivity.kt  :  Class**
The entry point IntelliJ calls when a project opens. Its only job is to wait until git repositories are fully mapped, which does not happen immediately on project open, and then hand off to `AlethiaInstaller`. 

Separating the IntelliJ entry point from the installation logic makes the installer independently testable.

---

### VCS

**AlethiaCheckinHandlerFactory.kt  :  Classes**
Contains two classes. `AlethiaCheckinHandlerFactory` hooks into IntelliJ's commit pipeline so Alethia is notified every time a commit succeeds. `AlethiaCheckinHandler` does the work on each commit, writing the accumulated flags as a git note, saving a snapshot of the session state before clearing it, and recording the commit SHA to prevent duplicate processing. Git notes are the transport mechanism that gets Alethia's data to Themis without requiring any external server.

---

### Build and Config

**plugin.xml**
Tells IntelliJ what this plugin is and what it registers. Without this file IntelliJ would have no knowledge of any of the plugin's services, listeners, or extension points.

**logging.properties**
Configures where and how Alethia writes its logs. Keeping configuration out of code means log output location and format can be adjusted without recompiling the plugin. Alethia logs to a dedicated `~/.alethia/alethia.log` file rather than IntelliJ's `idea.log` so that plugin-specific output is easy to find without searching through IDE internals.
