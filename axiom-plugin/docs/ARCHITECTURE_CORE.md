# Core Architecture (Kernel)

## Goal
Make the plugin core explicit and modular for the military-political-industrial server model, with clear boundaries, order, and lifecycle.

## Kernel Overview
The kernel is a small lifecycle manager that owns module registration, dependency validation, and startup order. It also exposes a `ServiceRegistry` for cross-module access.

## Layers
- Kernel: module lifecycle and service registry.
- Domain: services grouped by `com.axiom.domain.service.{state|politics|industry|technology|military|infrastructure}`.
- Infrastructure: integration, caching, metrics, exports, dashboards.
- Application: commands, controllers, listeners (`com.axiom.app.*`).
- Presentation: GUI menus and UI-mod messaging.

## Module Order
Startup order is derived from dependencies and validated at boot. If a dependency is missing or a cycle exists, plugin enable fails.
- `state` -> baseline for all.
- `politics` -> depends on `state`.
- `industry` -> depends on `state`.
- `technology` -> depends on `state` and `industry`.
- `military` -> depends on `state`, `politics`, `industry`.
- `infrastructure` -> depends on all core modules (cross-cutting).

## Module Responsibilities (High-Level)
- `state`: nation/player state, territory, education, population, migration, health, sanitation, and core life-cycle services.
- `politics`: diplomacy, treaties, culture/religion, policies, sanctions/embargoes, propaganda, elections.
- `industry`: economy, markets, resources, supply chains, stockpiles, currency, trade logistics.
- `technology`: research tree and space/research programs (consumes state/industry signals).
- `military`: war systems, conquests, mobilization, intelligence, military alliances/espionage.
- `infrastructure`: integration and cross-cutting services (mod integration, exports, maps, UI sync, caching, metrics).

## Domain Index (Minimal Public API)
`com.axiom.domain.DomainServices` is a small facade that exposes only the stable, high-value services by domain.
Use `AXIOM.domain()` to access this index without importing deep package trees.

## Service Registry
Modules construct services and register them in `ServiceRegistry`. The registry:
- provides lookup by type;
- enforces no duplicate registrations;
- throws on missing required services.

## Binding to AXIOM
As services are registered, they are bound into `AXIOM` fields by type. This keeps existing getters and external integrations stable while moving creation into modules.

## Kernel Lifecycle
- `registerModule`: module registration, duplicate id check.
- `start`: validates dependencies, resolves order, runs `register`, then `onEnable` for each module.
- `stop`: calls `onDisable` in reverse order.

## Files
- `axiom-plugin/src/main/java/com/axiom/kernel/AxiomKernel.java`
- `axiom-plugin/src/main/java/com/axiom/kernel/ServiceRegistry.java`
- `axiom-plugin/src/main/java/com/axiom/kernel/ServiceBinder.java`
- `axiom-plugin/src/main/java/com/axiom/kernel/modules/*.java`
- `axiom-plugin/src/main/java/com/axiom/AXIOM.java`
