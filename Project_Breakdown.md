# alethia-clients

Client-side detection for the Alethia platform. This repository contains the IDE integrations responsible for passively monitoring developer activity, detecting suspicious code insertions, and serializing flagged metadata as git notes on each commit. The notes are then pushed to remote where the Themis reporting engine reads them on incoming pull requests.

Alethia clients are designed to be passive - they require no action from the developer and do not block or modify any part of the normal development workflow.

# Class Descriptions

## Config

### DetectionConfig.kt — Data Class

Data class holding all configurable thresholds and rules used by the detection system. Uses constructor parameters with default values so it works out of the box. Passed into rules via constructor injection so rules never hardcode thresholds directly.


## Detection

### AlethiaEventHandler.kt — Class
Single entry point for all detection events. Receives `SessionState` via constructor injection. Responsible for two things: deduplicating events by source priority, and delegates to `RuleEngine` for rule evaluation.

### DetectionRule.kt — Interface
The contract that all detection rules must implement. Defines a single method `evaluate(event: DetectionEvent): String?` that returns a rationale string if the event should be flagged or null if it should be ignored. Adding a new detection rule means creating a new class that implements this interface and registering it in `RuleEngine`, no other files need to change.

### LargePasteRule.kt — Class
The first concrete implementation of `DetectionRule`. Evaluates if an insertion exceeds the configured character threshold and whether the file should be ignored. Returns different rationale strings depending on the event source.

### RuleEngine.kt — Object
Singleton that holds a list of all registered `DetectionRule` implementations and evaluates them against incoming events. Returns the rationale from the first rule that fires or null if no rules match. New rules are added by instantiating them in the rules list.

### AlethiaDocumentListener.kt — Class
A thin adapter, implemented as a class, that is responsible for detecting and changes to open documents within the project. When an event is received, the class sends a structured `DetectionEvent` to `AlethiEventHandler` for further processing and flag evaluation. Implements `DocumentListener`.

### AlethiaPasteListener.kt — Class
A thin adapter, implemented as a class, that is responsible for detecting and clipboard pasts within the project. When an event is received, the class sends a structured `DetectionEvent` to `AlethiEventHandler` for further processing and flag evaluation. Implements `CopyPastePreprocessor`.


## Model

### DetectionEvent.kt — Data Class
Data class that wraps all raw information from any listener into a structured object. Listeners build one of these and hand it to `AlethiaEventHandler`, they make no decisions themselves.

### EventSource.kt — Enum Class
An enum defining the possible sources of a detection event. Each value carries a priority integer so the event handler can determine which source is more specific when deduplication is needed. `CLIPBOARD_PASTE` has higher priority than `DOCUMENT_CHANGE` because a confirmed paste is more specific than an inferred insertion.

### FlaggedRegion.kt — Data Class
The core model that represents a single region of code that has been flagged as suspicious. Immutable, all fields are `val`. A data class because it is pure data, auto-generated `equals()` and `hashCode()` are needed for test assertions, and `toString()` is useful for logging and debugging.

### SerializableFlaggedRegion.kt — Class
An XML-serializable wrapper for `FlaggedRegion` used exclusively inside `AlethiaStateService`. Cannot be a data class because IntelliJ's XML serializer requires mutable fields with no-arg defaults. Contains `companion object` with `from()` factory method to convert from `FlaggedRegion`, and a `toFlaggedRegion()` method to convert back.

### RuleResult.kt — Data Class
A data Class designed to represent an events `rationale` and `eventType` variables. `EventType` was added as a strandardized idnetifier for events. `EventType` allows Themis to check against a list of known eventTypes rather than parsing the rationale description.


## Services

### LoggingService.kt — Interface
An interface used to create logger objects from the `java.util.logging.Logging` package. Previously used `Log4j`, however logs were appearing in IntelliJ's `idea.log` making it difficult to identify Alethia specific logs.

### LoggingFactory.kt — Class
This class implements `LoggingService` to create logger objects. On initiliazation, the class uses a `FileHandler` object for formatting the logger and produced logs. This class is registered as an application scoped service. If the `LoggingFactory` fails to be created, the failure is logged in IntelliJ's `idea.log` via `Log4j` loggers.

## Session

### SessionState.kt — Interface
Contract for Alethia session state. Defines the methods and properties any session state implementation must provide, `addFlag`, `getFlags`, `flagCount`, `clearFlags`, and `lastCommitSha`. `AlethiaEventHandler` and any other component that needs session state is coupled only to this interface. This makes the persistence mechanism swappable and allows tests to inject a simple in-memory mock.

### AlethiaStateService.kt — Class
Implementation of `SessionState` and the single source of truth for all session persistence. A project-scoped IntelliJ Platform service managed by the IntelliJ container, never instantiated directly, always accessed via `project.service<AlethiaStateService>()`. Project-scoped to prevent state conflicts between multiple open repositories.


## Startup

### AlethiaInstaller.kt — Class
A class that handles all one-time activities that need to be ran on project startup. Currently installs two items: First, the `pre-push hook` used for commit/push detection and responses. Second, it udpates the `.git\config` file to enable note copying when rebase and amend actions occur. Called by `AlethiaStartupActivity.kt`, a class registered with IntelliJ as a `postStartupActivity`. All tasks are designed to be idempotent and check for evidence of previous calls, preventing duplicate logic.

### AlethiaStartupActivity.kt — Class
A class that handles calls as services that must be ran on project startup. Tasks that must be run on startup are divided into seperate classes (currently just `AlethiaInstaller.kt`). If we add more startup tasks in the future that do not belong in the Installer, then we can define a new class and handle tasks from this central controller. The class implements `ProjectActivity` and is registered as a `postStartupActivity` to ensure it is run on porject load.


## VCS

### AlethiaCheckinHandlerFactory.kt::AlethiaCheckinHandlerFactory — Class
A class that handles creating `CheckinHandler` objects for each commit. This class implements `VcsCheckinHandlerFactory` and overrides `createVCSHandler()` which is called whenever a commit is detected. The class is registered as a `VcsCheckinHandlerFactory` which means that it is hooked into IntelliJs commit pipeline, detecting any and all commits.

### AlethiaCheckinHandlerFactory.kt::AlethiaCheckinHandler — Class
A class that gets instantiated on every commit. A seperate `AlethiaCheckinHandler` object is created for each commit operations, calling `checkinSuccessful()` which we override to handle creating git notes, creating a snapshot of the current porject state, and clear all cached `FlaggedRegion` object in the SessionState.


## Build

### plugin.xml - Build config file
Configuration file read by the IntelliJ Platform at startup. Declares the plugin identity, dependencies, registered services, listeners, and extension points. IntelliJ will not know any of the plugin's components exist without being declared here.
