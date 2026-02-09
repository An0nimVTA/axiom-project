# Core Architecture (Plugin Kernel)

## Goal
Define and anchor a core architecture for the AXIOM plugin that reflects the military-political-industrial server focus, with explicit module boundaries, lifecycle, and service registry.

## Context
The plugin was a monolith centered on `com.axiom.AXIOM` with a very large set of services created directly in `onEnable()`. We needed explicit module boundaries, dependency ordering, and a registry to reflect the military-political-industrial core.

## Docs
- `KEY_DOCS.md`
- `docs/PROJECT_SPEC.md`
- `axiom-plugin/docs/PLUGIN_INFO.md`
- `axiom-plugin/docs/PLUGIN_STRUCTURE.md`

## Knowledge
- Entry point: `com.axiom.AXIOM` (extends `JavaPlugin`)
- Initialization: service construction moved into kernel modules; `onEnable()` focuses on bootstrap + command/listener registration
- No explicit module boundaries; services live in `com.axiom.service`
- New architecture components existed but were isolated (factory, cache, military controller)

## Steps
1. Map current architecture (entry points, initialization, service clusters).
2. Define a kernel layer with module lifecycle + service registry.
3. Create domain modules aligned to the military-political-industrial loop.
4. Register existing services into the kernel without breaking runtime behavior.
5. Document the architecture and module dependencies.

## Current Architecture (Snapshot)
- Entry point: `AXIOM#onEnable()`
- Initialization sequence: core services -> mod integration services -> command executors -> listeners + plugin messaging -> autosave + optional autotests
- Service placement: all domain logic in `com.axiom.service` (165+ services)
- Interface adapters: commands in `com.axiom.command`, controllers in `com.axiom.controller`, listeners in `com.axiom.listener`, GUI in `com.axiom.gui`

## Target Architecture (Core Kernel)
Layered view:
- Kernel: module lifecycle + service registry
- Domain modules: State (nation, territory, population backbone); Politics (diplomacy, treaties, elections, influence); Industry (economy, banking, trade, resources); Military (war, mobilization, conquest); Technology (education, tech tree)
- Application layer: commands, controllers, listeners orchestrating domain services
- Infrastructure: persistence, integration, caching, metrics, exports
- Presentation: GUI menus, UI mod messaging, dashboards

Module dependencies:
- `state` -> baseline for all
- `politics` -> depends on `state`
- `industry` -> depends on `state`
- `technology` -> depends on `state`, `industry`
- `military` -> depends on `state`, `politics`, `industry`
- `infrastructure` -> depends on all core modules (cross-cutting)

## Implementation Notes
- Introduced kernel packages: `com.axiom.kernel` (kernel, module interface, registry, binder) and `com.axiom.kernel.modules` (domain module registrations).
- Services are now constructed inside modules and registered into `ServiceRegistry`.
- Dependency validation is enforced; missing deps or cycles fail plugin enable.
- `AXIOM` binds services on registration to preserve existing getters.
- Runtime behavior unchanged; architecture is now explicit and enforceable.

## Acceptance Criteria
- Kernel + module lifecycle implemented in code.
- Domain modules and dependencies aligned to the military-political-industrial loop.
- `AXIOM` bootstraps the kernel and registers modules.
- Architecture documented and indexed.

## Notes
- This is a minimal structural change to lock in boundaries.
- Next step: reduce legacy field usage by moving callers to `ServiceRegistry` directly where appropriate.
