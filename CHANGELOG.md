# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [v1.0.0] - 2026-09-12

Initial release. No prior tag exists, so this covers the full history of the project to date.

### Added

- Client-only Fabric mod scaffold for Minecraft 26.1.2 + Fabric Loader `0.19.5`, built with Fabric Loom `1.17` and Gradle `9.5.1`, targeting Java 25 (auto-provisioned via the Foojay toolchain resolver when the host JDK is older).
- `AutoPacksConfig`: JSON-persisted settings (`enabled`, `delaySeconds` clamped 0–30s, `notifyOnApply`, `skipIfAlreadyApplied`, `snapshot`) stored at `config/auto-texturepacks.json`; corrupt/missing config falls back to defaults without crashing.
- `PackSnapshot`: captures the current resource-pack selection, in order, excluding Minecraft's "required" packs (vanilla plus every mod's own auto-registered resource pack) so counts and comparisons only ever reflect real, user-toggleable packs.
- `AutoApplyScheduler`: arms a one-shot countdown on game start (only when enabled and a snapshot exists) and fires exactly once per launch, regardless of which screen is showing.
- `PackApplier`: re-selects the saved pack order, skips the reload when the current selection already matches (configurable), reports packs that went missing since the snapshot was taken, and shows a toast/chat notification summarizing what happened.
- `AutoPacksConfigScreen`: vanilla-widget settings screen — enable toggle, delay slider, notification toggle, skip-if-match toggle, read-only saved-order list, and **Use current pack order** / **Apply now** / **Done** buttons.
- Mod Menu integration (`ModMenuIntegration`) as the only entry point to the settings screen — no vanilla Options button, no chat command.
- Mod icon.

### Fixed

Issues found and resolved during development, before this first release:

- Snapshot capture and pack-apply comparisons no longer count Minecraft's auto-included "required" packs (vanilla + every mod's own resource pack, ~48 entries) as part of the saved order — previously this could silently wipe a real pack selection while reporting a misleading "48 packs applied".
- Removed automatic snapshot capture on game close (`CLIENT_STOPPING`): it could silently overwrite a deliberately saved pack order with whatever happened to be selected (or not selected) at the moment the player quit. The saved order now only changes via the explicit **Use current pack order** button.
- A manual **Apply now** click no longer leaves the scheduled auto-apply timer pending; without this, the timer could fire again shortly after a manual apply and redundantly reload with no visible change.
- Pack reloads are now forced immediately instead of using Minecraft's deferrable public `reloadResourcePacks()` API, which silently postpones the reload while the boot loading overlay is still showing (routinely true when the auto-apply timer fires) and flushes it later on its own — this was the source of an apparent "double apply" with no mod involvement in the second reload.
